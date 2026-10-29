package com.wayfit.wayfitpleos.screen.coupon

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DefaultPairingRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CouponViewModelTest : JUnit5Test() {

    private val coupons = MutableStateFlow(listOf(Coupon("a", "가게A", "혜택", "111111", 7)))
    private val couponRepository = mockk<CouponRepository> { every { coupons } returns this@CouponViewModelTest.coupons }

    @Test
    fun `쿠폰과 연결된 폰 수가 바뀌면 화면 상태도 바뀐다`() {
        val pairing = DefaultPairingRepository(testScope.backgroundScope)
        val sut = CouponViewModel(couponRepository, pairing)
        testScope.runCurrent()
        assertEquals(CouponState(coupons.value, phoneCount = 0), sut.stateFlow.value)

        val newCoupon = Coupon("b", "가게B", "혜택2", "222222", 7)
        coupons.value = listOf(newCoupon) + coupons.value
        pairing.demoPhonePaired()
        testScope.runCurrent()

        assertEquals(newCoupon, sut.stateFlow.value.coupons.first())
        assertEquals(1, sut.stateFlow.value.phoneCount)
    }

    @Test
    fun `다른 폰 연결 코드를 띄웠다가 그냥 나와도 연결된 폰 수는 그대로 (리뷰 1-2)`() {
        val pairing = DefaultPairingRepository(testScope.backgroundScope)
        pairing.demoPhonePaired()
        val sut = CouponViewModel(couponRepository, pairing)

        pairing.requestCode()
        testScope.runCurrent()

        assertEquals(1, sut.stateFlow.value.phoneCount)
    }
}
