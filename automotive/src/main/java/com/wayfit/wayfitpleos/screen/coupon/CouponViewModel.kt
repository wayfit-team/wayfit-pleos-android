package com.wayfit.wayfitpleos.screen.coupon

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
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
        // 폰에서 사용 처리한 쿠폰·새로 연결된 폰을 반영 (실패하면 갖고 있던 목록을 그대로 보여준다)
        viewModelScope.launch {
            runCatching { couponRepository.refresh() }.onFailure { Log.w(TAG, "coupon refresh failed", it) }
            runCatching { pairingRepository.refreshPhones() }.onFailure { Log.w(TAG, "phone refresh failed", it) }
        }
    }

    private companion object {
        const val TAG = "CouponViewModel"
    }
}
