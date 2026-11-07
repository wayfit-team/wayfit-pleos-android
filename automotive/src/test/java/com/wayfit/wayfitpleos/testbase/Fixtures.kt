package com.wayfit.wayfitpleos.testbase

import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.dto.CreatePairingCodeResponse
import com.wayfit.wayfitpleos.data.remote.dto.DevicePhonesResponse
import com.wayfit.wayfitpleos.data.remote.dto.LinkedPhoneDto
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.StoreCategory
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

internal object Fixtures {

    /** 테스트용 매장 (서버 데모 매장과 같은 아주대 주변) */
    val STORE = Store(
        "store_01", "광교 모퉁이커피", StoreCategory.CAFE, "아메리카노 1잔 무료",
        location = GeoPoint(37.2842, 127.0530), aheadKm = 0.8, detourMin = 3,
    )

    const val PAIRING_CODE = "48201937"
    const val PAIRING_URL = "https://wayfit-pwa.pages.dev/pair?code=$PAIRING_CODE"

    /** 페어링 API 만 흉내 내는 서버. [linked] 를 바꾸면 연결된 폰이 그 수만큼 생긴다 */
    class PairingServer {
        val phones = mutableListOf<LinkedPhoneDto>()
        private var nextId = 1

        var linked: Int
            get() = phones.size
            set(value) {
                while (phones.size < value) phones += LinkedPhoneDto("phn_${nextId++}", "2026-11-07T10:00:00Z")
                while (phones.size > value) phones.removeAt(phones.lastIndex)
            }

        val api: WayfitApi = mockk {
            coEvery { phones() } answers { DevicePhonesResponse(phones.toList()) }
            coEvery { unlinkPhone(any()) } answers {
                if (!phones.removeIf { it.phoneId == firstArg<String>() }) throw notFound()
            }
            coEvery { unlinkAllPhones() } answers { phones.clear() }
            coEvery { createPairingCode() } returns
                CreatePairingCodeResponse(PAIRING_CODE, "2026-11-05T03:20:00Z", PAIRING_URL)
        }

        private fun notFound() = HttpException(
            Response.error<Any>(
                404,
                """{"error":{"code":"NOT_FOUND","message":"없음","details":{}}}""".toResponseBody("application/json".toMediaType()),
            )
        )
    }
}
