package com.wayfit.wayfitpleos.domain

/** 추천 음성 응답 분류 (Spec REQ-A21, EDGE-A12) */
enum class VoiceIntent { YES, NO, UNKNOWN }

private val YES_WORDS = listOf("응", "네", "예", "어", "그래", "좋아", "가자", "들를게", "들러", "콜", "오케이", "ok")
private val NO_WORDS = listOf("아니", "괜찮아", "괜찮습니다", "다음에", "됐어", "싫어", "패스", "안 가", "안가")

fun classifyVoice(text: String?): VoiceIntent {
    val normalized = text?.trim()?.lowercase()?.replace(Regex("[.,!?~]"), "") ?: return VoiceIntent.UNKNOWN
    if (normalized.isEmpty()) return VoiceIntent.UNKNOWN

    // "괜찮아"에 "아"가, "아니"에 "어"가 섞이지 않도록 부정어를 먼저 보고, 긍정어는 어절 단위로 본다
    val no = NO_WORDS.any { normalized.contains(it) }
    val tokens = normalized.split(Regex("\\s+"))
    val yes = tokens.any { token -> YES_WORDS.any { token == it || token.startsWith(it) && it.length >= 2 } }

    return when {
        yes && no -> VoiceIntent.UNKNOWN
        no -> VoiceIntent.NO
        yes -> VoiceIntent.YES
        else -> VoiceIntent.UNKNOWN
    }
}
