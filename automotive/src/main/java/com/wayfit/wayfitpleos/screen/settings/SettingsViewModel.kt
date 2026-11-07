package com.wayfit.wayfitpleos.screen.settings

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.model.PairedPhone
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
        launchReporting("연결된 폰을 불러오지 못했어요") { pairingRepository.refreshPhones() }
    }

    fun disconnect(phone: PairedPhone) = launchReporting("${phone.name} 연결을 해제하지 못했어요") {
        pairingRepository.disconnect(phone.id)
        sendEvent(SettingsEvent.ShowMessage("${phone.name} 연결을 해제했어요"))
    }

    fun disconnectAll() = launchReporting("연결을 해제하지 못했어요") {
        pairingRepository.disconnectAll()
        sendEvent(SettingsEvent.ShowMessage("모든 폰 연결을 해제했어요"))
    }

    fun setVoiceGuideEnabled(enabled: Boolean) = settingsRepository.setVoiceGuideEnabled(enabled)

    /** 서버 요청이 실패하면 [failMessage] 를 띄운다 */
    private fun launchReporting(failMessage: String, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, failMessage, e)
            sendEvent(SettingsEvent.ShowMessage(failMessage))
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
