package com.wayfit.wayfitpleos

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.FakeData
import com.wayfit.wayfitpleos.data.pleos.PleosLocation
import com.wayfit.wayfitpleos.data.pleos.PleosNavi
import com.wayfit.wayfitpleos.data.pleos.PleosVehicle
import com.wayfit.wayfitpleos.data.pleos.PleosVoice
import com.wayfit.wayfitpleos.domain.VoiceIntent
import com.wayfit.wayfitpleos.domain.classifyVoice
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.DrivePhase
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.MAX_RECOMMEND_PER_TRIP
import com.wayfit.wayfitpleos.model.PAIRING_CODE_TTL_SEC
import com.wayfit.wayfitpleos.model.PairState
import com.wayfit.wayfitpleos.model.RECOMMEND_TIMEOUT_SEC
import com.wayfit.wayfitpleos.model.SdkStatus
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.Tab
import com.wayfit.wayfitpleos.model.UiState
import com.wayfit.wayfitpleos.model.VisitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

/**
 * 화면 상태 머신.
 * - Pleos SDK 이벤트(기어 P단, 경유지 도착, 지오펜스 진입, 음성 응답)로 실제 흐름을 진행한다.
 * - SDK가 없는 환경(일반 에뮬레이터)이나 시연을 위해 DEMO 트리거도 같은 함수로 흐름을 진행한다.
 * - 추천 대상 매장·쿠폰은 백엔드 연동 전까지 FakeData 사용.
 */
class WayfitViewModel(app: Application) : AndroidViewModel(app) {

    private val vehicle = PleosVehicle(app)
    private val navi = PleosNavi(app)
    private val location = PleosLocation(app)
    private val voice = PleosVoice(app)

    private val _state = MutableStateFlow(
        UiState(
            driving = true,
            tab = Tab.DRIVE,
            phase = DrivePhase.Idle,
            route = FakeData.route,
            tripRecommendCount = 0,
            coupons = FakeData.coupons,
            pair = PairState.NotPaired,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var sdkStarted = false
    private var phaseJob: Job? = null
    private var voiceJob: Job? = null
    private var pairJob: Job? = null
    private var nextStoreIndex = 0

    // region SDK 연결

    /** 권한 요청이 끝난 뒤 MainActivity 가 호출 */
    fun startSdks(fusedLocationGranted: Boolean) {
        if (sdkStarted) return
        sdkStarted = true
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                val v = vehicle.start()
                val n = navi.start()
                val l = fusedLocationGranted && location.start()
                voice.start()
                SdkStatus(vehicle = v, navi = n, location = l, tts = voice.isTtsReady, stt = voice.isSttReady)
            }
            Log.i(TAG, "SDK status: $status")
            _state.update { it.copy(sdk = status) }
            observeSdks()
        }
    }

    private fun observeSdks() {
        viewModelScope.launch {
            vehicle.gear.filterNotNull().collect { gear ->
                val parked = gear == VehicleGear.GEAR_PARK
                _state.update { it.copy(driving = !parked) }
                if (parked) onParked()
            }
        }
        viewModelScope.launch {
            vehicle.tripDrivingTime.filterNotNull().collect { minutes ->
                _state.update { it.copy(route = it.route.copy(drivingMinutes = minutes)) }
            }
        }
        viewModelScope.launch {
            vehicle.doorOpened.collect { Log.i(TAG, "evidence: DOOR_OPEN") }
        }
        viewModelScope.launch {
            navi.destination.filterNotNull().collect { dest ->
                // remainDistance(m), remainTime(초)로 가정 — 스파이크에서 단위 확인
                _state.update {
                    it.copy(
                        route = it.route.copy(
                            destination = dest.name,
                            remainingKm = dest.remainDistance / 1000.0,
                            etaMin = dest.remainTime / 60,
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            navi.waypointArrived.collect {
                Log.i(TAG, "evidence: WAYPOINT_ARRIVED ${it.name}")
                onArrived()
            }
        }
        viewModelScope.launch {
            location.geofenceEntered.collect {
                Log.i(TAG, "evidence: GEOFENCE_ENTER")
                onArrived()
            }
        }
        // 내비 위치·목적지 주기 갱신 + 방문 중 남은 거리 계산 (지오펜스가 안 되면 앱이 직접 반경 판정)
        viewModelScope.launch {
            while (isActive) {
                navi.refresh()
                updateVisitDistance()
                delay(3_000)
            }
        }
    }

    private fun currentPoint(): GeoPoint? = location.currentPoint() ?: navi.currentPoint()

    private fun updateVisitDistance() {
        val visiting = _state.value.phase as? DrivePhase.Visiting ?: return
        val here = currentPoint() ?: return
        val distance = here.distanceTo(visiting.store.location)
        updateVisiting { it.copy(remainingM = distance.toInt()) }
        if (distance <= visiting.store.radiusM) onArrived()
    }

    // endregion

    // region 사용자 액션

    fun accept() {
        val phase = _state.value.phase as? DrivePhase.Recommending ?: return
        stopVoice()
        phaseJob?.cancel()
        val store = phase.store
        val waypointAdded = navi.addWaypoint(store)
        location.watchGeofence(store.location, store.radiusM)
        _state.update {
            it.copy(
                phase = DrivePhase.Visiting(
                    store = store,
                    remainingM = currentPoint()?.distanceTo(store.location)?.toInt()
                        ?: (store.aheadKm * 1000).toInt(),
                    // SDK가 없으면(DEMO) 경유지 추가는 성공으로 표시
                    done = if (waypointAdded || !_state.value.sdk.navi) setOf(VisitStep.WAYPOINT) else emptySet(),
                    verifying = false,
                )
            )
        }
    }

    fun decline() {
        stopVoice()
        phaseJob?.cancel()
        _state.update { it.copy(phase = DrivePhase.Idle) }
    }

    fun cancelVisit() {
        phaseJob?.cancel()
        navi.removeWaypoint()
        location.clearGeofence()
        _state.update { it.copy(phase = DrivePhase.Idle) }
    }

    fun dismissResult() {
        _state.update { it.copy(phase = DrivePhase.Idle) }
    }

    fun selectTab(tab: Tab) {
        _state.update { it.copy(tab = tab) }
        if (tab == Tab.PAIR && _state.value.pair is PairState.NotPaired) startPairing()
    }

    fun startPairing() {
        pairJob?.cancel()
        val code = (1..8).joinToString("") { Random.nextInt(10).toString() }
        pairJob = viewModelScope.launch {
            for (left in PAIRING_CODE_TTL_SEC downTo 0) {
                _state.update { it.copy(pair = PairState.Showing(code, left)) }
                delay(1_000)
            }
        }
    }

    // endregion

    // region 추천 → 방문 → 인증

    private fun recommend(store: Store) {
        val current = _state.value
        if (current.phase !is DrivePhase.Idle) return
        val reason = if (current.route.drivingMinutes >= 120) "2시간째 운전 중이에요 · 잠깐 쉬어가세요" else null

        phaseJob?.cancel()
        _state.update { it.copy(tab = Tab.DRIVE, tripRecommendCount = it.tripRecommendCount + 1) }
        phaseJob = viewModelScope.launch {
            for (left in RECOMMEND_TIMEOUT_SEC downTo 1) {
                _state.update { it.copy(phase = DrivePhase.Recommending(store, left, reason)) }
                delay(1_000)
            }
            stopVoice()
            _state.update { it.copy(phase = DrivePhase.Idle) }
        }
        askByVoice(store)
    }

    /** 추천을 음성으로 읽고 5초간 "응/아니"를 듣는다 (Spec REQ-A20, A21) */
    private fun askByVoice(store: Store) {
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            voice.speak("${spokenKm(store.aheadKm)} 앞 ${store.name}, ${store.benefit}예요. 들를까요?")
            if (_state.value.phase !is DrivePhase.Recommending) return@launch
            _state.update { it.copy(listening = true) }
            val heard = voice.listen()
            _state.update { it.copy(listening = false) }
            Log.i(TAG, "heard: $heard")
            when (classifyVoice(heard)) {
                VoiceIntent.YES -> accept()
                VoiceIntent.NO -> decline()
                VoiceIntent.UNKNOWN -> Unit // 카드만 유지, 다시 묻지 않음
            }
        }
    }

    private fun stopVoice() {
        voiceJob?.cancel()
        voice.stopSpeaking()
        voice.stopListening()
        _state.update { it.copy(listening = false) }
    }

    private fun onArrived() {
        val visiting = _state.value.phase as? DrivePhase.Visiting ?: return
        if (VisitStep.ARRIVED in visiting.done) return
        updateVisiting { it.copy(remainingM = 0, done = it.done + VisitStep.ARRIVED) }
        if (vehicle.gear.value == VehicleGear.GEAR_PARK) onParked()
    }

    /** 매장 도착 후 P단 → 방문 인증 (백엔드 연동 전: 로컬에서 쿠폰 발급) */
    private fun onParked() {
        val visiting = _state.value.phase as? DrivePhase.Visiting ?: return
        if (VisitStep.ARRIVED !in visiting.done || visiting.verifying) return
        location.clearGeofence()
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            _state.update { it.copy(driving = false) }
            updateVisiting { it.copy(done = it.done + VisitStep.PARKED, verifying = true) }
            delay(1_200)
            val coupon = Coupon(
                id = "c${System.currentTimeMillis()}",
                storeName = visiting.store.name,
                benefit = visiting.store.benefit,
                code = (1..6).joinToString("") { Random.nextInt(10).toString() },
                expiresInDays = 7,
            )
            _state.update {
                it.copy(phase = DrivePhase.Verified(coupon), coupons = listOf(coupon) + it.coupons)
            }
            voice.speak("${coupon.storeName} 쿠폰이 도착했어요")
        }
    }

    // endregion

    // region 데모 트리거 (SDK 이벤트를 흉내)

    fun demoToggleDriving() {
        _state.update { it.copy(driving = !it.driving) }
    }

    fun demoToggleLongDrive() {
        _state.update {
            val minutes = if (it.route.drivingMinutes >= 120) 35 else 130
            it.copy(route = it.route.copy(drivingMinutes = minutes))
        }
    }

    fun demoRecommend() {
        if (_state.value.tripRecommendCount >= MAX_RECOMMEND_PER_TRIP) {
            _state.update { it.copy(tripRecommendCount = 0) } // 데모 편의상 리셋
        }
        recommend(FakeData.stores[nextStoreIndex++ % FakeData.stores.size])
    }

    fun demoArriveAndPark() {
        val visiting = _state.value.phase as? DrivePhase.Visiting ?: return
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            var remaining = visiting.remainingM
            while (remaining > 0) {
                remaining = (remaining - 150).coerceAtLeast(0)
                updateVisiting { it.copy(remainingM = remaining) }
                delay(250)
            }
            onArrived()
            delay(800)
            onParked()
        }
    }

    fun demoPhonePaired() {
        pairJob?.cancel()
        _state.update {
            val count = (it.pair as? PairState.Paired)?.phoneCount ?: 0
            it.copy(pair = PairState.Paired(count + 1))
        }
    }

    // endregion

    override fun onCleared() {
        stopVoice()
        vehicle.stop()
        navi.stop()
        location.stop()
        voice.stop()
        super.onCleared()
    }

    private fun updateVisiting(transform: (DrivePhase.Visiting) -> DrivePhase.Visiting) {
        _state.update { state ->
            val phase = state.phase as? DrivePhase.Visiting ?: return@update state
            state.copy(phase = transform(phase))
        }
    }

    private companion object {
        const val TAG = "WayfitVM"

        fun spokenKm(km: Double): String =
            if (km < 1) "${(km * 1000).toInt()}미터" else String.format(Locale.KOREA, "%.1f킬로미터", km)
    }
}
