package com.wayfit.wayfitpleos.data.repository

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.wayfit.wayfitpleos.data.di.ApplicationScope
import com.wayfit.wayfitpleos.data.pleos.PleosVehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** SDK 이벤트와 DEMO 조작을 같은 상태로 합친다 (마지막 값이 이긴다) */
@Singleton
class DefaultVehicleRepository @Inject constructor(
    private val vehicle: PleosVehicle,
    @ApplicationScope private val scope: CoroutineScope,
) : VehicleRepository {

    override var isAvailable: Boolean = false
        private set

    private val _parked = MutableStateFlow<Boolean?>(null)
    override val parked: StateFlow<Boolean?> = _parked.asStateFlow()

    override val doorOpened: Flow<Long> = vehicle.doorOpened

    private val _drivingMinutes = MutableStateFlow<Int?>(null)
    override val drivingMinutes: StateFlow<Int?> = _drivingMinutes.asStateFlow()

    override fun start(): Boolean {
        isAvailable = vehicle.start()
        if (isAvailable) {
            scope.launch { vehicle.gear.filterNotNull().collect { _parked.value = it == VehicleGear.GEAR_PARK } }
            scope.launch { vehicle.tripDrivingTime.filterNotNull().collect { _drivingMinutes.value = it } }
        }
        return isAvailable
    }

    override fun stop() = vehicle.stop()

    override fun demoSetParked(parked: Boolean) {
        _parked.value = parked
    }

    override fun demoSetDrivingMinutes(minutes: Int) {
        _drivingMinutes.value = minutes
    }
}
