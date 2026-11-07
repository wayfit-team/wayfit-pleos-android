package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import kotlinx.coroutines.flow.StateFlow

interface PairingRepository {

    /** 연결된 폰 목록. 코드 발급과 무관하게 항상 최신 */
    val phones: StateFlow<List<PairedPhone>>

    /** 지금 띄우고 있는 연결 코드. 없으면 null */
    val code: StateFlow<PairingCode?>

    /** 서버에서 새 연결 코드 발급 (10분 유효, 1회용). 실패하면 예외 */
    suspend fun requestCode()

    /**
     * 서버에서 연결된 폰 수를 다시 받아온다. 코드를 띄운 사이 폰이 늘었으면 그 코드는 끝난다.
     * 연결 해제는 폰(PWA)에서만 할 수 있다 — 차량 쪽 해제 API 가 없다.
     */
    suspend fun refreshPhones()

    fun demoPhonePaired()
}
