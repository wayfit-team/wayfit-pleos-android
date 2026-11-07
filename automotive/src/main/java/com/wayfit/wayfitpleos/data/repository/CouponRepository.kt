package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Coupon
import kotlinx.coroutines.flow.StateFlow

interface CouponRepository {

    /** 이 차량 기기에 발급된 쿠폰 (최신순) */
    val coupons: StateFlow<List<Coupon>>

    /** 서버에서 다시 받아온다 — 폰에서 사용 처리한 쿠폰도 반영된다 */
    suspend fun refresh()

    /** 방문 인증 응답으로 받은 쿠폰을 바로 목록에 넣는다 (같은 ID 면 교체) */
    fun add(coupon: Coupon)
}
