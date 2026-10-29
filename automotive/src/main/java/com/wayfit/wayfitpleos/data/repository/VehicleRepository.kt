package com.wayfit.wayfitpleos.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {

    val isAvailable: Boolean

    /** 기어 P단이면 true. SDK 값이 없으면 null (주행 중으로 간주) */
    val parked: StateFlow<Boolean?>

    val doorOpened: Flow<Long>

    /** 이번 주행 누적 운전 시간(분) */
    val drivingMinutes: StateFlow<Int?>

    fun start(): Boolean

    fun stop()

    fun demoSetParked(parked: Boolean)

    fun demoSetDrivingMinutes(minutes: Int)
}
