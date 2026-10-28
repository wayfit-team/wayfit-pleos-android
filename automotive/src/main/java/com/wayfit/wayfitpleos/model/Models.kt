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
    val aheadKm: Double,
    val detourMin: Int,
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

data class UiState(
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
