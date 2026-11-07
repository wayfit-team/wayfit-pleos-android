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

    /** 서버에서 연결된 폰 목록을 다시 받아온다. 코드를 띄운 사이 폰이 늘었으면 그 코드는 끝난다 */
    suspend fun refreshPhones()

    /** 폰 한 대 연결 해제. 그 폰에서는 이 차의 쿠폰이 더 보이지 않는다 (쿠폰은 차에 남음). 실패하면 예외 */
    suspend fun disconnect(phoneId: String)

    suspend fun disconnectAll()

    fun demoPhonePaired()
}
