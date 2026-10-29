package com.wayfit.wayfitpleos.screen.drive

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.DemoCommand
import com.wayfit.wayfitpleos.data.repository.DemoRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.domain.RecommendStoreUseCase
import com.wayfit.wayfitpleos.domain.VisitEvidence
import com.wayfit.wayfitpleos.domain.VisitUseCase
import com.wayfit.wayfitpleos.domain.VoiceRecommendation
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
import javax.inject.Inject

/**
 * 추천 → 방문 → 인증 화면 상태 머신.
 * 비즈니스 규칙은 UseCase(추천 선정·방문 인증 판정·음성 문답)에 두고, 여기서는 화면 단계 전환만 한다.
 * SDK 이벤트와 DEMO 명령은 같은 경로로 흐름을 진행한다.
 */
@HiltViewModel
class DriveViewModel @Inject constructor(
    private val recommendStore: RecommendStoreUseCase,
    private val visit: VisitUseCase,
    private val voice: VoiceRecommendation,
    private val naviRepository: NaviRepository,
    demoRepository: DemoRepository,
) : BaseViewModel<DriveState, DriveEvent>() {

    override fun getInitialState(): DriveState = DriveState.Idle(DEMO_ROUTE, tripRecommendCount = 0)

    private var tripRecommendCount = 0
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
        viewModelScope.launch { naviRepository.route.collect { updateStateIf<DriveState.Idle> { it.copy(route = route) } } }
        viewModelScope.launch {
            visit.evidence.collect { evidence ->
                when (evidence) {
                    VisitEvidence.WAYPOINT_ARRIVED, VisitEvidence.GEOFENCE_ENTER -> onArrived(evidence.name)
                    VisitEvidence.DOOR_OPEN -> Log.i(TAG, "evidence: DOOR_OPEN")
                }
            }
        }
        viewModelScope.launch { visit.parked.collect { if (it) tryVerify() } }
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
        val start = visit.start(store)
        if (start.waypointFailed) sendEvent(DriveEvent.ShowMessage("내비에 경유지를 추가하지 못했어요"))
        setState(
            DriveState.Visiting(
                store = store,
                remainingM = visit.distanceTo(store) ?: (store.aheadKm * 1000).toInt(),
                done = if (start.waypointStepDone) setOf(VisitStep.WAYPOINT) else emptySet(),
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
        visit.cancel()
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
        val recommendation = recommendStore()
        tripRecommendCount++

        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            for (left in TripPolicy.RECOMMEND_TIMEOUT_SEC downTo 1) {
                updateState { current ->
                    DriveState.Recommending(
                        store = recommendation.store,
                        distanceKm = recommendation.distanceKm,
                        secondsLeft = left,
                        reason = recommendation.reason,
                        listening = (current as? DriveState.Recommending)?.listening ?: false,
                    )
                }
                delay(1_000)
            }
            stopVoice()
            setIdle()
        }
        askByVoice(recommendation.store, recommendation.distanceKm)
    }

    private fun askByVoice(store: Store, distanceKm: Double) {
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            val intent = voice.ask(
                store = store,
                distanceKm = distanceKm,
                stillAsking = { stateFlow.value is DriveState.Recommending },
                onListening = ::setListening,
            )
            Log.i(TAG, "voice: $intent")
            when (intent) {
                VoiceIntent.YES -> accept()
                VoiceIntent.NO -> decline()
                VoiceIntent.UNKNOWN, null -> Unit // 카드만 유지, 다시 묻지 않음
            }
        }
    }

    private fun setListening(listening: Boolean) =
        updateStateIf<DriveState.Recommending> { it.copy(listening = listening) }

    private fun stopVoice() {
        voiceJob?.cancel()
        voice.stop()
        setListening(false)
    }

    // endregion

    // region 방문 → 인증

    private fun updateVisitDistance() {
        val visiting = stateFlow.value as? DriveState.Visiting ?: return
        if (VisitStep.ARRIVED in visiting.done) return
        val distance = visit.distanceTo(visiting.store) ?: return
        updateStateIf<DriveState.Visiting> { if (VisitStep.ARRIVED in it.done) it else it.copy(remainingM = distance) }
        if (visit.isWithinRadius(visiting.store, distance)) onArrived("LOCATION")
    }

    private fun onArrived(evidence: String) {
        var arrivedNow = false
        updateStateIf<DriveState.Visiting> {
            if (VisitStep.ARRIVED in it.done) it
            else {
                arrivedNow = true
                it.copy(remainingM = 0, done = it.done + VisitStep.ARRIVED)
            }
        }
        if (!arrivedNow) return
        Log.i(TAG, "evidence: $evidence")
        // 이미 P단인 채로 도착하면 parked 스트림이 새 값을 내지 않으므로 여기서 확인
        if (visit.isParked()) tryVerify()
    }

    /** 도착 + P단이면 인증 (확인과 'verifying' 표시를 원자적으로 해서 중복 인증 방지) */
    private fun tryVerify() {
        var claimed: DriveState.Visiting? = null
        updateStateIf<DriveState.Visiting> {
            if (!visit.canVerify(it.done, it.verifying, parked = visit.isParked())) it
            else it.copy(done = it.done + VisitStep.PARKED, verifying = true).also { next -> claimed = next }
        }
        val visiting = claimed ?: return
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            delay(1_200)
            val result = visit.verify(visiting.store)
            setState(DriveState.Verified(result.coupon, result.phonePaired))
            voice.announceCoupon(result.coupon.storeName)
        }
    }

    /** DEMO: 남은 거리를 줄여 도착시키고 P단을 넣는다. 인증은 실제 SDK 이벤트와 같은 parked 스트림 경로로 */
    private fun demoArriveAndPark() {
        val visiting = stateFlow.value as? DriveState.Visiting ?: return
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            var remaining = visiting.remainingM
            while (remaining > 0) {
                remaining = (remaining - 150).coerceAtLeast(0)
                updateStateIf<DriveState.Visiting> { it.copy(remainingM = remaining) }
                delay(250)
            }
            onArrived("DEMO")
            delay(800)
            visit.demoPark()
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
    }
}
