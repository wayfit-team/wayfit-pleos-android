package com.wayfit.wayfitpleos.screen.pair

import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 폰 연결 화면. 화면에 들어올 때마다(새 ViewModel) 새 폰을 연결하러 온 것으로 본다 */
@HiltViewModel
class PairViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
) : BaseViewModel<PairState, Event>() {

    override fun getInitialState(): PairState = PairState()

    private val phoneCountAtEntry = pairingRepository.phones.value.size

    init {
        // 유효한 코드가 이미 떠 있으면 그대로 쓴다
        if (pairingRepository.code.value?.expired != false) pairingRepository.requestCode()

        viewModelScope.launch {
            combine(pairingRepository.code, pairingRepository.phones) { code, phones ->
                PairState(code = code, phoneCount = phones.size, justPaired = phones.size > phoneCountAtEntry)
            }.collect(::setState)
        }
    }

    fun requestNewCode() = pairingRepository.requestCode()
}
