package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.dto.CreateImpressionRequest
import com.wayfit.wayfitpleos.data.remote.dto.NearbyStoreDto
import com.wayfit.wayfitpleos.data.remote.dto.NearbyStoresResponse
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.testbase.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultStoreRepositoryTest {

    private val serverStores = listOf(
        NearbyStoreDto("store_02", "광교 수제소금빵집", "BAKERY", "소금빵 1개", 37.2828, 127.0538, distanceM = 841, detourM = 15),
        NearbyStoreDto("store_03", "법조타운 곰탕집", "FOOD", "수육 맛보기", 37.2840, 127.0572, distanceM = 1147, detourM = 0),
    )
    private val api = mockk<WayfitApi>(relaxed = true) {
        coEvery { nearbyStores(any(), any(), any(), any()) } returns NearbyStoresResponse(serverStores)
    }
    private val scope = TestScope(StandardTestDispatcher())
    private val sut = DefaultStoreRepository(api, scope.backgroundScope)

    @Test
    fun `데모 지역 안이면 현재 위치로 조회하고 서버 좌표를 그대로 쓴다`() = runTest {
        val here = GeoPoint(37.2835, 127.0450)
        val stores = sut.getRecommendableStores(here)

        val dest = DefaultStoreRepository.DEMO_DEST
        coVerify { api.nearbyStores(here.lat, here.lng, dest.lat, dest.lng) }
        assertEquals(listOf("store_02", "store_03"), stores.map { it.id })
        stores.forEach { assertEquals(it.serverLocation, it.location) }
    }

    @Test
    fun `서버 응답을 매장 모델로 바꾼다 (직선 거리 km, 우회 분)`() = runTest {
        val store = sut.getRecommendableStores(DefaultStoreRepository.DEMO_ORIGIN).first()

        assertEquals("광교 수제소금빵집", store.name)
        assertEquals(0.841, store.aheadKm, 0.001)
        assertEquals(1, store.detourMin) // 15m 우회도 최소 1분
    }

    @Test
    fun `위치를 모르면 데모 지역 기준으로 조회한다`() = runTest {
        sut.getRecommendableStores(null)

        val origin = DefaultStoreRepository.DEMO_ORIGIN
        val dest = DefaultStoreRepository.DEMO_DEST
        coVerify { api.nearbyStores(origin.lat, origin.lng, dest.lat, dest.lng) }
    }

    @Test
    fun `Pleos 에뮬레이터 위치(강남, 약 24km)도 다시 배치한다`() = runTest {
        val gangnam = GeoPoint(37.4854, 127.0364)
        sut.getRecommendableStores(gangnam).forEach { store ->
            assertTrue(gangnam.distanceTo(store.location) < 3_000, store.name)
        }
    }

    @Test
    fun `데모 지역에서 멀면 현재 위치 근처로 옮기고 서버 좌표는 따로 들고 있는다 (에뮬레이터 마운틴뷰 9000km 문제)`() = runTest {
        val mountainView = GeoPoint(37.4220, -122.0841)
        val stores = sut.getRecommendableStores(mountainView)

        stores.zip(serverStores).forEach { (moved, original) ->
            val server = GeoPoint(original.lat, original.lng)
            val movedDistance = mountainView.distanceTo(moved.location)
            val originalDistance = DefaultStoreRepository.DEMO_ORIGIN.distanceTo(server)
            assertTrue(movedDistance < 3_000, "${moved.name}: ${movedDistance}m")
            // 미터 단위로 옮기므로 위도가 달라도 거리가 보존된다 (리뷰 2-3)
            assertEquals(originalDistance, movedDistance, 5.0)
            assertEquals(server, moved.serverLocation)
        }
    }

    @Test
    fun `옮긴 매장 근처의 차량 위치는 서버 매장 근처 좌표로 바뀐다`() = runTest {
        val mountainView = GeoPoint(37.4220, -122.0841)
        val store = sut.getRecommendableStores(mountainView).first()
        val nearMovedStore = store.location.offset(northM = 40.0, eastM = -30.0)

        val serverPoint = store.toServerPoint(nearMovedStore)

        assertEquals(50.0, store.serverLocation.distanceTo(serverPoint), 1.0)
    }

    @Test
    fun `노출 기록은 매장·트립으로 보내고 실패해도 예외를 내지 않는다`() {
        coEvery { api.createImpression(any()) } throws IOException("offline")

        sut.recordImpression(Fixtures.STORE, "trip_1")
        scope.runCurrent()

        coVerify { api.createImpression(CreateImpressionRequest(Fixtures.STORE.id, "trip_1")) }
    }
}
