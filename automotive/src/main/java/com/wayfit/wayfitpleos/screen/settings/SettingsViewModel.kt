package com.wayfit.wayfitpleos.screen.settings

import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<SettingsState, SettingsEvent>() {

    override fun getInitialState(): SettingsState = SettingsState()

    override fun fetchData(): Job = viewModelScope.launch {
        // 코드 표시(Showing) 중에는 목록을 바꾸지 않는다
        val phones = pairingRepository.pairing
            .filterNot { it is Pairing.Showing }
            .map { (it as? Pairing.Paired)?.phones.orEmpty() }
        combine(phones, settingsRepository.voiceGuideEnabled, ::SettingsState).collect(::setState)
    }

    fun disconnect(phone: PairedPhone) {
        pairingRepository.disconnect(phone.id)
        sendEvent(SettingsEvent.ShowMessage("${phone.name} 연결을 해제했어요"))
    }

    fun disconnectAll() {
        pairingRepository.disconnectAll()
        sendEvent(SettingsEvent.ShowMessage("모든 폰 연결을 해제했어요"))
    }

    fun setVoiceGuideEnabled(enabled: Boolean) = settingsRepository.setVoiceGuideEnabled(enabled)
}
