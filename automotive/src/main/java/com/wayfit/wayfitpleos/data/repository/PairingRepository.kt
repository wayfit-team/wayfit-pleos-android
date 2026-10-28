package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Pairing
import kotlinx.coroutines.flow.StateFlow

interface PairingRepository {

    val pairing: StateFlow<Pairing>

    /** 새 연결 코드 발급 (10분 유효). 백엔드 연동 전: 로컬 생성 */
    fun requestCode()

    /** 폰 한 대 연결 해제. 마지막 폰이면 미연결 상태가 된다 */
    fun disconnect(phoneId: String)

    fun disconnectAll()

    fun demoPhonePaired()
}
