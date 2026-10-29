package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store

interface StoreRepository {

    /** [here] 기준으로 추천 가능한 매장 목록 (백엔드 연동 전: 가상 매장) */
    suspend fun getRecommendableStores(here: GeoPoint?): List<Store>
}
