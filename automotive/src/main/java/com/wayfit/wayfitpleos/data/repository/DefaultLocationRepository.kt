package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.pleos.PleosLocation
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultLocationRepository @Inject constructor(
    private val fused: PleosLocation,
    @ApplicationScope scope: CoroutineScope,
) : LocationRepository {

    @Volatile
    override var isAvailable: Boolean = false
        private set

    override val location: StateFlow<GeoPoint?> = fused.location
        .map { data -> data?.let { GeoPoint(it.latitude, it.longitude) } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val geofenceEntered: Flow<Unit> = fused.geofenceEntered.map { }

    override fun start(permissionGranted: Boolean): Boolean =
        (permissionGranted && fused.start()).also { isAvailable = it }

    override fun stop() {
        fused.stop()
        isAvailable = false
    }

    override fun watch(store: Store): Boolean = fused.watchGeofence(store.location, store.radiusM)

    override fun clearWatch() = fused.clearGeofence()
}
