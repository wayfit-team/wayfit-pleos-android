package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.apiCall
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPairingRepository @Inject constructor(
    private val api: WayfitApi,
    @ApplicationScope private val scope: CoroutineScope,
) : PairingRepository {

    private val _phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    override val phones: StateFlow<List<PairedPhone>> = _phones.asStateFlow()

    private val _code = MutableStateFlow<PairingCode?>(null)
    override val code: StateFlow<PairingCode?> = _code.asStateFlow()

    private var countdown: Job? = null
    private var serverCount = 0
    /** DEMO 버튼으로 붙인 가상 폰 수 (서버에는 없다) */
    private var demoCount = 0

    override suspend fun requestCode() {
        val response = apiCall { api.createPairingCode() }
        countdown?.cancel()
        // 차량 시계가 틀려도 맞도록 서버 만료 시각(expiresAt) 대신 받은 시점부터 10분을 센다
        countdown = scope.launch {
            for (left in PairingCode.TTL_SEC downTo 0) {
                _code.value = PairingCode(response.code, left, response.pairingUrl)
                delay(1_000)
            }
        }
    }

    override suspend fun refreshPhones() {
        val count = apiCall { api.pairingStatus() }.linkedUserCount
        val increased = count > serverCount
        serverCount = count
        publishPhones()
        if (increased) endCode()
    }

    /** 폰이 코드로 연결되면 그 코드는 끝난다 (1회용) */
    override fun demoPhonePaired() {
        demoCount++
        publishPhones()
        endCode()
    }

    private fun publishPhones() {
        _phones.value = List(serverCount + demoCount) { index ->
            PairedPhone(id = "phone_${index + 1}", name = "휴대폰 ${index + 1}")
        }
    }

    private fun endCode() {
        countdown?.cancel()
        _code.value = null
    }
}
