package com.wayfit.wayfitpleos.screen.drive

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.domain.CurrentLocation
import com.wayfit.wayfitpleos.domain.RecommendStoreUseCase
import com.wayfit.wayfitpleos.domain.TripSession
import com.wayfit.wayfitpleos.domain.VisitUseCase
import com.wayfit.wayfitpleos.domain.VoiceRecommendation
import com.wayfit.wayfitpleos.data.repository.DemoCommand
import com.wayfit.wayfitpleos.data.repository.DemoRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.data.repository.StoreRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VisitRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.EvidenceType
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.TripPolicy
import com.wayfit.wayfitpleos.model.VerifyResult
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.testbase.Fixtures
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DriveViewModelTest : JUnit5Test() {

    private val store = Fixtures.STORE

    /** 매장에서 약 700m 떨어진 지점 — 거리 판정만으로는 도착하지 않음 */
    private val here = store.location.offset(northM = 700.0, eastM = 0.0)

    private val parked = MutableStateFlow<Boolean?>(false)
    private val drivingMinutes = MutableStateFlow<Int?>(null)
    private val fusedLocation = MutableStateFlow<GeoPoint?>(here)
    private val geofenceEntered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val waypointArrived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val naviRoute = MutableStateFlow<RouteSummary?>(null)
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    private val voiceGuide = MutableStateFlow(true)

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var naviRepository: NaviRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var couponRepository: CouponRepository
    private lateinit var visitRepository: VisitRepository
    private lateinit var pairingRepository: PairingRepository
    private lateinit var settingsRepository: SettingsRepository
    private val demoRepository = DemoRepository()

    private lateinit var sut: DriveViewModel

    @BeforeEach
    override fun setup() {
        super.setup()
        vehicleRepository = mockk(relaxed = true) {
            every { parked } returns this@DriveViewModelTest.parked
            // 실제 구현처럼 DEMO 주차도 parked 스트림을 바꾼다
            every { demoSetParked(any()) } answers { this@DriveViewModelTest.parked.value = firstArg() }
            every { drivingMinutes } returns this@DriveViewModelTest.drivingMinutes
            every { doorOpened } returns emptyFlow()
        }
        naviRepository = mockk(relaxed = true) {
            every { isAvailable } returns true
            every { location } returns MutableStateFlow(null)
            every { route } returns naviRoute
            every { waypointArrived } returns this@DriveViewModelTest.waypointArrived
            every { addWaypoint(any()) } returns true
        }
        locationRepository = mockk(relaxed = true) {
            every { location } returns fusedLocation
            every { geofenceEntered } returns this@DriveViewModelTest.geofenceEntered
        }
        voiceRepository = mockk(relaxed = true) {
            coEvery { listen(any()) } returns null
        }
        storeRepository = mockk(relaxUnitFun = true) {
            coEvery { getRecommendableStores(any()) } returns listOf(store)
        }
        couponRepository = mockk(relaxUnitFun = true)
        visitRepository = mockk(relaxUnitFun = true) {
            coEvery { create(any(), any()) } returns VISIT_ID
            coEvery { verify(any(), any()) } returns VerifyResult.Verified(coupon(store))
        }
        pairingRepository = mockk(relaxed = true) { every { phones } returns this@DriveViewModelTest.phones }
        settingsRepository = mockk(relaxed = true) { every { voiceGuideEnabled } returns voiceGuide }

        val currentLocation = CurrentLocation(locationRepository, naviRepository)
        val trip = TripSession()
        sut = DriveViewModel(
            recommendStore = RecommendStoreUseCase(storeRepository, vehicleRepository, currentLocation, trip),
            visit = VisitUseCase(
                naviRepository, locationRepository, vehicleRepository, couponRepository,
                pairingRepository, visitRepository, trip, currentLocation,
            ),
            voice = VoiceRecommendation(voiceRepository, settingsRepository),
            naviRepository = naviRepository,
            demoRepository = demoRepository,
        )
        testScope.runCurrent() // init 의 collector 들이 구독을 시작하도록
    }

    private fun coupon(store: Store) = Coupon("c-test", store.name, store.benefit, code = "123456", expiresInDays = 7)

    private fun collectEvents(): List<DriveEvent> {
        val events = mutableListOf<DriveEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(events::add) }
        testScope.runCurrent()
        return events
    }

    private fun arriveAndPark() {
        geofenceEntered.tryEmit(Unit)
        testScope.runCurrent()
        parked.value = true
        testScope.runCurrent()
        testScope.advanceTimeBy(1_300)
        testScope.runCurrent()
    }

    private fun recommend() {
        demoRepository.send(DemoCommand.RECOMMEND)
        testScope.runCurrent()
    }

    private fun recommendAndAccept() {
        recommend()
        sut.accept()
        testScope.runCurrent()
    }

    // region 대기

    @Test
    fun `처음에는 내비 안내가 없으면 데모 경로로 대기한다`() {
        val state = sut.stateFlow.value as DriveState.Idle
        assertEquals("광교호수공원", state.route.destination)
        assertEquals(0, state.tripRecommendCount)
    }

    @Test
    fun `내비 목적지가 들어오면 대기 화면 경로가 바뀐다`() {
        naviRoute.value = RouteSummary("수원역", remainingKm = 3.2, etaMin = 9)
        testScope.runCurrent()

        assertEquals("수원역", (sut.stateFlow.value as DriveState.Idle).route.destination)
    }

    // endregion

    // region 추천

    @Test
    fun `추천하면 현재 위치 기준 거리로 카드를 띄운다`() {
        recommend()

        val state = sut.stateFlow.value as DriveState.Recommending
        assertEquals(store, state.store)
        assertEquals(0.7, state.distanceKm, 0.01)
        assertEquals(TripPolicy.RECOMMEND_TIMEOUT_SEC, state.secondsLeft)
        assertNull(state.reason)
    }

    @Test
    fun `추천 카드를 띄우면 노출을 기록한다`() {
        recommend()

        verify(exactly = 1) { storeRepository.recordImpression(store, match { it.startsWith("trip_") }) }
    }

    @Test
    fun `경로 위에 매장이 없으면 알리고 대기를 유지한다`() {
        coEvery { storeRepository.getRecommendableStores(any()) } returns emptyList()
        val events = collectEvents()

        recommend()

        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
        assertEquals(listOf(DriveEvent.ShowMessage("가는 길에 추천할 매장이 없어요")), events)
    }

    @Test
    fun `매장 조회에 실패하면 알리고 대기를 유지한다`() {
        coEvery { storeRepository.getRecommendableStores(any()) } throws IOException("offline")
        val events = collectEvents()

        recommend()

        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
        assertEquals(listOf(DriveEvent.ShowMessage("추천 매장을 불러오지 못했어요")), events)
    }

    @Test
    fun `2시간 넘게 운전 중이면 쉬어가라는 이유를 붙인다`() {
        drivingMinutes.value = 130
        recommend()

        assertEquals("2시간째 운전 중이에요 · 잠깐 쉬어가세요", (sut.stateFlow.value as DriveState.Recommending).reason)
    }

    @Test
    fun `15초 동안 반응이 없으면 카드를 닫고 추천 횟수는 남긴다`() {
        recommend()

        testScope.advanceTimeBy(TripPolicy.RECOMMEND_TIMEOUT_SEC * 1_000L + 100)
        testScope.runCurrent()

        val state = sut.stateFlow.value as DriveState.Idle
        assertEquals(1, state.tripRecommendCount)
    }

    @Test
    fun `추천을 음성으로 읽어준다`() {
        recommend()

        coVerify { voiceRepository.speak(match { it.contains(store.name) && it.endsWith("들를까요?") }) }
    }

    @Test
    fun `음성 안내를 끄면 읽지도 듣지도 않는다`() {
        voiceGuide.value = false
        recommend()

        coVerify(exactly = 0) { voiceRepository.speak(any()) }
        coVerify(exactly = 0) { voiceRepository.listen(any()) }
    }

    @Test
    fun `음성으로 "응"이라고 답하면 수락한다`() {
        coEvery { voiceRepository.listen(any()) } returns "응"
        recommend()

        assertInstanceOf(DriveState.Visiting::class.java, sut.stateFlow.value)
    }

    @Test
    fun `음성으로 "아니"라고 답하면 거절한다`() {
        coEvery { voiceRepository.listen(any()) } returns "아니 괜찮아요"
        recommend()

        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
    }

    @Test
    fun `알아들을 수 없는 답이면 카드를 유지한다`() {
        coEvery { voiceRepository.listen(any()) } returns "오늘 날씨 어때"
        recommend()

        assertInstanceOf(DriveState.Recommending::class.java, sut.stateFlow.value)
    }

    // endregion

    // region 방문

    @Test
    fun `들를게요를 누르면 서버에 방문을 만들고 경유지를 추가하고 지오펜스를 건다`() {
        recommendAndAccept()

        val state = sut.stateFlow.value as DriveState.Visiting
        assertEquals(setOf(VisitStep.WAYPOINT), state.done)
        assertEquals(700.0, state.remainingM.toDouble(), 5.0)
        coVerify { visitRepository.create(store.id, match { it.startsWith("trip_") }) }
        verify { naviRepository.addWaypoint(store) }
        verify { locationRepository.watch(store) }
    }

    @Test
    fun `서버 방문 생성에 실패하면 경유지를 걸지 않고 알린 뒤 대기로 돌아간다`() {
        coEvery { visitRepository.create(any(), any()) } throws IOException("offline")
        val events = collectEvents()

        recommendAndAccept()

        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
        verify(exactly = 0) { naviRepository.addWaypoint(any()) }
        assertEquals(listOf(DriveEvent.ShowMessage("방문을 시작하지 못했어요. 잠시 후 다시 시도해 주세요")), events)
    }

    @Test
    fun `경유지 추가에 실패하면 알리고 경유지 단계는 완료하지 않는다`() {
        every { naviRepository.addWaypoint(any()) } returns false
        val events = collectEvents()

        recommendAndAccept()

        assertFalse(VisitStep.WAYPOINT in (sut.stateFlow.value as DriveState.Visiting).done)
        assertEquals(listOf(DriveEvent.ShowMessage("내비에 경유지를 추가하지 못했어요")), events)
    }

    @Test
    fun `방문을 취소하면 서버 방문도 취소하고 경유지와 지오펜스를 지우고 대기로 돌아간다`() {
        recommendAndAccept()

        sut.cancelVisit()
        testScope.runCurrent()

        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
        verify { naviRepository.removeWaypoint() }
        verify { locationRepository.clearWatch() }
        coVerify { visitRepository.cancel(VISIT_ID) }
    }

    // endregion

    // region 인증

    @Test
    fun `지오펜스 진입 후 P단이면 모은 증거로 서버에 인증하고 쿠폰을 받는다`() {
        recommendAndAccept()

        geofenceEntered.tryEmit(Unit)
        testScope.runCurrent()
        assertTrue(VisitStep.ARRIVED in (sut.stateFlow.value as DriveState.Visiting).done)

        parked.value = true
        testScope.runCurrent()
        assertTrue((sut.stateFlow.value as DriveState.Visiting).verifying)

        testScope.advanceTimeBy(1_300)
        testScope.runCurrent()

        val state = sut.stateFlow.value as DriveState.Verified
        assertEquals(store.name, state.coupon.storeName)
        assertFalse(state.phonePaired)
        coVerify {
            visitRepository.verify(VISIT_ID, match { evidence -> evidence.map { it.type } == listOf(EvidenceType.GEOFENCE_ENTER, EvidenceType.GEAR_PARK) })
        }
        verify { couponRepository.add(state.coupon) }
        coVerify { voiceRepository.speak("${store.name} 쿠폰이 도착했어요") }
    }

    @Test
    fun `내비 경유지 도착 이벤트도 도착으로 인정한다`() {
        recommendAndAccept()

        waypointArrived.tryEmit(Unit)
        testScope.runCurrent()

        assertTrue(VisitStep.ARRIVED in (sut.stateFlow.value as DriveState.Visiting).done)
    }

    @Test
    fun `매장 반경 안으로 들어오면 앱이 직접 도착으로 판정한다`() {
        recommendAndAccept()

        fusedLocation.value = store.location.offset(northM = 50.0, eastM = 0.0)
        testScope.advanceTimeBy(3_100)
        testScope.runCurrent()

        assertTrue(VisitStep.ARRIVED in (sut.stateFlow.value as DriveState.Visiting).done)
    }

    @Test
    fun `도착 전에 P단을 넣어도 인증하지 않는다`() {
        recommendAndAccept()

        parked.value = true
        testScope.advanceTimeBy(2_000)
        testScope.runCurrent()

        val state = sut.stateFlow.value as DriveState.Visiting
        assertFalse(state.verifying)
        coVerify(exactly = 0) { visitRepository.verify(any(), any()) }
    }

    @Test
    fun `서버가 인증을 거절하면 안내하고 방문은 유지한 채 주차 단계만 되돌린다`() {
        coEvery { visitRepository.verify(any(), any()) } returns
            VerifyResult.Rejected("VISIT_OUT_OF_RADIUS", "매장 인증 반경 내 위치 증거가 확인되지 않았습니다")
        val events = collectEvents()
        recommendAndAccept()

        arriveAndPark()

        val state = sut.stateFlow.value as DriveState.Visiting
        assertFalse(state.verifying)
        assertFalse(VisitStep.PARKED in state.done)
        assertTrue(VisitStep.ARRIVED in state.done)
        assertEquals(listOf(DriveEvent.ShowMessage("매장 인증 반경 내 위치 증거가 확인되지 않았습니다")), events)
    }

    @Test
    fun `거절된 뒤 다시 P단에 넣으면 다시 인증을 요청한다`() {
        coEvery { visitRepository.verify(any(), any()) } returnsMany listOf(
            VerifyResult.Rejected("VISIT_NO_PARKING", "주차 미확인"),
            VerifyResult.Verified(coupon(store)),
        )
        recommendAndAccept()
        arriveAndPark()

        parked.value = false
        testScope.runCurrent()
        parked.value = true
        testScope.runCurrent()
        testScope.advanceTimeBy(1_300)
        testScope.runCurrent()

        assertInstanceOf(DriveState.Verified::class.java, sut.stateFlow.value)
        coVerify(exactly = 2) { visitRepository.verify(VISIT_ID, any()) }
    }

    @Test
    fun `인증 요청이 네트워크 오류로 실패하면 안내하고 방문을 유지한다`() {
        coEvery { visitRepository.verify(any(), any()) } throws IOException("offline")
        val events = collectEvents()
        recommendAndAccept()

        arriveAndPark()

        assertInstanceOf(DriveState.Visiting::class.java, sut.stateFlow.value)
        assertEquals(listOf(DriveEvent.ShowMessage("인증 요청을 보내지 못했어요")), events)
    }

    @Test
    fun `이미 P단인 채로 도착하면 바로 인증한다`() {
        recommendAndAccept()
        parked.value = true
        testScope.runCurrent()

        geofenceEntered.tryEmit(Unit)
        testScope.runCurrent()
        testScope.advanceTimeBy(1_300)
        testScope.runCurrent()

        assertInstanceOf(DriveState.Verified::class.java, sut.stateFlow.value)
    }

    @Test
    fun `폰이 연결돼 있으면 쿠폰 도착 화면에 표시한다`() {
        phones.value = listOf(PairedPhone("p1", "Galaxy S25", 0L))
        recommendAndAccept()
        geofenceEntered.tryEmit(Unit)
        parked.value = true
        testScope.runCurrent()
        testScope.advanceTimeBy(1_300)
        testScope.runCurrent()

        assertTrue((sut.stateFlow.value as DriveState.Verified).phonePaired)
    }

    @Test
    fun `폰으로 받기를 누르면 연결 화면을 연다`() {
        val received = mutableListOf<DriveEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(received::add) }
        testScope.runCurrent()

        sut.sendToPhone()
        testScope.runCurrent()

        assertEquals(listOf(DriveEvent.OpenPairing), received)
        assertInstanceOf(DriveState.Idle::class.java, sut.stateFlow.value)
    }

    // endregion

    @Test
    fun `DEMO 도착·주차는 실제 이벤트와 같은 경로로 인증까지 간다`() {
        recommendAndAccept()

        demoRepository.send(DemoCommand.ARRIVE_AND_PARK)
        testScope.advanceTimeBy(5_000)
        testScope.runCurrent()

        assertInstanceOf(DriveState.Verified::class.java, sut.stateFlow.value)
        verify { vehicleRepository.demoSetParked(true) }
        // P단 스트림 한 경로로만 인증 → 요청은 한 번 (리뷰: onParked 중복 호출)
        coVerify(exactly = 1) { visitRepository.verify(any(), any()) }
    }

    @Test
    fun `DEMO 도착은 차량이 움직이지 않았으므로 매장 위치를 증거 위치로 보낸다`() {
        recommendAndAccept()

        demoRepository.send(DemoCommand.ARRIVE_AND_PARK)
        testScope.advanceTimeBy(5_000)
        testScope.runCurrent()

        coVerify {
            visitRepository.verify(VISIT_ID, match { evidence ->
                evidence.map { it.type } == listOf(EvidenceType.LOCATION, EvidenceType.GEAR_PARK) &&
                    evidence.all { it.location == store.serverLocation }
            })
        }
    }

    private companion object {
        const val VISIT_ID = "visit_1"
    }
}
