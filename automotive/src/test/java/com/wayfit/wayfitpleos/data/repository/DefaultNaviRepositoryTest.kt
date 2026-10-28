package com.wayfit.wayfitpleos.data.repository

import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import com.wayfit.wayfitpleos.data.pleos.PleosNavi
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.RouteSummary
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultNaviRepositoryTest {

    private val scope = TestScope(StandardTestDispatcher())
    private val location = MutableStateFlow<CurrentLocationInfo?>(null)
    private val destination = MutableStateFlow<DestinationInfo?>(null)

    private val sut = DefaultNaviRepository(
        mockk<PleosNavi>(relaxed = true) {
            every { this@mockk.location } returns this@DefaultNaviRepositoryTest.location
            every { this@mockk.destination } returns this@DefaultNaviRepositoryTest.destination
            every { waypointArrived } returns MutableSharedFlow()
        },
        scope.backgroundScope,
    )

    private fun destination(name: String, remainDistanceM: Int, remainTimeSec: Int) = DestinationInfo(
        37.2836, 127.0655, null, null, name, "경기 수원시", remainDistanceM, remainTimeSec, 0, "", "0", "",
    )

    @Test
    fun `내비 목적지를 km·분 단위 경로 요약으로 바꾼다`() {
        destination.value = destination("광교호수공원", remainDistanceM = 12_400, remainTimeSec = 1_080)
        scope.runCurrent()

        assertEquals(RouteSummary("광교호수공원", remainingKm = 12.4, etaMin = 18), sut.route.value)
    }

    @Test
    fun `목적지 이름이 비어 있으면 경로 안내가 없는 것으로 본다`() {
        destination.value = destination("", remainDistanceM = 0, remainTimeSec = 0)
        scope.runCurrent()

        assertNull(sut.route.value)
    }

    @Test
    fun `내비 현재 위치를 좌표로 바꾼다`() {
        location.value = CurrentLocationInfo(37.2830, 127.0443, 90.0, "아주대학교", "route-1", 0)
        scope.runCurrent()

        assertEquals(GeoPoint(37.2830, 127.0443), sut.location.value)
    }
}
