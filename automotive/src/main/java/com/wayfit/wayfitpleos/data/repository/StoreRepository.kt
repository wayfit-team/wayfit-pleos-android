package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store

interface StoreRepository {

    /** [here] 기준 앞으로 지나갈 경로 위 추천 매장 (서버 `/stores/nearby`, 우회 거리순) */
    suspend fun getRecommendableStores(here: GeoPoint?): List<Store>

    /** 추천 카드 노출 기록. 기다리지 않고, 실패해도 추천 흐름을 막지 않는다 */
    fun recordImpression(store: Store, tripId: String)
}
