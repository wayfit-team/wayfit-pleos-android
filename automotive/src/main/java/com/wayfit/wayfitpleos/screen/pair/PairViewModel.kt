package com.wayfit.wayfitpleos.screen.pair

import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
) : BaseViewModel<PairState, Event>() {

    override fun getInitialState(): PairState = PairState()

    override fun fetchData(): Job = viewModelScope.launch {
        // 들어올 때마다 새 폰을 연결하러 온 것으로 본다. 유효한 코드가 떠 있으면 그대로 둔다
        val current = pairingRepository.pairing.value
        if (current !is Pairing.Showing || current.secondsLeft <= 0) pairingRepository.requestCode()
        pairingRepository.pairing.collect { setState(PairState(it)) }
    }

    fun requestNewCode() = pairingRepository.requestCode()
}
