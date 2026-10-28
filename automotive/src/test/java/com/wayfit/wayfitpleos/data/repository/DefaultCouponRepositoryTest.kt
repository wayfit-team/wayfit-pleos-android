package com.wayfit.wayfitpleos.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DefaultCouponRepositoryTest {

    private val sut = DefaultCouponRepository()
    private val store = DefaultStoreRepository.DEMO_STORES.first()

    @Test
    fun `발급한 쿠폰은 매장 정보를 담아 목록 맨 앞에 들어간다`() = runTest {
        val before = sut.coupons.value.size

        val coupon = sut.issue(store)

        assertEquals(before + 1, sut.coupons.value.size)
        assertEquals(coupon, sut.coupons.value.first())
        assertEquals(store.name, coupon.storeName)
        assertEquals(store.benefit, coupon.benefit)
    }

    @Test
    fun `쿠폰은 6자리 숫자 코드에 7일 유효, 미사용 상태로 발급된다`() = runTest {
        val coupon = sut.issue(store)

        assertTrue(coupon.code.matches(Regex("\\d{6}")), coupon.code)
        assertEquals(7, coupon.expiresInDays)
        assertFalse(coupon.redeemed)
    }
}
