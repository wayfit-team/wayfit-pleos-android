package com.wayfit.wayfitpleos.data.repository

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DefaultSettingsRepositoryTest {

    @Test
    fun `음성 안내는 기본으로 켜져 있고 끌 수 있다`() {
        val sut = DefaultSettingsRepository()
        assertTrue(sut.voiceGuideEnabled.value)

        sut.setVoiceGuideEnabled(false)

        assertFalse(sut.voiceGuideEnabled.value)
    }
}
