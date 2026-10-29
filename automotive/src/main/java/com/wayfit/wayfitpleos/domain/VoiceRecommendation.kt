package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.TripPolicy
import com.wayfit.wayfitpleos.model.VoiceIntent
import java.util.Locale
import javax.inject.Inject

/** 음성 추천 문답 흐름: 추천을 읽고, 5초간 "응/아니"를 듣고, 쿠폰 도착을 알린다 (Spec REQ-A20~A23) */
class VoiceRecommendation @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val enabled: Boolean get() = settingsRepository.voiceGuideEnabled.value

    /**
     * 추천을 읽고 듣는다. 음성 안내가 꺼져 있거나, 읽는 사이 추천이 끝났으면([stillAsking] false) null.
     * 듣는 동안 [onListening] 으로 화면에 알린다 — 코루틴이 취소돼도 마지막에 false 를 보낸다.
     */
    suspend fun ask(
        store: Store,
        distanceKm: Double,
        stillAsking: () -> Boolean,
        onListening: (Boolean) -> Unit,
    ): VoiceIntent? {
        if (!enabled) return null
        voiceRepository.speak(prompt(store, distanceKm))
        if (!stillAsking()) return null
        onListening(true)
        try {
            return VoiceIntent.classify(voiceRepository.listen(TripPolicy.VOICE_LISTEN_MS))
        } finally {
            onListening(false)
        }
    }

    suspend fun announceCoupon(storeName: String) {
        if (enabled) voiceRepository.speak("$storeName 쿠폰이 도착했어요")
    }

    fun stop() = voiceRepository.stopAll()

    companion object {
        fun prompt(store: Store, distanceKm: Double): String =
            "${spokenDistance(distanceKm)} 앞 ${store.name}, ${store.benefit}예요. 들를까요?"

        private fun spokenDistance(km: Double): String =
            if (km < 1) "${(km * 1000).toInt()}미터" else String.format(Locale.KOREA, "%.1f킬로미터", km)
    }
}
