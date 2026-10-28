package com.wayfit.wayfitpleos.screen.coupon

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Pairing
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

    @Test
    fun `쿠폰과 폰 연결 상태가 바뀌면 화면 상태도 바뀐다`() {
        val coupons = MutableStateFlow(listOf(Coupon("a", "가게A", "혜택", "111111", 7)))
        val pairing = MutableStateFlow<Pairing>(Pairing.NotPaired)
        val sut = CouponViewModel(
            mockk<CouponRepository> { every { this@mockk.coupons } returns coupons },
            mockk<PairingRepository> { every { this@mockk.pairing } returns pairing },
        )
        sut.fetchData()
        testScope.runCurrent()
        assertEquals(CouponState(coupons.value, Pairing.NotPaired), sut.stateFlow.value)

        val newCoupon = Coupon("b", "가게B", "혜택2", "222222", 7)
        coupons.value = listOf(newCoupon) + coupons.value
        pairing.value = Pairing.Paired(emptyList())
        testScope.runCurrent()

        assertEquals(2, sut.stateFlow.value.coupons.size)
        assertEquals(newCoupon, sut.stateFlow.value.coupons.first())
        assertEquals(Pairing.Paired(emptyList()), sut.stateFlow.value.pairing)
    }
}
