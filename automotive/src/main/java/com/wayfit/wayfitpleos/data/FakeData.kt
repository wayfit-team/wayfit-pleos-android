package com.wayfit.wayfitpleos.data

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.StoreCategory

/** PoC용 가짜 데이터 (백엔드 연동 전). 데모 경로: 아주대학교 → 광교호수공원. 매장은 가상 */
object FakeData {

    val route = RouteSummary(
        destination = "광교호수공원",
        remainingKm = 12.4,
        etaMin = 18,
        drivingMinutes = 35,
    )

    val stores = listOf(
        Store(
            "s1", "광교 모퉁이커피", StoreCategory.CAFE, "아메리카노 1잔 무료",
            location = GeoPoint(37.2842, 127.0530), aheadKm = 1.2, detourMin = 3,
        ),
        Store(
            "s2", "원천동 손칼국수", StoreCategory.FOOD, "공깃밥·음료 서비스",
            location = GeoPoint(37.2790, 127.0480), aheadKm = 2.4, detourMin = 4,
        ),
        Store(
            "s3", "매탄 아침빵집", StoreCategory.BAKERY, "소금빵 1개 증정",
            location = GeoPoint(37.2700, 127.0450), aheadKm = 0.9, detourMin = 2,
        ),
    )

    val coupons = listOf(
        Coupon("c1", "아주대 앞 브루잉랩", "음료 사이즈 업", code = "482913", expiresInDays = 5),
        Coupon("c2", "영통 떡볶이연구소", "튀김 1인분 서비스", code = "730154", expiresInDays = 0, redeemed = true),
    )
}
