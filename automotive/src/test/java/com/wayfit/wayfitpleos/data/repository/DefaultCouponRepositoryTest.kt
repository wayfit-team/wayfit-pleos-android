package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.dto.CouponDto
import com.wayfit.wayfitpleos.data.remote.dto.CouponListResponse
import com.wayfit.wayfitpleos.model.Coupon
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class DefaultCouponRepositoryTest {

    private val api = mockk<WayfitApi>()
    private val sut = DefaultCouponRepository(api)

    private fun dto(id: String, status: String = "ISSUED") = CouponDto(
        id = id, storeId = "store_01", storeNameSnapshot = "호수길 로스터리", benefitSnapshot = "아메리카노 1잔",
        code6 = "482910", status = status,
        issuedAt = Instant.now().toString(),
        expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).minusSeconds(60).toString(),
    )

    @Test
    fun `서버 쿠폰 목록을 받아 화면 모델로 바꾼다`() = runTest {
        coEvery { api.coupons() } returns CouponListResponse(listOf(dto("c1"), dto("c2", status = "REDEEMED")))

        sut.refresh()

        val (first, second) = sut.coupons.value
        assertEquals(Coupon("c1", "호수길 로스터리", "아메리카노 1잔", "482910", expiresInDays = 7), first)
        assertEquals(true, second.redeemed)
    }

    @Test
    fun `인증으로 받은 쿠폰은 맨 앞에 넣고 같은 ID 면 바꿔 끼운다`() = runTest {
        coEvery { api.coupons() } returns CouponListResponse(listOf(dto("c1"), dto("c2")))
        sut.refresh()
        val issued = Coupon("c2", "새 매장", "새 혜택", "111111", expiresInDays = 7)

        sut.add(issued)

        assertEquals(listOf("c2", "c1"), sut.coupons.value.map { it.id })
        assertEquals(issued, sut.coupons.value.first())
    }
}
