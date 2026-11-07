package com.wayfit.wayfitpleos.data.remote

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

internal class DeviceAuthInterceptorTest {

    private class MemoryTokenStore(override var token: String? = null) : DeviceTokenStore {
        override fun save(deviceId: String, token: String) {
            this.token = token
        }

        override fun clear() {
            token = null
        }
    }

    private val server = MockWebServer()
    private val store = MemoryTokenStore()
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setup() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(DeviceAuthInterceptor(store, Json { ignoreUnknownKeys = true }))
            .build()
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun registered(token: String) =
        MockResponse().setResponseCode(201).setBody("""{"deviceId":"dev_1","deviceToken":"$token"}""")

    private fun get(path: String = "/v1/devices/me/coupons") =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().use { it.code }

    @Test
    fun `토큰이 없으면 기기를 먼저 등록하고 그 토큰을 붙인다`() {
        server.enqueue(registered("wfd_first"))
        server.enqueue(MockResponse().setBody("""{"coupons":[]}"""))

        assertEquals(200, get())

        val register = server.takeRequest()
        assertEquals("POST", register.method)
        assertEquals("/v1/devices", register.path)
        assertEquals("Bearer wfd_first", server.takeRequest().getHeader("Authorization"))
        assertEquals("wfd_first", store.token)
    }

    @Test
    fun `저장된 토큰이 있으면 다시 등록하지 않는다`() {
        store.token = "wfd_saved"
        server.enqueue(MockResponse().setBody("{}"))

        get()

        assertEquals(1, server.requestCount)
        assertEquals("Bearer wfd_saved", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `401 이면 한 번만 다시 등록해 재시도한다`() {
        store.token = "wfd_stale"
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(registered("wfd_new"))
        server.enqueue(MockResponse().setBody("{}"))

        assertEquals(200, get())

        assertEquals("Bearer wfd_stale", server.takeRequest().getHeader("Authorization"))
        assertEquals("/v1/devices", server.takeRequest().path)
        assertEquals("Bearer wfd_new", server.takeRequest().getHeader("Authorization"))
        assertEquals("wfd_new", store.token)
    }

    @Test
    fun `등록에 실패하면 IOException 으로 알린다 (OkHttp 비동기 호출에서 앱이 죽지 않게)`() {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThrows<IOException> { get() }
        assertNull(store.token)
    }
}
