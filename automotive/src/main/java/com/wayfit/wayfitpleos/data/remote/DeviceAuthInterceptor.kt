package com.wayfit.wayfitpleos.data.remote

import com.wayfit.wayfitpleos.data.remote.dto.RegisterDeviceResponse
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 요청에 `Authorization: Bearer <기기 토큰>` 을 붙인다.
 * 토큰이 없으면(첫 실행) 먼저 `POST /v1/devices` 로 기기를 등록하고,
 * 401 이 오면(서버 DB 초기화 등) 한 번만 다시 등록해 재시도한다.
 *
 * OkHttp 비동기 호출에서 IOException 이 아닌 예외는 앱을 죽이므로 실패는 모두 IOException 으로 던진다.
 */
@Singleton
class DeviceAuthInterceptor @Inject constructor(
    private val store: DeviceTokenStore,
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = token(chain, request)
        val response = chain.proceed(request.withToken(token))
        if (response.code != 401) return response

        response.close()
        synchronized(this) { if (store.token == token) store.clear() }
        return chain.proceed(request.withToken(token(chain, request)))
    }

    /** 여러 요청이 동시에 첫 등록을 하지 않도록 잠근다 */
    private fun token(chain: Interceptor.Chain, request: Request): String = synchronized(this) {
        store.token ?: register(chain, request)
    }

    private fun register(chain: Interceptor.Chain, original: Request): String {
        val url = original.url.newBuilder().encodedPath(REGISTER_PATH).query(null).build()
        val request = Request.Builder().url(url).post(ByteArray(0).toRequestBody()).build()
        chain.proceed(request).use { response ->
            if (!response.isSuccessful) throw IOException("기기 등록 실패 (HTTP ${response.code})")
            val body = try {
                json.decodeFromString<RegisterDeviceResponse>(response.body?.string().orEmpty())
            } catch (e: IllegalArgumentException) {
                throw IOException("기기 등록 응답을 읽지 못했어요", e)
            }
            store.save(body.deviceId, body.deviceToken)
            return body.deviceToken
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        const val REGISTER_PATH = "/v1/devices"
    }
}
