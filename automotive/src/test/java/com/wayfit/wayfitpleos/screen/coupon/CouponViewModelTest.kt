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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `화면이 보이면 바로 쿠폰과 연결된 폰을 서버에서 받는다`() {
        server.linked = 1
        val sut = CouponViewModel(couponRepository, DefaultPairingRepository(server.api, testScope.backgroundScope))
        testScope.backgroundScope.launch { sut.refreshWhileVisible() }
        testScope.runCurrent()

        coVerify(exactly = 1) { couponRepository.refresh() }
        assertEquals(CouponState(coupons.value, phoneCount = 1), sut.stateFlow.value)
    }

    @Test
    fun `보이는 동안 5초마다 다시 받아 폰에서 사용 처리한 쿠폰을 반영한다`() {
        val sut = CouponViewModel(couponRepository, DefaultPairingRepository(server.api, testScope.backgroundScope))
        testScope.backgroundScope.launch { sut.refreshWhileVisible() }
        testScope.runCurrent()

        coupons.value = coupons.value.map { it.copy(redeemed = true) } // 서버가 REDEEMED 로 내려줬다고 가정
        testScope.advanceTimeBy(10_100)
        testScope.runCurrent()

        coVerify(exactly = 3) { couponRepository.refresh() }
        assertTrue(sut.stateFlow.value.coupons.single().redeemed)
    }

    @Test
    fun `화면이 사라지면 다시 받지 않는다`() {
        val sut = CouponViewModel(couponRepository, DefaultPairingRepository(server.api, testScope.backgroundScope))
        val visible = testScope.backgroundScope.launch { sut.refreshWhileVisible() }
        testScope.runCurrent()

        visible.cancel()
        testScope.advanceTimeBy(20_000)
        testScope.runCurrent()

        coVerify(exactly = 1) { couponRepository.refresh() }
    }

    @Test
    fun `쿠폰을 못 받아와도 갖고 있던 목록을 보여주고 다음 주기에 다시 시도한다`() {
        coEvery { couponRepository.refresh() } throws IOException("offline")
        val sut = CouponViewModel(couponRepository, DefaultPairingRepository(server.api, testScope.backgroundScope))
        testScope.backgroundScope.launch { sut.refreshWhileVisible() }
        testScope.runCurrent()
        testScope.advanceTimeBy(5_100)
        testScope.runCurrent()

        assertEquals(coupons.value, sut.stateFlow.value.coupons)
        coVerify(exactly = 2) { couponRepository.refresh() }
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
        testScope.launch { pairing.refreshPhones() }
        testScope.runCurrent()

        testScope.launch { pairing.requestCode() }
        testScope.runCurrent()

        assertEquals(1, sut.stateFlow.value.phoneCount)
    }
}
