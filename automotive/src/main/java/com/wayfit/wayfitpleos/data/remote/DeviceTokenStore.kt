package com.wayfit.wayfitpleos.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** `POST /v1/devices` 로 받은 기기 토큰 보관. 토큰은 등록 응답에서만 한 번 내려온다 */
interface DeviceTokenStore {
    val token: String?
    fun save(deviceId: String, token: String)
    fun clear()
}

/** 앱 전용 저장소(SharedPreferences)에 둔다. 로그에는 남기지 않는다 */
@Singleton
class PrefsDeviceTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceTokenStore {

    private val prefs = context.getSharedPreferences("wayfit_device", Context.MODE_PRIVATE)

    override val token: String? get() = prefs.getString(KEY_TOKEN, null)

    override fun save(deviceId: String, token: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).putString(KEY_TOKEN, token).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_TOKEN = "device_token"
    }
}
