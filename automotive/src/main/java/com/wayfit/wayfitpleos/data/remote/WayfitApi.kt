package com.wayfit.wayfitpleos.data.remote

import com.wayfit.wayfitpleos.data.remote.dto.CouponListResponse
import com.wayfit.wayfitpleos.data.remote.dto.CreateImpressionRequest
import com.wayfit.wayfitpleos.data.remote.dto.CreateImpressionResponse
import com.wayfit.wayfitpleos.data.remote.dto.CreatePairingCodeResponse
import com.wayfit.wayfitpleos.data.remote.dto.CreateVisitRequest
import com.wayfit.wayfitpleos.data.remote.dto.DevicePhonesResponse
import com.wayfit.wayfitpleos.data.remote.dto.NearbyStoresResponse
import com.wayfit.wayfitpleos.data.remote.dto.VerifyVisitRequest
import com.wayfit.wayfitpleos.data.remote.dto.VerifyVisitResponse
import com.wayfit.wayfitpleos.data.remote.dto.VisitResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 웨이핏 백엔드의 차량 앱용 API (`/v1/docs` 의 Vehicle 태그).
 * 기기 토큰은 [DeviceAuthInterceptor] 가 붙이므로 여기서는 신경 쓰지 않는다.
 */
interface WayfitApi {

    @GET("v1/stores/nearby")
    suspend fun nearbyStores(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("destLat") destLat: Double? = null,
        @Query("destLng") destLng: Double? = null,
    ): NearbyStoresResponse

    @POST("v1/impressions")
    suspend fun createImpression(@Body body: CreateImpressionRequest): CreateImpressionResponse

    /** 진행 중인 다른 방문은 서버가 자동으로 취소한다 */
    @POST("v1/visits")
    suspend fun createVisit(@Body body: CreateVisitRequest): VisitResponse

    @DELETE("v1/visits/{id}")
    suspend fun cancelVisit(@Path("id") visitId: String): VisitResponse

    /** 인증 조건 미달이면 422 (VISIT_OUT_OF_RADIUS · VISIT_NO_PARKING · VISIT_TIMEOUT · DAILY_LIMIT) */
    @POST("v1/visits/{id}/verify")
    suspend fun verifyVisit(@Path("id") visitId: String, @Body body: VerifyVisitRequest): VerifyVisitResponse

    @GET("v1/devices/me/coupons")
    suspend fun coupons(): CouponListResponse

    @POST("v1/pairing-codes")
    suspend fun createPairingCode(): CreatePairingCodeResponse

    @GET("v1/devices/me/phones")
    suspend fun phones(): DevicePhonesResponse

    /** 이 차에 없는 연결이면 404 */
    @DELETE("v1/devices/me/phones/{id}")
    suspend fun unlinkPhone(@Path("id") phoneId: String)

    @DELETE("v1/devices/me/phones")
    suspend fun unlinkAllPhones()
}
