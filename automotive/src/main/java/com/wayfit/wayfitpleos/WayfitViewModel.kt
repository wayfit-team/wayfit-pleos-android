package com.wayfit.wayfitpleos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.FakeData
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.DrivePhase
import com.wayfit.wayfitpleos.model.MAX_RECOMMEND_PER_TRIP
import com.wayfit.wayfitpleos.model.PAIRING_CODE_TTL_SEC
import com.wayfit.wayfitpleos.model.PairState
import com.wayfit.wayfitpleos.model.RECOMMEND_TIMEOUT_SEC
import com.wayfit.wayfitpleos.model.Tab
import com.wayfit.wayfitpleos.model.UiState
import com.wayfit.wayfitpleos.model.VisitStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * UI PoC 상태 머신. SDK·백엔드 없이 가짜 데이터와 데모 트리거로 전체 흐름을 재현한다.
 * 실제 구현에서는 RecommendationEngine / VisitTracker 가 이 트리거들을 대신한다.
 */
class WayfitViewModel : ViewModel() {

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

    private var phaseJob: Job? = null
    private var pairJob: Job? = null
    private var nextStoreIndex = 0

    // region 사용자 액션

    fun accept() {
        val phase = _state.value.phase as? DrivePhase.Recommending ?: return
        phaseJob?.cancel()
        _state.update {
            it.copy(
                phase = DrivePhase.Visiting(
                    store = phase.store,
                    remainingM = (phase.store.aheadKm * 1000).toInt(),
                    done = setOf(VisitStep.WAYPOINT),
                    verifying = false,
                )
            )
        }
    }

    fun decline() {
        phaseJob?.cancel()
        _state.update { it.copy(phase = DrivePhase.Idle) }
    }

    fun cancelVisit() {
        phaseJob?.cancel()
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

    // region 데모 트리거 (실제 앱에서는 SDK 이벤트가 대신함)

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
        val current = _state.value
        if (current.phase !is DrivePhase.Idle) return
        if (current.tripRecommendCount >= MAX_RECOMMEND_PER_TRIP) {
            _state.update { it.copy(tripRecommendCount = 0) } // 데모 편의상 리셋
        }
        val store = FakeData.stores[nextStoreIndex++ % FakeData.stores.size]
        val reason = if (current.route.drivingMinutes >= 120) "2시간째 운전 중이에요 · 잠깐 쉬어가세요" else null

        phaseJob?.cancel()
        _state.update { it.copy(tab = Tab.DRIVE, tripRecommendCount = it.tripRecommendCount + 1) }
        phaseJob = viewModelScope.launch {
            for (left in RECOMMEND_TIMEOUT_SEC downTo 1) {
                _state.update { it.copy(phase = DrivePhase.Recommending(store, left, reason)) }
                delay(1_000)
            }
            _state.update { it.copy(phase = DrivePhase.Idle) }
        }
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
            updateVisiting { it.copy(done = it.done + VisitStep.ARRIVED) }
            delay(800)
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

    private fun updateVisiting(transform: (DrivePhase.Visiting) -> DrivePhase.Visiting) {
        _state.update { state ->
            val phase = state.phase as? DrivePhase.Visiting ?: return@update state
            state.copy(phase = transform(phase))
        }
    }
}
