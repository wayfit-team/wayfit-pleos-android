package com.wayfit.wayfitpleos.screen.settings

import com.wayfit.wayfitpleos.data.repository.DefaultPairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.testbase.Fixtures
import com.wayfit.wayfitpleos.testbase.JUnit5Test
import io.mockk.coEvery
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsViewModelTest : JUnit5Test() {

    private val voiceGuide = MutableStateFlow(true)
    private val server = Fixtures.PairingServer()

    /** 실제 구현을 써서 "코드 발급 중에도 목록이 보이는지"를 그대로 확인 */
    private lateinit var pairingRepository: DefaultPairingRepository
    private lateinit var settingsRepository: SettingsRepository

    @BeforeEach
    override fun setup() {
        super.setup()
        pairingRepository = DefaultPairingRepository(server.api, testScope.backgroundScope)
        settingsRepository = mockk(relaxed = true) { every { voiceGuideEnabled } returns voiceGuide }
    }

    private fun create() = SettingsViewModel(pairingRepository, settingsRepository).also { testScope.runCurrent() }

    @Test
    fun `들어오면 서버의 연결된 폰 목록과 음성 안내 설정을 보여준다`() {
        server.linked = 2
        val sut = create()

        assertEquals(listOf("휴대폰 1", "휴대폰 2"), sut.stateFlow.value.phones.map { it.name })
        assertEquals(true, sut.stateFlow.value.voiceGuideEnabled)
    }

    @Test
    fun `연결 코드를 띄우는 중에 설정 화면을 처음 열어도 폰 목록이 보인다 (리뷰 1-2)`() {
        server.linked = 1
        testScope.launch {
            pairingRepository.refreshPhones()
            pairingRepository.requestCode() // 다른 폰 연결 화면에 들어갔다가 그냥 나온 상황
        }
        testScope.runCurrent()

        val sut = create()

        assertEquals(1, sut.stateFlow.value.phones.size)
    }

    @Test
    fun `폰 목록을 못 불러오면 안내한다`() {
        coEvery { server.api.phones() } throws IOException("offline")
        val sut = SettingsViewModel(pairingRepository, settingsRepository)
        val events = mutableListOf<SettingsEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(events::add) }
        testScope.runCurrent()

        assertEquals(listOf(SettingsEvent.ShowMessage("연결된 폰을 불러오지 못했어요")), events)
    }

    private fun collectEvents(sut: SettingsViewModel): List<SettingsEvent> {
        val events = mutableListOf<SettingsEvent>()
        testScope.backgroundScope.launch { sut.eventFlow.collect(events::add) }
        testScope.runCurrent()
        return events
    }

    @Test
    fun `폰 한 대 연결 해제 후 안내 메시지`() {
        server.linked = 2
        val sut = create()
        val events = collectEvents(sut)

        sut.disconnect(sut.stateFlow.value.phones.first())
        testScope.runCurrent()

        assertEquals(listOf("휴대폰 1"), sut.stateFlow.value.phones.map { it.name })
        assertEquals(1, server.phones.size)
        assertEquals(listOf(SettingsEvent.ShowMessage("휴대폰 1 연결을 해제했어요")), events)
    }

    @Test
    fun `모두 해제 후 안내 메시지`() {
        server.linked = 2
        val sut = create()
        val events = collectEvents(sut)

        sut.disconnectAll()
        testScope.runCurrent()

        assertEquals(emptyList<Any>(), sut.stateFlow.value.phones)
        assertEquals(listOf(SettingsEvent.ShowMessage("모든 폰 연결을 해제했어요")), events)
    }

    @Test
    fun `해제 요청이 실패하면 목록은 그대로 두고 알린다`() {
        server.linked = 1
        coEvery { server.api.unlinkPhone(any()) } throws IOException("offline")
        val sut = create()
        val events = collectEvents(sut)

        sut.disconnect(sut.stateFlow.value.phones.single())
        testScope.runCurrent()

        assertEquals(1, sut.stateFlow.value.phones.size)
        assertEquals(listOf(SettingsEvent.ShowMessage("휴대폰 1 연결을 해제하지 못했어요")), events)
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
