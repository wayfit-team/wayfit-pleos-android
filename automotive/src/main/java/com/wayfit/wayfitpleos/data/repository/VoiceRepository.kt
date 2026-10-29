package com.wayfit.wayfitpleos.data.repository

interface VoiceRepository {

    val isTtsAvailable: Boolean

    val isSttAvailable: Boolean

    fun start()

    fun stop()

    /** 다 읽을 때까지 기다린다 */
    suspend fun speak(text: String)

    /** [timeoutMs] 동안 듣고 최종 인식 문장을 돌려준다. 실패·무응답이면 null */
    suspend fun listen(timeoutMs: Long): String?

    fun stopAll()
}
