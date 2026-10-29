package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 차량 융합 위치(FusedLocation) + 매장 지오펜스 */
interface LocationRepository {

    val isAvailable: Boolean

    val location: StateFlow<GeoPoint?>

    val geofenceEntered: Flow<Unit>

    fun start(permissionGranted: Boolean): Boolean

    fun stop()

    fun watch(store: Store): Boolean

    fun clearWatch()
}
