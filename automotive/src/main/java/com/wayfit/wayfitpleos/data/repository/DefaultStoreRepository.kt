package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.StoreCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 백엔드(/stores/nearby) 연동 전 가상 매장. 데모 경로: 아주대학교 → 광교호수공원.
 *
 * 차량 위치가 데모 지역에서 3km 넘게 떨어져 있으면(에뮬레이터 마운틴뷰 9,000km·Pleos 강남 24km 문제)
 * 매장을 현재 위치 기준 같은 거리·방향으로 다시 배치해 어디서든 시연되게 한다.
 */
@Singleton
class DefaultStoreRepository @Inject constructor() : StoreRepository {

    override suspend fun getRecommendableStores(here: GeoPoint?): List<Store> {
        if (here == null || here.distanceTo(DEMO_ORIGIN) <= DEMO_AREA_RADIUS_M) return DEMO_STORES
        // 위경도를 그대로 더하면 위도가 다른 곳에서 동서 거리가 틀어진다 → 미터 단위로 옮긴다
        return DEMO_STORES.map { store ->
            val (northM, eastM) = store.location.northEastFrom(DEMO_ORIGIN)
            store.copy(location = here.offset(northM, eastM))
        }
    }

    companion object {
        /** 아주대학교 */
        val DEMO_ORIGIN = GeoPoint(37.2830, 127.0443)
        const val DEMO_AREA_RADIUS_M = 3_000.0

        val DEMO_STORES = listOf(
            Store(
                "s1", "광교 모퉁이커피", StoreCategory.CAFE, "아메리카노 1잔 무료",
                location = GeoPoint(37.2842, 127.0530), aheadKm = 0.8, detourMin = 3,
            ),
            Store(
                "s2", "원천동 손칼국수", StoreCategory.FOOD, "공깃밥·음료 서비스",
                location = GeoPoint(37.2790, 127.0480), aheadKm = 0.6, detourMin = 4,
            ),
            Store(
                "s3", "매탄 아침빵집", StoreCategory.BAKERY, "소금빵 1개 증정",
                location = GeoPoint(37.2700, 127.0450), aheadKm = 1.4, detourMin = 2,
            ),
        )
    }
}
