package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.PairingCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPairingRepositoryTest {

    private val scope = TestScope(StandardTestDispatcher())
    private val sut = DefaultPairingRepository(scope.backgroundScope)

    @Test
    fun `폰 한 대를 해제하면 나머지는 연결 상태로 남는다`() {
        sut.demoPhonePaired()
        sut.demoPhonePaired()
        val first = sut.phones.value.first()

        sut.disconnect(first.id)

        assertEquals(1, sut.phones.value.size)
        assertTrue(sut.phones.value.none { it.id == first.id })
    }

    @Test
    fun `모두 해제`() {
        repeat(3) { sut.demoPhonePaired() }
        sut.disconnectAll()
        assertEquals(emptyList<Any>(), sut.phones.value)
    }

    @Test
    fun `코드를 발급해도 연결된 폰 목록은 그대로다 (리뷰 1-2)`() {
        sut.demoPhonePaired()

        sut.requestCode()
        scope.runCurrent()

        assertEquals(1, sut.phones.value.size)
        val code = sut.code.value!!
        assertEquals(8, code.code.length)
        assertEquals(PairingCode.TTL_SEC, code.secondsLeft)
    }

    @Test
    fun `코드는 1초씩 줄고 10분 뒤 만료된다`() {
        sut.requestCode()
        scope.runCurrent()
        scope.advanceTimeBy(PairingCode.TTL_SEC * 1_000L)
        scope.runCurrent()

        assertTrue(sut.code.value!!.expired)
    }

    @Test
    fun `폰이 연결되면 코드는 끝난다 (1회용)`() {
        sut.requestCode()
        scope.runCurrent()

        sut.demoPhonePaired()

        assertNull(sut.code.value)
        assertEquals(1, sut.phones.value.size)
    }
}
