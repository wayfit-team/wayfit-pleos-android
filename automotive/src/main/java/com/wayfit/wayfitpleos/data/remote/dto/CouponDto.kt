package com.wayfit.wayfitpleos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CouponListResponse(val coupons: List<CouponDto>)

@Serializable
data class CouponDto(
    val id: String,
    val storeId: String,
    val storeNameSnapshot: String,
    val benefitSnapshot: String,
    /** 계산대 제시용 6자리 숫자 */
    val code6: String,
    /** ISSUED · REDEEMED · EXPIRED */
    val status: String,
    val issuedAt: String,
    val expiresAt: String,
)
