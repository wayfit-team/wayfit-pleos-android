package com.wayfit.wayfitpleos.data.repository

import android.util.Log
import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.apiCall
import com.wayfit.wayfitpleos.data.remote.dto.CreateImpressionRequest
import com.wayfit.wayfitpleos.data.remote.toModel
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 데모 매장은 아주대학교 → 광교호수공원 경로에 있다.
 *
 * 차량 위치가 데모 지역에서 3km 넘게 떨어져 있으면(에뮬레이터 마운틴뷰 9,000km·Pleos 강남 24km 문제)
 * 데모 지역 기준으로 조회한 뒤 매장을 현재 위치 기준 같은 거리·방향으로 옮겨 어디서든 시연되게 한다.
 * 옮긴 매장은 [Store.serverLocation] 에 원래 좌표를 들고 있어 방문 증거를 서버 좌표로 바꿔 보낼 수 있다.
 */
@Singleton
class DefaultStoreRepository @Inject constructor(
    private val api: WayfitApi,
    @ApplicationScope private val scope: CoroutineScope,
) : StoreRepository {

    override suspend fun getRecommendableStores(here: GeoPoint?): List<Store> {
        val inDemoArea = here != null && here.distanceTo(DEMO_ORIGIN) <= DEMO_AREA_RADIUS_M
        val origin = if (inDemoArea) here!! else DEMO_ORIGIN
        // 목적지가 없으면 서버가 우회 거리를 왕복 거리로 잡는다. 내비 목적지 좌표를 아직 받지 않아 데모 경로 목적지를 쓴다
        val stores = apiCall { api.nearbyStores(origin.lat, origin.lng, DEMO_DEST.lat, DEMO_DEST.lng) }.stores.map { it.toModel() }
        if (here == null || inDemoArea) return stores
        // 위경도를 그대로 더하면 위도가 다른 곳에서 동서 거리가 틀어진다 → 미터 단위로 옮긴다
        return stores.map { store ->
            val (northM, eastM) = store.location.northEastFrom(DEMO_ORIGIN)
            store.copy(location = here.offset(northM, eastM))
        }
    }

    override fun recordImpression(store: Store, tripId: String) {
        scope.launch {
            runCatching { apiCall { api.createImpression(CreateImpressionRequest(store.id, tripId)) } }
                .onFailure { Log.w(TAG, "impression failed: ${store.id}", it) }
        }
    }

    companion object {
        private const val TAG = "StoreRepository"

        /** 아주대학교 */
        val DEMO_ORIGIN = GeoPoint(37.2830, 127.0443)

        /** 광교호수공원 */
        val DEMO_DEST = GeoPoint(37.2850, 127.0660)

        const val DEMO_AREA_RADIUS_M = 3_000.0
    }
}
