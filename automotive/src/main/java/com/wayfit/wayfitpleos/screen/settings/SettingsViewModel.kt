package com.wayfit.wayfitpleos.screen.settings

import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<SettingsState, SettingsEvent>() {

    override fun getInitialState(): SettingsState = SettingsState()

    init {
        // 연결된 폰 목록은 코드 발급 세션과 분리된 스트림이라 항상 값이 있다
        viewModelScope.launch {
            combine(pairingRepository.phones, settingsRepository.voiceGuideEnabled, ::SettingsState).collect(::setState)
        }
        viewModelScope.launch {
            try {
                pairingRepository.refreshPhones()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                sendEvent(SettingsEvent.ShowMessage("연결된 폰을 불러오지 못했어요"))
            }
        }
    }

    fun setVoiceGuideEnabled(enabled: Boolean) = settingsRepository.setVoiceGuideEnabled(enabled)
}
