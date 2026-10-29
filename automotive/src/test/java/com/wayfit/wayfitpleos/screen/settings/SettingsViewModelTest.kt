package com.wayfit.wayfitpleos.screen.settings

import com.wayfit.wayfitpleos.data.repository.DefaultPairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
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

    private val voiceGuide = MutableStateFlow(true)

    /** 실제 구현을 써서 "코드 발급 중에도 목록이 보이는지"를 그대로 확인 */
    private lateinit var pairingRepository: DefaultPairingRepository
    private lateinit var settingsRepository: SettingsRepository

    @BeforeEach
    override fun setup() {
        super.setup()
        pairingRepository = DefaultPairingRepository(testScope.backgroundScope)
        settingsRepository = mockk(relaxed = true) { every { voiceGuideEnabled } returns voiceGuide }
    }

    private fun create() = SettingsViewModel(pairingRepository, settingsRepository).also { testScope.runCurrent() }

    @Test
    fun `연결된 폰 목록과 음성 안내 설정을 보여준다`() {
        pairingRepository.demoPhonePaired()
        pairingRepository.demoPhonePaired()
        val sut = create()

        assertEquals(listOf("Galaxy S25", "iPhone 17"), sut.stateFlow.value.phones.map { it.name })
        assertEquals(true, sut.stateFlow.value.voiceGuideEnabled)
    }

    @Test
    fun `연결 코드를 띄우는 중에 설정 화면을 처음 열어도 폰 목록이 보인다 (리뷰 1-2)`() {
        pairingRepository.demoPhonePaired()
        pairingRepository.requestCode() // 다른 폰 연결 화면에 들어갔다가 그냥 나온 상황
        testScope.runCurrent()

        val sut = create()

        assertEquals(1, sut.stateFlow.value.phones.size)
    }

    @Test
    fun `폰 한 대 연결 해제 후 안내 메시지`() {
        pairingRepository.demoPhonePaired()
        val sut = create()
        val events = mutableListOf<SettingsEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(events::add) }
        testScope.runCurrent()

        sut.disconnect(sut.stateFlow.value.phones.single())
        testScope.runCurrent()

        assertEquals(emptyList<Any>(), sut.stateFlow.value.phones)
        assertEquals(listOf(SettingsEvent.ShowMessage("Galaxy S25 연결을 해제했어요")), events)
    }

    @Test
    fun `음성 안내 끄기`() {
        val sut = create()
        sut.setVoiceGuideEnabled(false)
        verify { settingsRepository.setVoiceGuideEnabled(false) }

        voiceGuide.value = false
        testScope.runCurrent()
        assertFalse(sut.stateFlow.value.voiceGuideEnabled)
    }
}
