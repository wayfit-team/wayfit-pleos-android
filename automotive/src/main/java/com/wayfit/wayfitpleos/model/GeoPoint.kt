package com.wayfit.wayfitpleos.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lng: Double) {

    /** 하버사인 거리(m) */
    fun distanceTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    /** [origin] 에서 이 지점까지 북쪽·동쪽 방향 거리(m). [offset] 의 역 (짧은 거리용 근사) */
    fun northEastFrom(origin: GeoPoint): Pair<Double, Double> = Pair(
        Math.toRadians(lat - origin.lat) * EARTH_RADIUS_M,
        Math.toRadians(lng - origin.lng) * EARTH_RADIUS_M * cos(Math.toRadians(origin.lat)),
    )

    /** 북쪽·동쪽으로 [northM], [eastM] 미터 옮긴 지점 (짧은 거리용 근사) */
    fun offset(northM: Double, eastM: Double): GeoPoint = GeoPoint(
        lat = lat + Math.toDegrees(northM / EARTH_RADIUS_M),
        lng = lng + Math.toDegrees(eastM / (EARTH_RADIUS_M * cos(Math.toRadians(lat)))),
    )

    private companion object {
        const val EARTH_RADIUS_M = 6_371_000.0
    }
}
