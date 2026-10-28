package com.wayfit.wayfitpleos.data.pleos

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.wayfit.wayfitpleos.model.GeoPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FusedLocation SDK 래퍼 (GPS + 차량 센서 융합 위치).
 * 매장 반경 근접 알림(지오펜스)으로 방문 인증 증거를 만든다.
 * 런타임 권한 pleos.car.permission.FUSED_LOCATION 허용 후 start() 해야 한다.
 */
@Singleton
class PleosLocation @Inject constructor(@ApplicationContext context: Context) {

    private val appContext = context.applicationContext
    private val fused by lazy { FusedLocation(appContext) }
    private var started = false
    private var alertId: String? = null

    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location.asStateFlow()

    private val _geofenceEntered = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val geofenceEntered: SharedFlow<Long> = _geofenceEntered.asSharedFlow()

    private val locationListener = object : FusedLocationListener {
        override fun onChanged(data: LocationData?) {
            if (data != null) _location.value = data
        }

        override fun onError(e: Exception) { Log.w(TAG, "location error", e) }
    }

    private val proximityListener = object : ProximityAlertListener {
        override fun onRegistered(id: String) {
            alertId = id
        }

        override fun onTransition(event: ProximityAlertEvent) {
            if (event.transition == ProximityTransition.ENTER) {
                _geofenceEntered.tryEmit(System.currentTimeMillis())
            }
        }

        override fun onError(e: Exception) { Log.w(TAG, "proximity error", e) }
    }

    fun start(): Boolean {
        if (started) return true
        return runCatching {
            check(fused.isSupported()) { "FusedLocation not supported" }
            fused.initialize()
            fused.registerFusedLocationCallback(locationListener)
            started = true
        }.onFailure { Log.w(TAG, "FusedLocation start failed", it) }.isSuccess
    }

    fun stop() {
        if (!started) return
        clearGeofence()
        runCatching {
            fused.unregisterFusedLocationCallback(locationListener)
            fused.release()
        }.onFailure { Log.w(TAG, "FusedLocation stop failed", it) }
        started = false
    }

    /** 방문 중인 매장 반경에 진입 알림 등록. 기존 알림은 해제 (방문은 한 번에 하나) */
    fun watchGeofence(center: GeoPoint, radiusMeters: Float): Boolean {
        if (!started) return false
        clearGeofence()
        return runCatching {
            fused.addProximityAlert(
                ProximityAlertRequest(center.lat, center.lng, radiusMeters, setOf(ProximityTransition.ENTER)),
                proximityListener,
            )
        }.onFailure { Log.w(TAG, "addProximityAlert failed", it) }.isSuccess
    }

    fun clearGeofence() {
        val id = alertId ?: return
        runCatching { fused.removeProximityAlert(id) }
            .onFailure { Log.w(TAG, "removeProximityAlert failed", it) }
        alertId = null
    }

    fun currentPoint(): GeoPoint? = _location.value?.let { GeoPoint(it.latitude, it.longitude) }

    private companion object {
        const val TAG = "PleosLocation"
    }
}
