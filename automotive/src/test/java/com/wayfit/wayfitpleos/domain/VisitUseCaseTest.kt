package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DefaultStoreRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.VisitStep
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VisitUseCaseTest {

    private val store = DefaultStoreRepository.DEMO_STORES.first()
    private val navi = mockk<NaviRepository>(relaxed = true) {
        every { isAvailable } returns true
        every { waypointArrived } returns emptyFlow()
        every { location } returns MutableStateFlow(null)
    }
    private val location = mockk<LocationRepository>(relaxed = true) {
        every { geofenceEntered } returns emptyFlow()
        every { this@mockk.location } returns MutableStateFlow(store.location.offset(northM = 120.0, eastM = 0.0))
    }
    private val vehicle = mockk<VehicleRepository>(relaxed = true) {
        every { doorOpened } returns emptyFlow()
        every { parked } returns MutableStateFlow(true)
    }
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    private val pairing = mockk<PairingRepository> { every { phones } returns this@VisitUseCaseTest.phones }
    private val coupons = mockk<CouponRepository> {
        coEvery { issue(any()) } answers { Coupon("c", store.name, store.benefit, "123456", 7) }
    }
    private val sut = VisitUseCase(navi, location, vehicle, coupons, pairing, CurrentLocation(location, navi))

    @Test
    fun `인증은 도착 + P단 + 인증 중 아님일 때만`() {
        val arrived = setOf(VisitStep.WAYPOINT, VisitStep.ARRIVED)
        assertTrue(sut.canVerify(arrived, verifying = false, parked = true))
        assertFalse(sut.canVerify(setOf(VisitStep.WAYPOINT), verifying = false, parked = true))
        assertFalse(sut.canVerify(arrived, verifying = false, parked = false))
        assertFalse(sut.canVerify(arrived, verifying = true, parked = true))
    }

    @Test
    fun `반경 판정과 남은 거리는 현재 위치 기준`() {
        val distance = sut.distanceTo(store)!!
        assertEquals(120.0, distance.toDouble(), 2.0)
        assertTrue(sut.isWithinRadius(store, distance)) // 기본 반경 150m
    }

    @Test
    fun `내비가 있는데 경유지 추가에 실패하면 알리고 경유지 단계는 미완료`() {
        every { navi.addWaypoint(any()) } returns false
        val start = sut.start(store)
        assertTrue(start.waypointFailed)
        assertFalse(start.waypointStepDone)
        verify { location.watch(store) }
    }

    @Test
    fun `인증하면 지오펜스를 풀고 쿠폰을 발급하며 폰 연결 여부를 함께 준다`() = runTest {
        phones.value = listOf(PairedPhone("p", "Galaxy S25", 0L))
        val result = sut.verify(store)

        verify { location.clearWatch() }
        assertEquals(store.name, result.coupon.storeName)
        assertTrue(result.phonePaired)
    }
}
