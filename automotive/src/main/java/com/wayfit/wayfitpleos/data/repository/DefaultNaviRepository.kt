package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.pleos.PleosNavi
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.RouteSummary
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
class DefaultNaviRepository @Inject constructor(
    private val navi: PleosNavi,
    @ApplicationScope scope: CoroutineScope,
) : NaviRepository {

    @Volatile
    override var isAvailable: Boolean = false
        private set

    override val location: StateFlow<GeoPoint?> = navi.location
        .map { info -> info?.let { GeoPoint(it.latitude, it.longitude) } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // remainDistance(m), remainTime(초)로 가정 — 에뮬레이터 스파이크에서 단위 확인
    override val route: StateFlow<RouteSummary?> = navi.destination
        .map { dest ->
            dest?.takeIf { it.name.isNotBlank() }?.let {
                RouteSummary(destination = it.name, remainingKm = it.remainDistance / 1000.0, etaMin = it.remainTime / 60)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val waypointArrived: Flow<Unit> = navi.waypointArrived.map { }

    override fun start(): Boolean = navi.start().also { isAvailable = it }

    override fun stop() {
        navi.stop()
        isAvailable = false
    }

    override fun refresh() = navi.refresh()

    override fun addWaypoint(store: Store): Boolean = navi.addWaypoint(store)

    override fun removeWaypoint() = navi.removeWaypoint()
}
