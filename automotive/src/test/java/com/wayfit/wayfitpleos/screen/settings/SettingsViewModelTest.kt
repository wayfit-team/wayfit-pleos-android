package com.wayfit.wayfitpleos.screen.settings

import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsViewModelTest : JUnit5Test() {

    private val galaxy = PairedPhone("p1", "Galaxy S25", 0L)
    private val iphone = PairedPhone("p2", "iPhone 17", 0L)

    private val pairing = MutableStateFlow<Pairing>(Pairing.Paired(listOf(galaxy, iphone)))
    private val voiceGuide = MutableStateFlow(true)

    private lateinit var pairingRepository: PairingRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sut: SettingsViewModel

    @BeforeEach
    override fun setup() {
        super.setup()
        pairingRepository = mockk(relaxed = true) { every { pairing } returns this@SettingsViewModelTest.pairing }
        settingsRepository = mockk(relaxed = true) { every { voiceGuideEnabled } returns voiceGuide }
        sut = SettingsViewModel(pairingRepository, settingsRepository)
        sut.fetchData()
        testScope.runCurrent()
    }

    @Test
    fun `연결된 폰 목록과 음성 안내 설정을 보여준다`() {
        assertEquals(SettingsState(phones = listOf(galaxy, iphone), voiceGuideEnabled = true), sut.stateFlow.value)
    }

    @Test
    fun `새 코드를 띄우는 동안에도 기존 폰 목록은 유지한다`() {
        pairing.value = Pairing.Showing("12345678", 600)
        testScope.runCurrent()

        assertEquals(listOf(galaxy, iphone), sut.stateFlow.value.phones)
    }

    @Test
    fun `미연결이면 빈 목록`() {
        pairing.value = Pairing.NotPaired
        testScope.runCurrent()

        assertEquals(emptyList<PairedPhone>(), sut.stateFlow.value.phones)
    }

    @Test
    fun `폰 한 대 연결 해제 후 안내 메시지`() {
        val events = mutableListOf<SettingsEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(events::add) }
        testScope.runCurrent()

        sut.disconnect(galaxy)
        testScope.runCurrent()

        verify { pairingRepository.disconnect("p1") }
        assertEquals(listOf(SettingsEvent.ShowMessage("Galaxy S25 연결을 해제했어요")), events)
    }

    @Test
    fun `모두 해제`() {
        sut.disconnectAll()
        verify { pairingRepository.disconnectAll() }
    }

    @Test
    fun `음성 안내 끄기`() {
        sut.setVoiceGuideEnabled(false)
        verify { settingsRepository.setVoiceGuideEnabled(false) }

        voiceGuide.value = false
        testScope.runCurrent()
        assertFalse(sut.stateFlow.value.voiceGuideEnabled)
    }
}
