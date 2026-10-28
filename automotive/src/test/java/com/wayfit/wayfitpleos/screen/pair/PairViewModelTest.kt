package com.wayfit.wayfitpleos.screen.pair

import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PairViewModelTest : JUnit5Test() {

    private fun create(initial: Pairing): Pair<PairViewModel, PairingRepository> {
        val repository = mockk<PairingRepository>(relaxed = true) { every { pairing } returns MutableStateFlow(initial) }
        val sut = PairViewModel(repository)
        sut.fetchData()
        testScope.runCurrent()
        return sut to repository
    }

    @Test
    fun `미연결이면 들어오자마자 코드를 받는다`() {
        val (_, repository) = create(Pairing.NotPaired)
        verify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `이미 연결된 폰이 있어도 다른 폰을 위해 새 코드를 받는다`() {
        val (_, repository) = create(Pairing.Paired(emptyList()))
        verify(exactly = 1) { repository.requestCode() }
    }

    @Test
    fun `유효한 코드가 떠 있으면 새로 받지 않는다`() {
        val showing = Pairing.Showing("12345678", secondsLeft = 300)
        val (sut, repository) = create(showing)

        verify(exactly = 0) { repository.requestCode() }
        assertEquals(PairState(showing), sut.stateFlow.value)
    }

    @Test
    fun `만료된 코드면 새로 받는다`() {
        val (_, repository) = create(Pairing.Showing("12345678", secondsLeft = 0))
        verify(exactly = 1) { repository.requestCode() }
    }
}
