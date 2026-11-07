package com.wayfit.wayfitpleos.screen.pair

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 폰 연결 화면. 화면에 들어올 때마다(새 ViewModel) 새 폰을 연결하러 온 것으로 본다 */
@HiltViewModel
class PairViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
) : BaseViewModel<PairState, Event>() {

    override fun getInitialState(): PairState = PairState()

    private val phoneCountAtEntry = pairingRepository.phones.value.size

    private data class CodeRequest(val loading: Boolean = false, val error: String? = null)

    private val request = MutableStateFlow(CodeRequest())

    init {
        // 유효한 코드가 이미 떠 있으면 그대로 쓴다
        if (pairingRepository.code.value?.expired != false) requestNewCode()

        viewModelScope.launch {
            combine(pairingRepository.code, pairingRepository.phones, request) { code, phones, request ->
                PairState(
                    code = code,
                    phoneCount = phones.size,
                    justPaired = phones.size > phoneCountAtEntry,
                    loading = request.loading,
                    error = request.error,
                )
            }.collect(::setState)
        }
        // 폰이 QR 을 찍었는지 서버에 물어본다 (코드를 띄운 동안만)
        viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                val state = stateFlow.value
                if (state.code?.expired == false && !state.justPaired) {
                    runCatching { pairingRepository.refreshPhones() }
                        .onFailure { if (it is CancellationException) throw it else Log.w(TAG, "poll failed", it) }
                }
            }
        }
    }

    fun requestNewCode() {
        if (request.value.loading) return
        request.value = CodeRequest(loading = true)
        viewModelScope.launch {
            request.value = try {
                pairingRepository.requestCode()
                CodeRequest()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "pairing code failed", e)
                CodeRequest(error = "연결 코드를 받지 못했어요. 네트워크를 확인해 주세요")
            }
        }
    }

    private companion object {
        const val TAG = "PairViewModel"
        const val POLL_MS = 3_000L
    }
}
