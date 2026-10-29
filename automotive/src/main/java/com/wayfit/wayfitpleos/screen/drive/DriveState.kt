package com.wayfit.wayfitpleos.screen.drive

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.screen.base.State

sealed class DriveState : State {

    /** 경로를 따라가며 들를 곳을 찾는 중 */
    data class Idle(
        val route: RouteSummary,
        val tripRecommendCount: Int,
    ) : DriveState()

    /** 추천 카드 노출 중 (15초 후 자동 닫힘) */
    data class Recommending(
        val store: Store,
        val distanceKm: Double,
        val secondsLeft: Int,
        val reason: String?,
        val listening: Boolean = false,
    ) : DriveState()

    /** "들를게요" 이후 매장으로 가는 중 */
    data class Visiting(
        val store: Store,
        val remainingM: Int,
        val done: Set<VisitStep>,
        val verifying: Boolean = false,
    ) : DriveState()

    /** 방문 인증 완료, 쿠폰 발급 */
    data class Verified(
        val coupon: Coupon,
        val phonePaired: Boolean,
    ) : DriveState()
}
