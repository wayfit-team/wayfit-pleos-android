package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VisitRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.EvidenceType
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.LocationSource
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.VerifyResult
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.testbase.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

internal class VisitUseCaseTest {

    private val store = Fixtures.STORE
    private val fused = MutableStateFlow<GeoPoint?>(store.location.offset(northM = 120.0, eastM = 0.0))
    private val navi = mockk<NaviRepository>(relaxed = true) {
        every { isAvailable } returns true
        every { waypointArrived } returns emptyFlow()
        every { location } returns MutableStateFlow(null)
    }
    private val location = mockk<LocationRepository>(relaxed = true) {
        every { geofenceEntered } returns emptyFlow()
        every { this@mockk.location } returns fused
    }
    private val vehicle = mockk<VehicleRepository>(relaxed = true) {
        every { doorOpened } returns emptyFlow()
        every { parked } returns MutableStateFlow(true)
    }
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    private val pairing = mockk<PairingRepository> { every { phones } returns this@VisitUseCaseTest.phones }
    private val coupons = mockk<CouponRepository>(relaxUnitFun = true)
    private val coupon = Coupon("cpn_1", store.name, store.benefit, "123456", 7)
    private val sent = slot<List<Evidence>>()
    private val visits = mockk<VisitRepository>(relaxUnitFun = true) {
        coEvery { create(any(), any()) } returns "visit_1"
        coEvery { verify(any(), capture(sent)) } returns VerifyResult.Verified(coupon)
    }
    private val trip = TripSession()
    private val sut = VisitUseCase(navi, location, vehicle, coupons, pairing, visits, trip, CurrentLocation(location, navi))

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
        assertTrue(sut.checkWithinRadius(store, distance)) // 기본 반경 150m
        assertFalse(sut.checkWithinRadius(store, 151))
    }

    @Test
    fun `방문을 시작하면 이번 트립으로 서버 방문을 만든다`() = runTest {
        sut.start(store)
        coVerify { visits.create(store.id, trip.tripId) }
    }

    @Test
    fun `서버 방문 생성에 실패하면 경유지·지오펜스를 걸지 않는다`() = runTest {
        coEvery { visits.create(any(), any()) } throws IOException("offline")

        assertThrows<IOException> { sut.start(store) }

        verify(exactly = 0) { navi.addWaypoint(any()) }
        verify(exactly = 0) { location.watch(any()) }
    }

    @Test
    fun `내비가 있는데 경유지 추가에 실패하면 알리고 경유지 단계는 미완료`() = runTest {
        every { navi.addWaypoint(any()) } returns false
        val start = sut.start(store)
        assertTrue(start.waypointFailed)
        assertFalse(start.waypointStepDone)
        verify { location.watch(store) }
    }

    @Test
    fun `반경 안 위치 + P단을 시각·위치·출처와 함께 보내고, 인증되면 쿠폰을 넣고 지오펜스를 푼다`() = runTest {
        phones.value = listOf(PairedPhone("p", "휴대폰 1"))
        sut.start(store)
        sut.checkWithinRadius(store, 120)

        val result = sut.verify()

        assertEquals(listOf(EvidenceType.LOCATION, EvidenceType.GEAR_PARK), sent.captured.map { it.type })
        sent.captured.forEach {
            assertEquals(fused.value, it.location)
            assertEquals(LocationSource.FUSED, it.source)
        }
        verify { location.clearWatch() }
        verify { coupons.add(coupon) }
        assertEquals(VisitResult.Verified(coupon, phonePaired = true), result)
    }

    @Test
    fun `서버가 거절하면 안내 문구를 주고 방문은 남겨 다시 시도할 수 있다`() = runTest {
        coEvery { visits.verify(any(), any()) } returnsMany listOf(
            VerifyResult.Rejected("VISIT_OUT_OF_RADIUS", "반경 밖"),
            VerifyResult.Verified(coupon),
        )
        sut.start(store)

        assertEquals(VisitResult.Rejected("반경 밖"), sut.verify())
        verify(exactly = 0) { location.clearWatch() }
        assertInstanceOf(VisitResult.Verified::class.java, sut.verify())
    }

    @Test
    fun `방문 전이면 인증 요청을 보내지 않는다`() = runTest {
        assertInstanceOf(VisitResult.Rejected::class.java, sut.verify())
        coVerify(exactly = 0) { visits.verify(any(), any()) }
    }

    @Test
    fun `방문을 취소하면 서버 방문도 취소하고, 서버 취소가 실패해도 예외를 내지 않는다`() = runTest {
        coEvery { visits.cancel(any()) } throws IOException("offline")
        sut.start(store)

        sut.cancel()

        coVerify { visits.cancel("visit_1") }
        verify { navi.removeWaypoint() }
        verify { location.clearWatch() }
    }

    @Test
    fun `DEMO 도착은 매장 위치를 증거로 쓴다 (차량은 실제로 움직이지 않음)`() = runTest {
        sut.start(store)
        sut.demoArrive()
        sut.verify()

        sent.captured.forEach {
            assertEquals(store.serverLocation, it.location)
            assertEquals(null, it.source)
        }
    }

    @Test
    fun `매장을 옮겨 보여주는 중이면 증거 위치를 서버 좌표로 바꿔 보낸다`() = runTest {
        val moved = store.copy(location = GeoPoint(37.4854, 127.0364)) // 강남으로 옮긴 매장
        fused.value = moved.location.offset(northM = 30.0, eastM = 0.0)
        sut.start(moved)
        sut.checkWithinRadius(moved, 30)

        sut.verify()

        sent.captured.forEach { assertEquals(30.0, store.serverLocation.distanceTo(it.location!!), 1.0) }
    }
}
