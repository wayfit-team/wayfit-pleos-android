package com.wayfit.wayfitpleos.screen.pair

import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import com.wayfit.wayfitpleos.testbase.Fixtures
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class PairViewModelTest : JUnit5Test() {

    private val phone = PairedPhone("phone_1", "휴대폰 1")
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    private val code = MutableStateFlow<PairingCode?>(null)
    private val repository = mockk<PairingRepository>(relaxed = true) {
        every { phones } returns this@PairViewModelTest.phones
        every { code } returns this@PairViewModelTest.code
    }

    private fun showing(secondsLeft: Int) = PairingCode(Fixtures.PAIRING_CODE, secondsLeft, Fixtures.PAIRING_URL)

    private fun create() = PairViewModel(repository).also { testScope.runCurrent() }

    @Test
    fun `코드가 없으면 들어오자마자 받는다`() {
        create()
        coVerify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `이미 연결된 폰이 있어도 다른 폰을 위해 새 코드를 받는다`() {
        phones.value = listOf(phone)
        create()
        coVerify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `유효한 코드가 떠 있으면 새로 받지 않는다`() {
        code.value = showing(300)
        val sut = create()

        coVerify(exactly = 0) { repository.requestCode() }
        assertEquals(showing(300), sut.stateFlow.value.code)
    }

    @Test
    fun `만료된 코드면 새로 받는다`() {
        code.value = showing(0)
        create()
        coVerify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `코드 발급에 실패하면 안내하고 다시 시도할 수 있다`() {
        coEvery { repository.requestCode() } throws IOException("offline") andThen Unit
        val sut = create()

        assertEquals("연결 코드를 받지 못했어요. 네트워크를 확인해 주세요", sut.stateFlow.value.error)
        assertFalse(sut.stateFlow.value.loading)

        sut.requestNewCode()
        testScope.runCurrent()

        assertNull(sut.stateFlow.value.error)
        coVerify(exactly = 2) { repository.requestCode() }
    }

    @Test
    fun `코드를 띄운 동안 3초마다 폰 연결 여부를 서버에 묻는다`() {
        code.value = showing(300)
        create()

        testScope.advanceTimeBy(9_100)
        testScope.runCurrent()

        coVerify(exactly = 3) { repository.refreshPhones() }
    }

    @Test
    fun `코드가 만료되면 묻지 않는다`() {
        code.value = showing(300)
        create()
        code.value = showing(0)
        testScope.runCurrent()

        testScope.advanceTimeBy(9_100)
        testScope.runCurrent()

        coVerify(exactly = 0) { repository.refreshPhones() }
    }

    @Test
    fun `이 화면에서 새 폰이 연결되면 연결 완료로 바뀐다 (기존 폰은 세지 않음)`() {
        phones.value = listOf(phone)
        val sut = create()
        assertFalse(sut.stateFlow.value.justPaired)

        phones.value = listOf(phone, phone.copy(id = "phone_2", name = "휴대폰 2"))
        testScope.runCurrent()

        assertTrue(sut.stateFlow.value.justPaired)
        assertEquals(2, sut.stateFlow.value.phoneCount)
    }
}
