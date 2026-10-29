package com.wayfit.wayfitpleos.screen.pair

import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PairViewModelTest : JUnit5Test() {

    private val phone = PairedPhone("p1", "Galaxy S25", 0L)
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    private val code = MutableStateFlow<PairingCode?>(null)
    private val repository = mockk<PairingRepository>(relaxed = true) {
        every { phones } returns this@PairViewModelTest.phones
        every { code } returns this@PairViewModelTest.code
    }

    private fun create() = PairViewModel(repository).also { testScope.runCurrent() }

    @Test
    fun `코드가 없으면 들어오자마자 받는다`() {
        create()
        verify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `이미 연결된 폰이 있어도 다른 폰을 위해 새 코드를 받는다`() {
        phones.value = listOf(phone)
        create()
        verify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `유효한 코드가 떠 있으면 새로 받지 않는다`() {
        code.value = PairingCode("12345678", secondsLeft = 300)
        val sut = create()

        verify(exactly = 0) { repository.requestCode() }
        assertEquals(PairingCode("12345678", 300), sut.stateFlow.value.code)
    }

    @Test
    fun `만료된 코드면 새로 받는다`() {
        code.value = PairingCode("12345678", secondsLeft = 0)
        create()
        verify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `이 화면에서 새 폰이 연결되면 연결 완료로 바뀐다 (기존 폰은 세지 않음)`() {
        phones.value = listOf(phone)
        val sut = create()
        assertFalse(sut.stateFlow.value.justPaired)

        phones.value = listOf(phone, phone.copy(id = "p2", name = "iPhone 17"))
        testScope.runCurrent()

        assertTrue(sut.stateFlow.value.justPaired)
        assertEquals(2, sut.stateFlow.value.phoneCount)
    }
}
