package com.wayfit.wayfitpleos.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** PoC: 메모리 저장. 앱을 다시 켜면 기본값(켜짐)으로 돌아간다 — 추후 DataStore 로 교체 */
@Singleton
class DefaultSettingsRepository @Inject constructor() : SettingsRepository {

    private val _voiceGuideEnabled = MutableStateFlow(true)
    override val voiceGuideEnabled: StateFlow<Boolean> = _voiceGuideEnabled.asStateFlow()

    override fun setVoiceGuideEnabled(enabled: Boolean) {
        _voiceGuideEnabled.value = enabled
    }
}
