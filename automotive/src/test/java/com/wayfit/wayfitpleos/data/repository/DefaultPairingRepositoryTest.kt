package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Pairing
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DefaultPairingRepositoryTest {

    private val sut = DefaultPairingRepository(TestScope())

    @Test
    fun `폰 한 대를 해제하면 나머지는 연결 상태로 남는다`() {
        sut.demoPhonePaired()
        sut.demoPhonePaired()
        val first = (sut.pairing.value as Pairing.Paired).phones.first()

        sut.disconnect(first.id)

        val paired = sut.pairing.value as Pairing.Paired
        assertEquals(1, paired.phoneCount)
        assertTrue(paired.phones.none { it.id == first.id })
    }

    @Test
    fun `마지막 폰을 해제하면 미연결이 된다`() {
        sut.demoPhonePaired()
        val phone = (sut.pairing.value as Pairing.Paired).phones.single()

        sut.disconnect(phone.id)

        assertEquals(Pairing.NotPaired, sut.pairing.value)
    }

    @Test
    fun `모두 해제`() {
        repeat(3) { sut.demoPhonePaired() }

        sut.disconnectAll()

        assertEquals(Pairing.NotPaired, sut.pairing.value)
    }
}
