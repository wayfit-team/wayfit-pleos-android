package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.DefaultStoreRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.VoiceIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class VoiceRecommendationTest {

    private val store = DefaultStoreRepository.DEMO_STORES.first()
    private val enabled = MutableStateFlow(true)
    private val voice = mockk<VoiceRepository>(relaxed = true)
    private val settings = mockk<SettingsRepository> { every { voiceGuideEnabled } returns enabled }
    private val sut = VoiceRecommendation(voice, settings)

    @Test
    fun `추천을 읽고 듣고 응답을 분류한다`() = runTest {
        coEvery { voice.listen(any()) } returns "들를게요"
        val listening = mutableListOf<Boolean>()

        val intent = sut.ask(store, 0.8, stillAsking = { true }, onListening = listening::add)

        assertEquals(VoiceIntent.YES, intent)
        assertEquals(listOf(true, false), listening)
        coVerify { voice.speak("800미터 앞 광교 모퉁이커피, 아메리카노 1잔 무료예요. 들를까요?") }
    }

    @Test
    fun `음성 안내가 꺼져 있으면 읽지도 듣지도 않는다`() = runTest {
        enabled.value = false

        assertNull(sut.ask(store, 1.2, stillAsking = { true }, onListening = {}))
        coVerify(exactly = 0) { voice.speak(any()) }
        coVerify(exactly = 0) { voice.listen(any()) }
    }

    @Test
    fun `읽는 사이 추천이 끝났으면 듣지 않는다`() = runTest {
        assertNull(sut.ask(store, 1.2, stillAsking = { false }, onListening = {}))
        coVerify(exactly = 0) { voice.listen(any()) }
    }

    @Test
    fun `듣는 중에 취소돼도 화면의 듣는 중 표시는 꺼진다`() = runTest {
        val never = CompletableDeferred<String?>()
        coEvery { voice.listen(any()) } coAnswers { never.await() }
        val listening = mutableListOf<Boolean>()

        val job = launch { sut.ask(store, 1.2, stillAsking = { true }, onListening = listening::add) }
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(listOf(true, false), listening)
    }
}
