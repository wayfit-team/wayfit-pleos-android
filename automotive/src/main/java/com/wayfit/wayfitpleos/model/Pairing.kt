package com.wayfit.wayfitpleos.model

/** 연결된 폰 (영속 상태) */
data class PairedPhone(
    val id: String,
    val name: String,
    val connectedAtMillis: Long,
)

/**
 * 폰 연결 코드 발급 세션 (일시 상태). 연결된 폰 목록과 따로 둔다 —
 * 하나의 상태로 합쳐 두면 코드를 띄우는 동안 "연결된 폰이 있는지"를 알 수 없게 된다.
 */
data class PairingCode(val code: String, val secondsLeft: Int) {
    val expired: Boolean get() = secondsLeft <= 0

    companion object {
        const val TTL_SEC = 600
        const val URL_BASE = "https://wayfit.app/pair?code="
    }
}
