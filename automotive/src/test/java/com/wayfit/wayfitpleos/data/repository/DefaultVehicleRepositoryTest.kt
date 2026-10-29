package com.wayfit.wayfitpleos.data.repository

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.wayfit.wayfitpleos.data.pleos.PleosVehicle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultVehicleRepositoryTest {

    private val scope = TestScope(StandardTestDispatcher())
    private val gear = MutableStateFlow<VehicleGear?>(null)
    private val drivingTime = MutableStateFlow<Int?>(null)

    private fun pleosVehicle(started: Boolean) = mockk<PleosVehicle>(relaxed = true) {
        every { start() } returns started
        every { this@mockk.gear } returns this@DefaultVehicleRepositoryTest.gear
        every { tripDrivingTime } returns drivingTime
        every { doorOpened } returns MutableSharedFlow()
    }

    @Test
    fun `P단이면 parked=true, D단이면 false`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = true), scope)
        assertTrue(sut.start())
        scope.runCurrent()
        assertNull(sut.parked.value)

        gear.value = VehicleGear.GEAR_PARK
        scope.runCurrent()
        assertEquals(true, sut.parked.value)

        gear.value = VehicleGear.GEAR_DRIVE
        scope.runCurrent()
        assertEquals(false, sut.parked.value)
    }

    @Test
    fun `주행 시간을 그대로 전달한다`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = true), scope)
        sut.start()
        drivingTime.value = 42
        scope.runCurrent()

        assertEquals(42, sut.drivingMinutes.value)
    }

    @Test
    fun `SDK 연결에 실패하면 사용 불가로 표시하고 SDK 값을 따르지 않는다`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = false), scope)
        assertFalse(sut.start())
        assertFalse(sut.isAvailable)

        gear.value = VehicleGear.GEAR_PARK
        scope.runCurrent()
        assertNull(sut.parked.value)
    }

    @Test
    fun `stop 하면 SDK 구독을 끊고 사용 불가로 바뀐다 (리뷰 1-4)`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = true), scope)
        sut.start()
        scope.runCurrent()

        sut.stop()
        gear.value = VehicleGear.GEAR_PARK
        scope.runCurrent()

        assertFalse(sut.isAvailable)
        assertNull(sut.parked.value)
    }

    @Test
    fun `start 를 다시 불러도 이전 구독은 정리되고 하나만 동작한다 (리뷰 1-4)`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = true), scope)
        sut.start()
        sut.start()
        scope.runCurrent()

        gear.value = VehicleGear.GEAR_PARK
        scope.runCurrent()
        assertEquals(true, sut.parked.value)

        sut.stop()
        gear.value = VehicleGear.GEAR_DRIVE
        scope.runCurrent()
        assertEquals(true, sut.parked.value) // 첫 start 의 구독이 남아 있었다면 false 로 바뀌었을 것
    }

    @Test
    fun `DEMO 조작은 SDK 없이도 상태를 바꾼다`() {
        val sut = DefaultVehicleRepository(pleosVehicle(started = false), scope)

        sut.demoSetParked(true)
        sut.demoSetDrivingMinutes(130)

        assertEquals(true, sut.parked.value)
        assertEquals(130, sut.drivingMinutes.value)
    }
}
