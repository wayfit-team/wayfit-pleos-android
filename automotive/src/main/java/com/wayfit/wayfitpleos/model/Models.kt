package com.wayfit.wayfitpleos.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class StoreCategory(val label: String, val emoji: String) {
    CAFE("카페", "☕"),
    FOOD("음식점", "🍜"),
    BAKERY("베이커리", "🥐"),
    ETC("기타", "🏪"),
}

data class GeoPoint(val lat: Double, val lng: Double) {
    /** 하버사인 거리(m) */
    fun distanceTo(other: GeoPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
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

data class Coupon(
    val id: String,
    val storeName: String,
    val benefit: String,
    val code: String,
    val expiresInDays: Int,
    val redeemed: Boolean = false,
)

data class RouteSummary(
    val destination: String,
    val remainingKm: Double,
    val etaMin: Int,
    val drivingMinutes: Int,
)

enum class VisitStep(val label: String) {
    WAYPOINT("내비 경유지 추가"),
    ARRIVED("매장 도착"),
    PARKED("주차 확인"),
    ISSUED("쿠폰 발급"),
}

sealed interface DrivePhase {
    data object Idle : DrivePhase

    data class Recommending(
        val store: Store,
        val secondsLeft: Int,
        val reason: String?,
    ) : DrivePhase

    data class Visiting(
        val store: Store,
        val remainingM: Int,
        val done: Set<VisitStep>,
        val verifying: Boolean,
    ) : DrivePhase

    data class Verified(val coupon: Coupon) : DrivePhase
}

sealed interface PairState {
    data object NotPaired : PairState
    data class Showing(val code: String, val secondsLeft: Int) : PairState
    data class Paired(val phoneCount: Int) : PairState
}

enum class Tab { DRIVE, COUPONS, PAIR }

/** 디버그 패널 표시용 SDK 연결 상태 */
data class SdkStatus(
    val vehicle: Boolean = false,
    val navi: Boolean = false,
    val location: Boolean = false,
    val tts: Boolean = false,
    val stt: Boolean = false,
)

data class UiState(
    val sdk: SdkStatus = SdkStatus(),
    val listening: Boolean = false,
    val driving: Boolean,
    val tab: Tab,
    val phase: DrivePhase,
    val route: RouteSummary,
    val tripRecommendCount: Int,
    val coupons: List<Coupon>,
    val pair: PairState,
)

const val RECOMMEND_TIMEOUT_SEC = 15
const val MAX_RECOMMEND_PER_TRIP = 3
const val PAIRING_CODE_TTL_SEC = 600
const val PAIR_URL_BASE = "https://wayfit.app/pair?code="
