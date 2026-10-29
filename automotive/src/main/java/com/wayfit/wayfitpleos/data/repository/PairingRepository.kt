package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.model.PairingCode
import kotlinx.coroutines.flow.StateFlow

interface PairingRepository {

    /** 연결된 폰 목록. 코드 발급과 무관하게 항상 최신 */
    val phones: StateFlow<List<PairedPhone>>

    /** 지금 띄우고 있는 연결 코드. 없으면 null */
    val code: StateFlow<PairingCode?>

    /** 새 연결 코드 발급 (10분 유효). 백엔드 연동 전: 로컬 생성 */
    fun requestCode()

    /** 폰 한 대 연결 해제 */
    fun disconnect(phoneId: String)

    fun disconnectAll()

    fun demoPhonePaired()
}
