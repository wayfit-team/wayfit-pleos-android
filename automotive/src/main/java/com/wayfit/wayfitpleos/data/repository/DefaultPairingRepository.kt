package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.remote.ApiException
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
import java.time.Instant
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
    private var serverPhones: List<PairedPhone> = emptyList()
    /** DEMO 버튼으로 붙인 가상 폰 (서버에는 없다) */
    private var demoPhones: List<PairedPhone> = emptyList()

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
        val phones = apiCall { api.phones() }.phones.map {
            PairedPhone(id = it.phoneId, name = "", connectedAtMillis = Instant.parse(it.linkedAt).toEpochMilli())
        }
        val increased = phones.size > serverPhones.size
        serverPhones = phones
        publishPhones()
        if (increased) endCode()
    }

    override suspend fun disconnect(phoneId: String) {
        if (demoPhones.any { it.id == phoneId }) {
            demoPhones = demoPhones.filterNot { it.id == phoneId }
        } else {
            try {
                apiCall { api.unlinkPhone(phoneId) }
            } catch (e: ApiException) {
                // 폰에서 먼저 해제했으면 이미 없다 → 목록에서만 지운다
                if (e.httpStatus != 404) throw e
            }
            serverPhones = serverPhones.filterNot { it.id == phoneId }
        }
        publishPhones()
    }

    override suspend fun disconnectAll() {
        apiCall { api.unlinkAllPhones() }
        serverPhones = emptyList()
        demoPhones = emptyList()
        publishPhones()
    }

    /** 폰이 코드로 연결되면 그 코드는 끝난다 (1회용) */
    override fun demoPhonePaired() {
        demoPhones = demoPhones + PairedPhone(
            id = "demo_${System.nanoTime()}",
            name = "",
            connectedAtMillis = System.currentTimeMillis(),
        )
        publishPhones()
        endCode()
    }

    private fun publishPhones() {
        _phones.value = (serverPhones + demoPhones).mapIndexed { index, phone -> phone.copy(name = "휴대폰 ${index + 1}") }
    }

    private fun endCode() {
        countdown?.cancel()
        _code.value = null
    }
}
