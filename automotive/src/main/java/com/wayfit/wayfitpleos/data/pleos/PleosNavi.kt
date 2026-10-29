package com.wayfit.wayfitpleos.data.pleos

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NaviHelper SDK 래퍼. 조회 API(getXxx)는 결과를 리스너 콜백(onXxx)으로 돌려준다.
 */
@Singleton
class PleosNavi @Inject constructor(@ApplicationContext context: Context) : NaviHelperEventListener {

    private val appContext = context.applicationContext
    private val helper by lazy { NaviHelper(appContext) }
    @Volatile private var started = false

    private val _location = MutableStateFlow<CurrentLocationInfo?>(null)
    val location: StateFlow<CurrentLocationInfo?> = _location.asStateFlow()

    private val _destination = MutableStateFlow<DestinationInfo?>(null)
    val destination: StateFlow<DestinationInfo?> = _destination.asStateFlow()

    private val _status = MutableStateFlow(NavigationStatus.INACTIVE)
    val status: StateFlow<NavigationStatus> = _status.asStateFlow()

    private val _waypointArrived = MutableSharedFlow<WaypointArrivedInfo>(extraBufferCapacity = 4)
    val waypointArrived: SharedFlow<WaypointArrivedInfo> = _waypointArrived.asSharedFlow()

    fun start(): Boolean {
        if (started) return true
        return runCatching {
            helper.initialize()
            helper.addListener(this)
            started = true
            refresh()
        }.onFailure { Log.w(TAG, "NaviHelper start failed", it) }.isSuccess
    }

    fun stop() {
        if (!started) return
        runCatching {
            helper.removeListener(this)
            helper.release()
        }.onFailure { Log.w(TAG, "NaviHelper stop failed", it) }
        started = false
    }

    /** 현재 위치·목적지를 다시 요청. 결과는 onCurrentLocationInfo / onDestinationInfo 로 들어온다 */
    fun refresh() {
        if (!started) return
        runCatching {
            helper.getCurrentLocationInfo()
            helper.getDestinationInfo()
        }.onFailure { Log.w(TAG, "NaviHelper refresh failed", it) }
    }

    fun addWaypoint(store: Store): Boolean {
        if (!started) return false
        return runCatching {
            helper.addWaypoint(
                RequestWaypointInfo(
                    latitude = store.location.lat,
                    longitude = store.location.lng,
                    poiName = store.name,
                    waypointIndex = WaypointIndex.FIRST,
                    poiId = "",
                    address = store.name,
                    poiSubId = "0",
                )
            )
        }.onFailure { Log.w(TAG, "addWaypoint failed", it) }.isSuccess
    }

    fun removeWaypoint() {
        if (!started) return
        runCatching { helper.removeWaypoint(WaypointIndex.FIRST) }
            .onFailure { Log.w(TAG, "removeWaypoint failed", it) }
    }

    fun currentPoint(): GeoPoint? = _location.value?.let { GeoPoint(it.latitude, it.longitude) }

    // region NaviHelperEventListener — 필요한 콜백만 구현, 나머지는 SDK 기본 구현 사용

    override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
        _location.value = currentLocationInfo
    }

    override fun onDestinationInfo(destinationInfo: DestinationInfo) {
        _destination.value = destinationInfo
    }

    override fun onNavigationStatus(status: NavigationStatus) {
        _status.value = status
    }

    override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) {
        _waypointArrived.tryEmit(waypointArrivedInfo)
    }

    override fun onError(errorCode: NaviErrorCode) {
        Log.w(TAG, "NaviHelper error: $errorCode")
    }

    // endregion

    private companion object {
        const val TAG = "PleosNavi"
    }
}
