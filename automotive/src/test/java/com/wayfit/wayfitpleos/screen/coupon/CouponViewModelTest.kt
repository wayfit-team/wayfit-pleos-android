package com.wayfit.wayfitpleos.screen.coupon

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DefaultPairingRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.testbase.Fixtures
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class CouponViewModelTest : JUnit5Test() {

    private val coupons = MutableStateFlow(listOf(Coupon("a", "가게A", "혜택", "111111", 7)))
    private val couponRepository = mockk<CouponRepository>(relaxUnitFun = true) {
        every { coupons } returns this@CouponViewModelTest.coupons
    }
    private val server = Fixtures.PairingServer()

    @Test
    fun `들어오면 쿠폰과 연결된 폰을 서버에서 새로 받는다`() {
        server.linked = 1
        val pairing = DefaultPairingRepository(server.api, testScope.backgroundScope)
        val sut = CouponViewModel(couponRepository, pairing)
        testScope.runCurrent()

        coVerify { couponRepository.refresh() }
        assertEquals(CouponState(coupons.value, phoneCount = 1), sut.stateFlow.value)
    }

    @Test
    fun `쿠폰을 못 받아와도 갖고 있던 목록을 보여준다`() {
        coEvery { couponRepository.refresh() } throws IOException("offline")
        val sut = CouponViewModel(couponRepository, DefaultPairingRepository(server.api, testScope.backgroundScope))
        testScope.runCurrent()

        assertEquals(coupons.value, sut.stateFlow.value.coupons)
    }

    @Test
    fun `쿠폰과 연결된 폰 수가 바뀌면 화면 상태도 바뀐다`() {
        val pairing = DefaultPairingRepository(server.api, testScope.backgroundScope)
        val sut = CouponViewModel(couponRepository, pairing)
        testScope.runCurrent()

        val newCoupon = Coupon("b", "가게B", "혜택2", "222222", 7)
        coupons.value = listOf(newCoupon) + coupons.value
        pairing.demoPhonePaired()
        testScope.runCurrent()

        assertEquals(newCoupon, sut.stateFlow.value.coupons.first())
        assertEquals(1, sut.stateFlow.value.phoneCount)
    }

    @Test
    fun `다른 폰 연결 코드를 띄웠다가 그냥 나와도 연결된 폰 수는 그대로 (리뷰 1-2)`() {
        server.linked = 1
        val pairing = DefaultPairingRepository(server.api, testScope.backgroundScope)
        val sut = CouponViewModel(couponRepository, pairing)
        testScope.runCurrent()

        testScope.launch { pairing.requestCode() }
        testScope.runCurrent()

        assertEquals(1, sut.stateFlow.value.phoneCount)
    }
}
