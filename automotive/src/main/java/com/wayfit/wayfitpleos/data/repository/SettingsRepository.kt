package com.wayfit.wayfitpleos.data.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {

    /** 추천·쿠폰 도착 음성 안내와 음성 응답 (Spec REQ-A23) */
    val voiceGuideEnabled: StateFlow<Boolean>

    fun setVoiceGuideEnabled(enabled: Boolean)
}
