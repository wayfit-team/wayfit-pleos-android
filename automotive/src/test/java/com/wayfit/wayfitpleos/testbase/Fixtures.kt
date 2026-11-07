package com.wayfit.wayfitpleos.testbase

import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.dto.CreatePairingCodeResponse
import com.wayfit.wayfitpleos.data.remote.dto.DevicePairingStatusResponse
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.StoreCategory
import io.mockk.coEvery
import io.mockk.mockk

internal object Fixtures {

    /** 테스트용 매장 (서버 데모 매장과 같은 아주대 주변) */
    val STORE = Store(
        "store_01", "광교 모퉁이커피", StoreCategory.CAFE, "아메리카노 1잔 무료",
        location = GeoPoint(37.2842, 127.0530), aheadKm = 0.8, detourMin = 3,
    )

    const val PAIRING_CODE = "48201937"
    const val PAIRING_URL = "https://wayfit-pwa.pages.dev/pair?code=$PAIRING_CODE"

    /** 페어링 API 만 흉내 내는 서버. [linked] 를 바꾸면 연결된 폰 수가 바뀐다 */
    class PairingServer {
        var linked = 0
        val api: WayfitApi = mockk {
            coEvery { pairingStatus() } answers { DevicePairingStatusResponse("dev_test", linked) }
            coEvery { createPairingCode() } returns
                CreatePairingCodeResponse(PAIRING_CODE, "2026-11-05T03:20:00Z", PAIRING_URL)
        }
    }
}
