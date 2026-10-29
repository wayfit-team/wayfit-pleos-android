package com.wayfit.wayfitpleos.screen.coupon

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
    couponRepository: CouponRepository,
    pairingRepository: PairingRepository,
) : BaseViewModel<CouponState, Event>() {

    override fun getInitialState(): CouponState = CouponState()

    init {
        viewModelScope.launch {
            combine(couponRepository.coupons, pairingRepository.phones) { coupons, phones ->
                CouponState(coupons, phoneCount = phones.size)
            }.collect(::setState)
        }
    }
}
