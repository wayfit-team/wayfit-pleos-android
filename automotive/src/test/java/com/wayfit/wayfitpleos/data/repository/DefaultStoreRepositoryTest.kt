package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DefaultStoreRepositoryTest {

    private val sut = DefaultStoreRepository()

    @Test
    fun `데모 지역 안이면 원래 매장 좌표를 그대로 쓴다`() = runTest {
        val stores = sut.getRecommendableStores(DefaultStoreRepository.DEMO_ORIGIN)
        assertEquals(DefaultStoreRepository.DEMO_STORES, stores)
    }

    @Test
    fun `위치를 모르면 원래 매장 좌표를 쓴다`() = runTest {
        assertEquals(DefaultStoreRepository.DEMO_STORES, sut.getRecommendableStores(null))
    }

    @Test
    fun `Pleos 에뮬레이터 위치(강남, 약 24km)도 다시 배치한다`() = runTest {
        val gangnam = GeoPoint(37.4854, 127.0364)
        sut.getRecommendableStores(gangnam).forEach { store ->
            assertTrue(gangnam.distanceTo(store.location) < 3_000, store.name)
        }
    }

    @Test
    fun `데모 지역에서 멀면 현재 위치 근처로 다시 배치한다 (에뮬레이터 마운틴뷰 9000km 문제)`() = runTest {
        val mountainView = GeoPoint(37.4220, -122.0841)
        val stores = sut.getRecommendableStores(mountainView)

        stores.zip(DefaultStoreRepository.DEMO_STORES).forEach { (moved, original) ->
            val movedDistance = mountainView.distanceTo(moved.location)
            val originalDistance = DefaultStoreRepository.DEMO_ORIGIN.distanceTo(original.location)
            assertTrue(movedDistance < 3_000, "${moved.name}: ${movedDistance}m")
            // 미터 단위로 옮기므로 위도가 달라도 거리가 보존된다 (리뷰 2-3)
            assertEquals(originalDistance, movedDistance, 5.0)
        }
    }
}
