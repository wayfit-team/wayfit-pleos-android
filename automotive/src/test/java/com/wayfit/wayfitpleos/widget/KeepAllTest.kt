package com.wayfit.wayfitpleos.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class KeepAllTest {

    private fun String.stripJoiners() = replace("⁠", "")

    @Test
    fun `한글은 어절 안에서만 붙인다`() {
        assertEquals("아⁠메⁠리⁠카⁠노 1⁠잔", "아메리카노 1잔".keepAll())
    }

    @Test
    fun `이모지 서로게이트 쌍은 깨지지 않는다`() {
        listOf("🍜 원천동 손칼국수", "☕ 광교 모퉁이커피", "🥐빵", "🎙 듣고 있어요").forEach { text ->
            val result = text.keepAll()
            assertEquals(text, result.stripJoiners())
            result.forEachIndexed { i, c ->
                if (c.isHighSurrogate()) assertFalse(result[i + 1] == '⁠', text)
            }
        }
    }
}
