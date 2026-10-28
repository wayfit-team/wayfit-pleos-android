package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.Pairing
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

    private val _pairing = MutableStateFlow<Pairing>(Pairing.NotPaired)
    override val pairing: StateFlow<Pairing> = _pairing.asStateFlow()

    /** 코드 화면을 띄우는 동안에도 이미 연결된 폰 목록은 유지한다 */
    private val phones = MutableStateFlow<List<PairedPhone>>(emptyList())

    private var countdown: Job? = null

    override fun requestCode() {
        countdown?.cancel()
        val code = (1..8).joinToString("") { Random.nextInt(10).toString() }
        countdown = scope.launch {
            for (left in Pairing.CODE_TTL_SEC downTo 0) {
                _pairing.value = Pairing.Showing(code, left)
                delay(1_000)
            }
        }
    }

    override fun disconnect(phoneId: String) {
        phones.update { list -> list.filterNot { it.id == phoneId } }
        publishPhones()
    }

    override fun disconnectAll() {
        phones.value = emptyList()
        publishPhones()
    }

    override fun demoPhonePaired() {
        val names = listOf("Galaxy S25", "iPhone 17", "Galaxy Z Flip7")
        phones.update { list ->
            list + PairedPhone(
                id = UUID.randomUUID().toString(),
                name = names[list.size % names.size],
                connectedAtMillis = System.currentTimeMillis(),
            )
        }
        publishPhones()
    }

    private fun publishPhones() {
        countdown?.cancel()
        _pairing.value = phones.value.takeIf { it.isNotEmpty() }?.let(Pairing::Paired) ?: Pairing.NotPaired
    }
}
