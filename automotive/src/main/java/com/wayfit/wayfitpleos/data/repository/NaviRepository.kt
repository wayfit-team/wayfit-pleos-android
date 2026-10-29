package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NaviRepository {

    val isAvailable: Boolean

    val location: StateFlow<GeoPoint?>

    /** 내비 경로 안내 중인 목적지. 안내가 없으면 null */
    val route: StateFlow<RouteSummary?>

    val waypointArrived: Flow<Unit>

    fun start(): Boolean

    fun stop()

    fun refresh()

    fun addWaypoint(store: Store): Boolean

    fun removeWaypoint()
}
