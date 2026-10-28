package com.wayfit.wayfitpleos.model

data class PairedPhone(
    val id: String,
    val name: String,
    val connectedAtMillis: Long,
)

sealed interface Pairing {
    data object NotPaired : Pairing
    data class Showing(val code: String, val secondsLeft: Int) : Pairing
    data class Paired(val phones: List<PairedPhone>) : Pairing {
        val phoneCount: Int get() = phones.size
    }

    companion object {
        const val CODE_TTL_SEC = 600
        const val URL_BASE = "https://wayfit.app/pair?code="
    }
}
