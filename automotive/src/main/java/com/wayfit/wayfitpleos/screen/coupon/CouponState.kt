package com.wayfit.wayfitpleos.screen.coupon

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.screen.base.State

data class CouponState(
    val coupons: List<Coupon> = emptyList(),
    val pairing: Pairing = Pairing.NotPaired,
) : State
