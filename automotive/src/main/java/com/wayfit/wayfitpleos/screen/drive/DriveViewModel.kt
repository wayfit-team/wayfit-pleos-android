package com.wayfit.wayfitpleos.screen.drive

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DemoCommand
import com.wayfit.wayfitpleos.data.repository.DemoRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.data.repository.StoreRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.TripPolicy
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.model.VoiceIntent
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * 추천 → 방문 → 인증 상태 머신.
 * SDK 이벤트(P단, 경유지 도착, 지오펜스, 음성 응답)와 DEMO 명령이 같은 함수로 흐름을 진행한다.
 */
@HiltViewModel
class DriveViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val naviRepository: NaviRepository,
    private val locationRepository: LocationRepository,
    private val voiceRepository: VoiceRepository,
    private val storeRepository: StoreRepository,
    private val couponRepository: CouponRepository,
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository,
    demoRepository: DemoRepository,
) : BaseViewModel<DriveState, DriveEvent>() {

    override fun getInitialState(): DriveState = DriveState.Idle(DEMO_ROUTE, tripRecommendCount = 0)

    private var tripRecommendCount = 0
    private var nextStoreIndex = 0
    private var phaseJob: Job? = null
    private var voiceJob: Job? = null

    private val route: RouteSummary get() = naviRepository.route.value ?: DEMO_ROUTE

    /** 넓은 화면의 경로 패널용. 단계(State)와 무관하게 항상 보인다 */
    val routeFlow: StateFlow<RouteSummary> = naviRepository.route
        .map { it ?: DEMO_ROUTE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEMO_ROUTE)

    init {
        viewModelScope.launch {
            demoRepository.commands.collect { command ->
                when (command) {
                    DemoCommand.RECOMMEND -> recommendNext()
                    DemoCommand.ARRIVE_AND_PARK -> demoArriveAndPark()
                }
            }
        }
        viewModelScope.launch {
            naviRepository.route.collect { withState<DriveState.Idle> { setState(it.copy(route = route)) } }
        }
        viewModelScope.launch { naviRepository.waypointArrived.collect { onArrived("WAYPOINT_ARRIVED") } }
        viewModelScope.launch { locationRepository.geofenceEntered.collect { onArrived("GEOFENCE_ENTER") } }
        viewModelScope.launch { vehicleRepository.parked.collect { if (it == true) onParked() } }
        viewModelScope.launch { vehicleRepository.doorOpened.collect { Log.i(TAG, "evidence: DOOR_OPEN") } }
        // 내비 위치·목적지 주기 갱신 + 방문 중 남은 거리 (지오펜스가 안 되면 앱이 직접 반경 판정)
        viewModelScope.launch {
            while (isActive) {
                naviRepository.refresh()
                updateVisitDistance()
                delay(3_000)
            }
        }
    }

    // region 사용자 액션

    fun accept() {
        val state = stateFlow.value as? DriveState.Recommending ?: return
        stopVoice()
        phaseJob?.cancel()
        val store = state.store
        val waypointAdded = naviRepository.addWaypoint(store)
        locationRepository.watch(store)
        if (naviRepository.isAvailable && !waypointAdded) {
            sendEvent(DriveEvent.ShowMessage("내비에 경유지를 추가하지 못했어요"))
        }
        setState(
            DriveState.Visiting(
                store = store,
                remainingM = currentPoint()?.distanceTo(store.location)?.toInt() ?: (store.aheadKm * 1000).toInt(),
                // SDK가 없으면(DEMO) 경유지 추가는 성공으로 표시
                done = if (waypointAdded || !naviRepository.isAvailable) setOf(VisitStep.WAYPOINT) else emptySet(),
            )
        )
    }

    fun decline() {
        stopVoice()
        phaseJob?.cancel()
        setIdle()
    }

    fun cancelVisit() {
        phaseJob?.cancel()
        naviRepository.removeWaypoint()
        locationRepository.clearWatch()
        setIdle()
    }

    fun dismissResult() = setIdle()

    fun sendToPhone() {
        setIdle()
        sendEvent(DriveEvent.OpenPairing)
    }

    // endregion

    // region 추천

    private fun recommendNext() = viewModelScope.launch {
        if (stateFlow.value !is DriveState.Idle) return@launch
        if (tripRecommendCount >= TripPolicy.MAX_RECOMMEND_PER_TRIP) tripRecommendCount = 0 // 데모 편의상 리셋

        val here = currentPoint()
        val stores = storeRepository.getRecommendableStores(here)
        val store = stores[nextStoreIndex++ % stores.size]
        val distanceKm = here?.distanceTo(store.location)?.div(1000) ?: store.aheadKm
        val longDrive = (vehicleRepository.drivingMinutes.value ?: 0) >= TripPolicy.LONG_DRIVE_MINUTES
        tripRecommendCount++

        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            for (left in TripPolicy.RECOMMEND_TIMEOUT_SEC downTo 1) {
                val listening = (stateFlow.value as? DriveState.Recommending)?.listening ?: false
                setState(
                    DriveState.Recommending(
                        store = store,
                        distanceKm = distanceKm,
                        secondsLeft = left,
                        reason = if (longDrive) "2시간째 운전 중이에요 · 잠깐 쉬어가세요" else null,
                        listening = listening,
                    )
                )
                delay(1_000)
            }
            stopVoice()
            setIdle()
        }
        askByVoice(store, distanceKm)
    }

    /** 추천을 음성으로 읽고 5초간 "응/아니"를 듣는다 (Spec REQ-A20, A21) */
    private fun askByVoice(store: Store, distanceKm: Double) {
        voiceJob?.cancel()
        if (!settingsRepository.voiceGuideEnabled.value) return
        voiceJob = viewModelScope.launch {
            voiceRepository.speak("${spokenDistance(distanceKm)} 앞 ${store.name}, ${store.benefit}예요. 들를까요?")
            if (stateFlow.value !is DriveState.Recommending) return@launch
            setListening(true)
            val heard = voiceRepository.listen(TripPolicy.VOICE_LISTEN_MS)
            setListening(false)
            Log.i(TAG, "heard: $heard")
            when (VoiceIntent.classify(heard)) {
                VoiceIntent.YES -> accept()
                VoiceIntent.NO -> decline()
                VoiceIntent.UNKNOWN -> Unit // 카드만 유지, 다시 묻지 않음
            }
        }
    }

    private fun setListening(listening: Boolean) =
        withState<DriveState.Recommending> { setState(it.copy(listening = listening)) }

    private fun stopVoice() {
        voiceJob?.cancel()
        voiceRepository.stopAll()
        setListening(false)
    }

    // endregion

    // region 방문 → 인증

    private fun currentPoint(): GeoPoint? = locationRepository.location.value ?: naviRepository.location.value

    private fun updateVisitDistance() {
        val visiting = stateFlow.value as? DriveState.Visiting ?: return
        if (VisitStep.ARRIVED in visiting.done) return
        val distance = currentPoint()?.distanceTo(visiting.store.location) ?: return
        setState(visiting.copy(remainingM = distance.toInt()))
        if (distance <= visiting.store.radiusM) onArrived("LOCATION")
    }

    private fun onArrived(evidence: String) {
        withState<DriveState.Visiting> { visiting ->
            if (VisitStep.ARRIVED in visiting.done) return
            Log.i(TAG, "evidence: $evidence")
            setState(visiting.copy(remainingM = 0, done = visiting.done + VisitStep.ARRIVED))
            if (vehicleRepository.parked.value == true) onParked()
        }
    }

    /** 매장 도착 후 P단 → 방문 인증 (백엔드 연동 전: 로컬에서 쿠폰 발급) */
    private fun onParked() {
        val visiting = stateFlow.value as? DriveState.Visiting ?: return
        if (VisitStep.ARRIVED !in visiting.done || visiting.verifying) return
        locationRepository.clearWatch()
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            setState(visiting.copy(done = visiting.done + VisitStep.PARKED, verifying = true))
            delay(1_200)
            val coupon = couponRepository.issue(visiting.store)
            setState(DriveState.Verified(coupon, phonePaired = pairingRepository.pairing.value is Pairing.Paired))
            if (settingsRepository.voiceGuideEnabled.value) voiceRepository.speak("${coupon.storeName} 쿠폰이 도착했어요")
        }
    }

    private fun demoArriveAndPark() {
        val visiting = stateFlow.value as? DriveState.Visiting ?: return
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            var remaining = visiting.remainingM
            while (remaining > 0) {
                remaining = (remaining - 150).coerceAtLeast(0)
                withState<DriveState.Visiting> { setState(it.copy(remainingM = remaining)) }
                delay(250)
            }
            onArrived("DEMO")
            delay(800)
            vehicleRepository.demoSetParked(true)
            onParked()
        }
    }

    // endregion

    private fun setIdle() = setState(DriveState.Idle(route, tripRecommendCount))

    override fun onCleared() {
        stopVoice()
        super.onCleared()
    }

    private companion object {
        const val TAG = "DriveViewModel"

        /** 내비 안내가 없을 때(에뮬레이터 등) 보여줄 데모 경로 */
        val DEMO_ROUTE = RouteSummary(destination = "광교호수공원", remainingKm = 12.4, etaMin = 18)

        fun spokenDistance(km: Double): String =
            if (km < 1) "${(km * 1000).toInt()}미터" else String.format(Locale.KOREA, "%.1f킬로미터", km)
    }
}
