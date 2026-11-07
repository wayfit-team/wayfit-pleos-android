package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.remote.ApiException
import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.dto.CouponDto
import com.wayfit.wayfitpleos.data.remote.dto.EvidenceDto
import com.wayfit.wayfitpleos.data.remote.dto.VerifyVisitRequest
import com.wayfit.wayfitpleos.data.remote.dto.VerifyVisitResponse
import com.wayfit.wayfitpleos.data.remote.dto.VisitDto
import com.wayfit.wayfitpleos.data.remote.dto.VisitResponse
import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.EvidenceType
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.LocationSource
import com.wayfit.wayfitpleos.model.VerifyResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.HttpException
import retrofit2.Response

internal class DefaultVisitRepositoryTest {

    private val api = mockk<WayfitApi>()
    private val sut = DefaultVisitRepository(api)

    private val visit = VisitDto("visit_1", "store_01", "trip_1", "VERIFIED", "2026-11-05T03:00:00Z")
    private val coupon = CouponDto(
        "cpn_1", "store_01", "호수길 로스터리", "아메리카노 1잔", "482910", "ISSUED",
        "2026-11-05T03:13:10Z", "2099-01-01T00:00:00Z",
    )

    private fun httpError(status: Int, code: String, message: String) = HttpException(
        Response.error<Any>(
            status,
            """{"error":{"code":"$code","message":"$message","details":{}}}""".toResponseBody("application/json".toMediaType()),
        )
    )

    @Test
    fun `증거를 서버 형식(ISO 시각, 출처)으로 보내고 쿠폰을 받는다`() = runTest {
        coEvery { api.verifyVisit(any(), any()) } returns VerifyVisitResponse(visit, coupon)
        val evidence = Evidence(EvidenceType.GEOFENCE_ENTER, 1_762_312_360_000, GeoPoint(37.28, 127.05), LocationSource.FUSED)

        val result = sut.verify("visit_1", listOf(evidence, Evidence(EvidenceType.GEAR_PARK, 1_762_312_400_000)))

        coVerify {
            api.verifyVisit(
                "visit_1",
                VerifyVisitRequest(
                    listOf(
                        EvidenceDto("GEOFENCE_ENTER", "2025-11-05T03:12:40Z", 37.28, 127.05, "FUSED"),
                        EvidenceDto("GEAR_PARK", "2025-11-05T03:13:20Z"),
                    )
                ),
            )
        }
        assertEquals("482910", (result as VerifyResult.Verified).coupon.code)
    }

    @Test
    fun `인증 조건 미달(422)은 서버 안내 문구와 함께 거절 결과로 준다`() = runTest {
        coEvery { api.verifyVisit(any(), any()) } throws httpError(422, "VISIT_NO_PARKING", "주차가 확인되지 않았습니다")

        val result = sut.verify("visit_1", emptyList())

        assertEquals(VerifyResult.Rejected("VISIT_NO_PARKING", "주차가 확인되지 않았습니다"), result)
    }

    @Test
    fun `다른 오류(404 등)는 예외로 던진다`() = runTest {
        coEvery { api.verifyVisit(any(), any()) } throws httpError(404, "NOT_FOUND", "없는 방문")

        val e = assertThrows<ApiException> { sut.verify("visit_1", emptyList()) }

        assertEquals(404, e.httpStatus)
        assertEquals("NOT_FOUND", e.code)
    }

    @Test
    fun `방문을 만들면 방문 ID 를 돌려준다`() = runTest {
        coEvery { api.createVisit(any()) } returns VisitResponse(visit)

        assertEquals("visit_1", sut.create("store_01", "trip_1"))
    }
}
