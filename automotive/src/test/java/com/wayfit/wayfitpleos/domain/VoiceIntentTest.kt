package com.wayfit.wayfitpleos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIntentTest {

    @Test
    fun yes() {
        listOf("응", "네", "좋아", "들를게", "들를게요", "그래 가자", "네!").forEach {
            assertEquals(it, VoiceIntent.YES, classifyVoice(it))
        }
    }

    @Test
    fun no() {
        listOf("아니", "아니요", "괜찮아", "괜찮아요", "다음에", "다음에 갈게").forEach {
            assertEquals(it, VoiceIntent.NO, classifyVoice(it))
        }
    }

    @Test
    fun ambiguousOrEmptyIsUnknown() {
        listOf(null, "", "   ", "응 아니 괜찮아", "오늘 날씨 어때").forEach {
            assertEquals(it.toString(), VoiceIntent.UNKNOWN, classifyVoice(it))
        }
    }
}
