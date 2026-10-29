package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.pleos.PleosVoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVoiceRepository @Inject constructor(
    private val voice: PleosVoice,
) : VoiceRepository {

    override val isTtsAvailable: Boolean get() = voice.isTtsReady

    override val isSttAvailable: Boolean get() = voice.isSttReady

    override fun start() = voice.start()

    override fun stop() = voice.stop()

    override suspend fun speak(text: String) = voice.speak(text)

    override suspend fun listen(timeoutMs: Long): String? = voice.listen(timeoutMs)

    override fun stopAll() {
        voice.stopSpeaking()
        voice.stopListening()
    }
}
