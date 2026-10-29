package com.wayfit.wayfitpleos.screen.coupon

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.screen.base.State

data class CouponState(
    val coupons: List<Coupon> = emptyList(),
    val phoneCount: Int = 0,
) : State
