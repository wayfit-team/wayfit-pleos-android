package com.wayfit.wayfitpleos.model

data class RouteSummary(
    val destination: String,
    val remainingKm: Double,
    val etaMin: Int,
)

enum class VisitStep(val label: String) {
    WAYPOINT("내비 경유지 추가"),
    ARRIVED("매장 도착"),
    PARKED("주차 확인"),
    ISSUED("쿠폰 발급"),
}

/** 디버그 패널 표시용 SDK 연결 상태 */
data class SdkStatus(
    val vehicle: Boolean = false,
    val navi: Boolean = false,
    val location: Boolean = false,
    val tts: Boolean = false,
    val stt: Boolean = false,
)

object TripPolicy {
    const val RECOMMEND_TIMEOUT_SEC = 15
    const val MAX_RECOMMEND_PER_TRIP = 3
    const val LONG_DRIVE_MINUTES = 120
    const val VOICE_LISTEN_MS = 5_000L
}
