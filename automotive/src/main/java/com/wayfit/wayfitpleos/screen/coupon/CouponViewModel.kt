package com.wayfit.wayfitpleos.screen.coupon

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val couponRepository: CouponRepository,
    private val pairingRepository: PairingRepository,
) : BaseViewModel<CouponState, Event>() {

    override fun getInitialState(): CouponState = CouponState()

    init {
        viewModelScope.launch {
            combine(couponRepository.coupons, pairingRepository.phones) { coupons, phones ->
                CouponState(coupons, phoneCount = phones.size)
            }.collect(::setState)
        }
    }

    /**
     * 화면이 보이는 동안 서버 목록을 주기적으로 다시 받는다 — 폰에서 사용 처리한 쿠폰·새로 연결된 폰이 바로 반영된다.
     * 탭 ViewModel 은 탭을 옮겨도 살아 있어 init 에서 한 번만 받으면 다시 들어와도 갱신되지 않는다.
     * 그래서 화면(CouponScreen)이 보이는 수명에 맞춰 부르고, 화면이 사라지면 취소된다.
     * 실패하면 갖고 있던 목록을 그대로 보여주고 다음 주기에 다시 시도한다.
     */
    suspend fun refreshWhileVisible() {
        while (true) {
            refresh("coupon") { couponRepository.refresh() }
            refresh("phone") { pairingRepository.refreshPhones() }
            delay(REFRESH_MS)
        }
    }

    private suspend fun refresh(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "$what refresh failed", e)
        }
    }

    private companion object {
        const val TAG = "CouponViewModel"
        const val REFRESH_MS = 5_000L
    }
}
