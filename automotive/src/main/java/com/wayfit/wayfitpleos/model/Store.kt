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
    /**
     * 서버에 등록된 실제 좌표. 데모 지역 밖에서는 매장을 차량 주변으로 옮겨 보여주므로 [location] 과 다르다.
     * 방문 증거 좌표는 [toServerPoint] 로 이 좌표계로 바꿔 보낸다.
     */
    val serverLocation: GeoPoint = location,
) {
    /** 화면 좌표계(차량 주변으로 옮긴 매장 기준)의 [point] 를 서버 좌표계로 바꾼다 */
    fun toServerPoint(point: GeoPoint): GeoPoint {
        if (serverLocation == location) return point
        val (northM, eastM) = point.northEastFrom(location)
        return serverLocation.offset(northM, eastM)
    }
}
