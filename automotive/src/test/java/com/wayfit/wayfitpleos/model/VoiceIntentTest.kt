package com.wayfit.wayfitpleos.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VoiceIntentTest {

    @Test
    fun `긍정 응답은 YES`() {
        listOf("응", "네", "좋아", "들를게", "들를게요", "그래 가자", "네!").forEach {
            assertEquals(VoiceIntent.YES, VoiceIntent.classify(it), it)
        }
    }

    @Test
    fun `부정 응답은 NO`() {
        listOf("아니", "아니요", "괜찮아", "괜찮아요", "다음에", "다음에 갈게").forEach {
            assertEquals(VoiceIntent.NO, VoiceIntent.classify(it), it)
        }
    }

    @Test
    fun `애매하거나 빈 응답은 UNKNOWN`() {
        listOf(null, "", "   ", "응 아니 괜찮아", "오늘 날씨 어때").forEach {
            assertEquals(VoiceIntent.UNKNOWN, VoiceIntent.classify(it), it.toString())
        }
    }
}
