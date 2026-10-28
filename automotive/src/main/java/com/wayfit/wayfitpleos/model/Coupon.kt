package com.wayfit.wayfitpleos.model

data class Coupon(
    val id: String,
    val storeName: String,
    val benefit: String,
    val code: String,
    val expiresInDays: Int,
    val redeemed: Boolean = false,
)
