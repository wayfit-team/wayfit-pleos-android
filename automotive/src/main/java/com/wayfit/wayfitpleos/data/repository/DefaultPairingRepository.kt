package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DefaultPairingRepository @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) : PairingRepository {

    private val _phones = MutableStateFlow<List<PairedPhone>>(emptyList())
    override val phones: StateFlow<List<PairedPhone>> = _phones.asStateFlow()

    private val _code = MutableStateFlow<PairingCode?>(null)
    override val code: StateFlow<PairingCode?> = _code.asStateFlow()

    private var countdown: Job? = null

    override fun requestCode() {
        countdown?.cancel()
        val code = (1..8).joinToString("") { Random.nextInt(10).toString() }
        countdown = scope.launch {
            for (left in PairingCode.TTL_SEC downTo 0) {
                _code.value = PairingCode(code, left)
                delay(1_000)
            }
        }
    }

    override fun disconnect(phoneId: String) {
        _phones.update { list -> list.filterNot { it.id == phoneId } }
    }

    override fun disconnectAll() {
        _phones.value = emptyList()
    }

    /** 폰이 코드로 연결되면 그 코드는 끝난다 (1회용) */
    override fun demoPhonePaired() {
        val names = listOf("Galaxy S25", "iPhone 17", "Galaxy Z Flip7")
        _phones.update { list ->
            list + PairedPhone(
                id = UUID.randomUUID().toString(),
                name = names[list.size % names.size],
                connectedAtMillis = System.currentTimeMillis(),
            )
        }
        countdown?.cancel()
        _code.value = null
    }
}
