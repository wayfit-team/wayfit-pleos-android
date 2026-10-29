package com.wayfit.wayfitpleos.model

/** 추천 음성 응답 분류 (Spec REQ-A21, EDGE-A12) */
enum class VoiceIntent {
    YES, NO, UNKNOWN;

    companion object {
        private val YES_WORDS = listOf("응", "네", "예", "어", "그래", "좋아", "가자", "들를게", "들러", "콜", "오케이", "ok")
        private val NO_WORDS = listOf("아니", "괜찮아", "괜찮습니다", "다음에", "됐어", "싫어", "패스", "안 가", "안가")

        fun classify(text: String?): VoiceIntent {
            val normalized = text?.trim()?.lowercase()?.replace(Regex("[.,!?~]"), "") ?: return UNKNOWN
            if (normalized.isEmpty()) return UNKNOWN

            // 부정어는 부분 일치, 긍정어는 어절 단위로 본다 ("괜찮아"의 "아", "어때"의 "어" 오인식 방지)
            val no = NO_WORDS.any { normalized.contains(it) }
            val tokens = normalized.split(Regex("\\s+"))
            val yes = tokens.any { token -> YES_WORDS.any { token == it || token.startsWith(it) && it.length >= 2 } }

            return when {
                yes && no -> UNKNOWN
                no -> NO
                yes -> YES
                else -> UNKNOWN
            }
        }
    }
}
