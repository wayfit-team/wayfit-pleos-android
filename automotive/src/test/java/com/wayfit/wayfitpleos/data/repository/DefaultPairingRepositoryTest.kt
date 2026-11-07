package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.PairingCode
import com.wayfit.wayfitpleos.testbase.Fixtures
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPairingRepositoryTest {

    private val scope = TestScope(StandardTestDispatcher())
    private val server = Fixtures.PairingServer()
    private val sut = DefaultPairingRepository(server.api, scope.backgroundScope)

    @Test
    fun `서버가 준 코드와 QR 주소를 10분 동안 띄운다`() = runTest {
        sut.requestCode()
        scope.runCurrent()

        val code = sut.code.value!!
        assertEquals(Fixtures.PAIRING_CODE, code.code)
        assertEquals(Fixtures.PAIRING_URL, code.url)
        assertEquals(PairingCode.TTL_SEC, code.secondsLeft)
    }

    @Test
    fun `코드는 1초씩 줄고 10분 뒤 만료된다`() = runTest {
        sut.requestCode()
        scope.runCurrent()
        scope.advanceTimeBy(PairingCode.TTL_SEC * 1_000L)
        scope.runCurrent()

        assertTrue(sut.code.value!!.expired)
    }

    @Test
    fun `연결된 폰 수만큼 목록을 만든다`() = runTest {
        server.linked = 2
        sut.refreshPhones()

        assertEquals(listOf("휴대폰 1", "휴대폰 2"), sut.phones.value.map { it.name })
    }

    @Test
    fun `코드를 발급해도 연결된 폰 목록은 그대로다 (리뷰 1-2)`() = runTest {
        server.linked = 1
        sut.refreshPhones()

        sut.requestCode()
        scope.runCurrent()

        assertEquals(1, sut.phones.value.size)
    }

    @Test
    fun `코드를 띄운 사이 폰이 늘면 그 코드는 끝난다 (1회용)`() = runTest {
        sut.requestCode()
        scope.runCurrent()

        server.linked = 1
        sut.refreshPhones()

        assertNull(sut.code.value)
        assertEquals(1, sut.phones.value.size)
    }

    @Test
    fun `폰 수가 그대로면 코드는 유지된다`() = runTest {
        sut.requestCode()
        scope.runCurrent()

        sut.refreshPhones()

        assertNotNull(sut.code.value)
    }

    @Test
    fun `서버의 연결 시각을 그대로 보여준다`() = runTest {
        server.linked = 1
        sut.refreshPhones()

        assertEquals(java.time.Instant.parse("2026-11-07T10:00:00Z").toEpochMilli(), sut.phones.value.single().connectedAtMillis)
    }

    @Test
    fun `한 대를 해제하면 서버에서 지우고 나머지는 남는다`() = runTest {
        server.linked = 2
        sut.refreshPhones()
        val first = sut.phones.value.first()

        sut.disconnect(first.id)

        coVerify { server.api.unlinkPhone(first.id) }
        assertEquals(listOf("phn_2"), server.phones.map { it.phoneId })
        assertEquals(listOf("휴대폰 1"), sut.phones.value.map { it.name })
        assertTrue(sut.phones.value.none { it.id == first.id })
    }

    @Test
    fun `폰에서 먼저 해제해 서버에 없으면(404) 목록에서만 지운다`() = runTest {
        server.linked = 1
        sut.refreshPhones()
        server.linked = 0 // 폰(PWA)에서 해제됨

        sut.disconnect(sut.phones.value.single().id)

        assertEquals(emptyList<Any>(), sut.phones.value)
    }

    @Test
    fun `모두 해제`() = runTest {
        server.linked = 3
        sut.refreshPhones()
        sut.demoPhonePaired()

        sut.disconnectAll()

        coVerify { server.api.unlinkAllPhones() }
        assertEquals(emptyList<Any>(), sut.phones.value)
    }

    @Test
    fun `DEMO 폰은 서버를 부르지 않고 해제한다`() = runTest {
        sut.demoPhonePaired()

        sut.disconnect(sut.phones.value.single().id)

        coVerify(exactly = 0) { server.api.unlinkPhone(any()) }
        assertEquals(emptyList<Any>(), sut.phones.value)
    }

    @Test
    fun `DEMO 폰은 서버 폰 수에 더해진다`() = runTest {
        server.linked = 1
        sut.refreshPhones()
        sut.demoPhonePaired()
        sut.refreshPhones()

        assertEquals(2, sut.phones.value.size)
    }
}
