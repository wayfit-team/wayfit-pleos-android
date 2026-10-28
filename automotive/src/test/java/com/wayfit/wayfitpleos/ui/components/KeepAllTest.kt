package com.wayfit.wayfitpleos.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeepAllTest {

    private fun String.stripJoiners() = replace("⁠", "")

    @Test
    fun joinsHangulWithinWordOnly() {
        assertEquals("아⁠메⁠리⁠카⁠노 1⁠잔", "아메리카노 1잔".keepAll())
    }

    @Test
    fun keepsEmojiSurrogatePairsIntact() {
        listOf("🍜 원천동 손칼국수", "☕ 광교 모퉁이커피", "🥐빵", "🎙 듣고 있어요").forEach { text ->
            val result = text.keepAll()
            assertEquals(text, result.stripJoiners())
            // 서로게이트 쌍 사이에 다른 문자가 끼면 안 된다
            result.forEachIndexed { i, c ->
                if (c.isHighSurrogate()) assertFalse(text, result[i + 1] == '⁠')
            }
        }
    }
}
