package com.wayfit.wayfitpleos.model

enum class StoreCategory(val label: String, val emoji: String) {
    CAFE("카페", "☕"),
    FOOD("음식점", "🍜"),
    BAKERY("베이커리", "🥐"),
    ETC("기타", "🏪"),
}

data class Store(
    val id: String,
    val name: String,
    val category: StoreCategory,
    val benefit: String,
    val location: GeoPoint,
    val aheadKm: Double,
    val detourMin: Int,
    val radiusM: Float = 150f,
)
