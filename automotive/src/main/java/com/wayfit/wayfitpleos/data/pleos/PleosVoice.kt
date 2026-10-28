package com.wayfit.wayfitpleos.data.pleos

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.util.Log
import com.wayfit.wayfitpleos.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import ai.pleos.playground.stt.constant.ErrorType as SttErrorType
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttServerListener
import ai.pleos.playground.tts.constant.ErrorType as TtsErrorType
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnServerConnectionListener as TtsServerListener

/**
 * TextToSpeech + SpeechToText SDK 래퍼.
 * HYBRID 모드: 서버 우선, 네트워크가 없으면 온디바이스. 서버를 쓰려면 Client ID/Secret 으로 registerApp 필요
 * (값은 local.properties → BuildConfig, 비어 있으면 온디바이스로만 동작).
 */
class PleosVoice(context: Context) {

    private val appContext = context.applicationContext
    private val tts by lazy { TextToSpeech(appContext, TtsMode.HYBRID) }
    private val stt by lazy { SpeechToText(appContext, SttMode.HYBRID) }
    private var ttsReady = false
    private var sttReady = false

    private var speakDone: CompletableDeferred<Unit>? = null
    private var heard: CompletableDeferred<String?>? = null

    private val ttsEvents = object : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onDone() { speakDone?.complete(Unit) }
        override fun onStop() { speakDone?.complete(Unit) }
        override fun onError(errorType: TtsErrorType) {
            Log.w(TAG, "TTS error: $errorType")
            speakDone?.complete(Unit)
        }
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private val sttResults = object : ResultListener {
        override fun onUpdated(stt: String, completed: Boolean) {
            if (completed) heard?.complete(stt)
        }
        override fun onUpdatedEpdData(epdOn: Long, epdOff: Long) = Unit
        override fun onStartedRecognition() = Unit
        override fun onEndedRecognition() { heard?.complete(null) }
        override fun onError(errorType: SttErrorType) {
            Log.w(TAG, "STT error: $errorType")
            heard?.complete(null)
        }
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }

    val isTtsReady get() = ttsReady
    val isSttReady get() = sttReady

    fun start() {
        val clientId = BuildConfig.PLEOS_CLIENT_ID
        val clientSecret = BuildConfig.PLEOS_CLIENT_SECRET
        val hasCredentials = clientId.isNotBlank() && clientSecret.isNotBlank()

        runCatching {
            tts.initialize()
            tts.addEventListener(ttsEvents)
            ttsReady = true
            if (hasCredentials) {
                tts.registerApp(clientId, clientSecret, object : TtsServerListener {
                    override fun onConnected() { Log.i(TAG, "TTS server connected") }
                    override fun onFailed(errorType: TtsErrorType) { Log.w(TAG, "TTS server failed: $errorType") }
                })
            }
        }.onFailure { Log.w(TAG, "TTS start failed", it) }

        runCatching {
            stt.initialize()
            stt.addListener(sttResults)
            sttReady = true
            if (hasCredentials) {
                stt.registerApp(clientId, clientSecret, object : SttServerListener {
                    override fun onConnected() { Log.i(TAG, "STT server connected") }
                    override fun onFailed(errorType: SttErrorType) { Log.w(TAG, "STT server failed: $errorType") }
                })
            }
        }.onFailure { Log.w(TAG, "STT start failed", it) }
    }

    fun stop() {
        if (ttsReady) runCatching { tts.removeEventListener(ttsEvents); tts.release() }
        if (sttReady) runCatching { stt.removeListener(sttResults); stt.release() }
        ttsReady = false
        sttReady = false
    }

    /** 문장을 읽고 끝날 때까지 기다린다 (최대 [timeoutMs]). 준비 안 됐으면 바로 반환 */
    suspend fun speak(text: String, timeoutMs: Long = 15_000) {
        if (!ttsReady) return
        val done = CompletableDeferred<Unit>().also { speakDone = it }
        runCatching { tts.speak(text, true) }.onFailure {
            Log.w(TAG, "speak failed", it)
            return
        }
        withTimeoutOrNull(timeoutMs) { done.await() }
    }

    fun stopSpeaking() {
        if (ttsReady) runCatching { tts.stop() }
        speakDone?.complete(Unit)
    }

    /** [timeoutMs] 동안만 마이크를 열고 최종 인식 문장을 돌려준다. 실패·무응답이면 null (Spec NFR-A7) */
    suspend fun listen(timeoutMs: Long = 5_000): String? {
        if (!sttReady) return null
        val result = CompletableDeferred<String?>().also { heard = it }
        runCatching { stt.request() }.onFailure {
            Log.w(TAG, "listen failed", it)
            return null
        }
        return withTimeoutOrNull(timeoutMs) { result.await() }.also {
            runCatching { stt.stop() }
        }
    }

    fun stopListening() {
        if (sttReady) runCatching { stt.stop() }
        heard?.complete(null)
    }

    private companion object {
        const val TAG = "PleosVoice"
    }
}
