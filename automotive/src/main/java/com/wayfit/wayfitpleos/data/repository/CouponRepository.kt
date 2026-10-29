package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.flow.StateFlow

interface CouponRepository {

    val coupons: StateFlow<List<Coupon>>

    /** 방문 인증 → 쿠폰 발급 (백엔드 연동 전: 로컬 발급) */
    suspend fun issue(store: Store): Coupon
}
