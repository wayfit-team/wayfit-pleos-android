package com.wayfit.wayfitpleos.screen.main

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DemoCommand
import com.wayfit.wayfitpleos.data.repository.DemoRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.SdkStatus
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainViewModelTest : JUnit5Test() {

    private val parked = MutableStateFlow<Boolean?>(null)
    private val drivingMinutes = MutableStateFlow<Int?>(null)
    private val coupons = MutableStateFlow(
        listOf(
            Coupon("a", "가게A", "혜택", "111111", 7),
            Coupon("b", "가게B", "혜택", "222222", 0, redeemed = true),
            Coupon("c", "가게C", "혜택", "333333", 0, expired = true),
        )
    )

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var naviRepository: NaviRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var couponRepository: CouponRepository
    private lateinit var pairingRepository: PairingRepository
    private val demoRepository = DemoRepository()

    private lateinit var sut: MainViewModel

    @BeforeEach
    override fun setup() {
        super.setup()
        vehicleRepository = mockk(relaxed = true) {
            every { parked } returns this@MainViewModelTest.parked
            every { drivingMinutes } returns this@MainViewModelTest.drivingMinutes
            every { start() } returns true
        }
        naviRepository = mockk(relaxed = true) { every { start() } returns true }
        locationRepository = mockk(relaxed = true) { every { start(any()) } answers { firstArg() } }
        voiceRepository = mockk(relaxed = true) {
            every { isTtsAvailable } returns true
            every { isSttAvailable } returns false
        }
        couponRepository = mockk(relaxUnitFun = true) { every { coupons } returns this@MainViewModelTest.coupons }
        pairingRepository = mockk(relaxed = true)

        sut = MainViewModel(
            vehicleRepository, naviRepository, locationRepository, voiceRepository,
            couponRepository, pairingRepository, demoRepository, ioDispatcher = testDispatcher,
        )
        testScope.runCurrent()
    }

    @Test
    fun `기어 정보가 없으면 주행 중으로 본다`() {
        assertFalse(sut.stateFlow.value.parked)
    }

    @Test
    fun `P단이면 정차 상태가 된다`() {
        parked.value = true
        testScope.runCurrent()

        assertTrue(sut.stateFlow.value.parked)
    }

    @Test
    fun `사용·만료되지 않은 쿠폰만 센다`() {
        assertEquals(1, sut.stateFlow.value.activeCouponCount)
    }

    @Test
    fun `2시간 이상 운전하면 장시간 운전 상태가 된다`() {
        drivingMinutes.value = 120
        testScope.runCurrent()

        assertTrue(sut.stateFlow.value.longDrive)
    }

    @Test
    fun `SDK 연결 결과를 상태로 모은다`() {
        sut.startSdks(fusedLocationGranted = false)
        testScope.runCurrent()

        assertEquals(
            SdkStatus(vehicle = true, navi = true, location = false, tts = true, stt = false),
            sut.stateFlow.value.sdk,
        )
        verify { voiceRepository.start() }
    }

    @Test
    fun `시작하면 서버에서 쿠폰과 연결된 폰을 받아온다 (첫 실행이면 기기 등록도 이때)`() {
        sut.startSdks(fusedLocationGranted = true)
        testScope.runCurrent()

        coVerify(exactly = 1) { couponRepository.refresh() }
        coVerify(exactly = 1) { pairingRepository.refreshPhones() }
    }

    @Test
    fun `SDK 는 한 번만 시작한다`() {
        sut.startSdks(fusedLocationGranted = true)
        sut.startSdks(fusedLocationGranted = true)
        testScope.runCurrent()

        verify(exactly = 1) { vehicleRepository.start() }
        verify(exactly = 1) { naviRepository.start() }
    }

    @Test
    fun `DEMO 정차 전환은 현재와 반대 상태로 바꾼다`() {
        sut.demoToggleParked()
        verify { vehicleRepository.demoSetParked(true) }
    }

    @Test
    fun `DEMO 장시간 운전 토글`() {
        sut.demoToggleLongDrive()
        verify { vehicleRepository.demoSetDrivingMinutes(130) }
    }

    @Test
    fun `DEMO 추천·도착은 DemoRepository 명령으로 보낸다`() {
        val commands = mutableListOf<DemoCommand>()
        testScope.backgroundScope.launch { demoRepository.commands.collect(commands::add) }
        testScope.runCurrent()

        sut.demoRecommend()
        sut.demoArriveAndPark()
        testScope.runCurrent()

        assertEquals(listOf(DemoCommand.RECOMMEND, DemoCommand.ARRIVE_AND_PARK), commands)
    }

    @Test
    fun `DEMO 폰 연결`() {
        sut.demoPhonePaired()
        verify { pairingRepository.demoPhonePaired() }
    }
}
