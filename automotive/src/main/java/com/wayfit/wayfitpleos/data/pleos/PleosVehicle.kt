package com.wayfit.wayfitpleos.data.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Vehicle SDK 래퍼.
 * - 기어: P단이면 주차로 본다 (Brake 도메인에는 주차 브레이크가 없고 페달 상태만 있음)
 * - 문: 운전석·조수석 문이 열리면 하차 증거
 * - 주행 시간: 장시간 운전 추천 우선순위용
 */
@Singleton
class PleosVehicle @Inject constructor(@ApplicationContext context: Context) {

    private val appContext = context.applicationContext
    // 비 Pleos 기기에서는 생성 자체가 실패할 수 있어 start() 안에서 처음 만든다
    private val vehicle by lazy { Vehicle(appContext) }
    private var started = false

    private val _gear = MutableStateFlow<VehicleGear?>(null)
    val gear: StateFlow<VehicleGear?> = _gear.asStateFlow()

    private val _doorOpened = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val doorOpened: SharedFlow<Long> = _doorOpened.asSharedFlow()

    /** SDK가 주는 원본 값. 단위(분/초)는 에뮬레이터 스파이크에서 확인 필요 — 현재 분으로 가정 */
    private val _tripDrivingTime = MutableStateFlow<Int?>(null)
    val tripDrivingTime: StateFlow<Int?> = _tripDrivingTime.asStateFlow()

    private val frontDoors = listOf(DoorArea(DoorValue.ROW_1_LEFT), DoorArea(DoorValue.ROW_1_RIGHT))

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            if (gear != null) _gear.value = gear
        }

        override fun onFailed(e: Exception) { logError("gear", e) }
    }

    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            if ((pos ?: 0) > 0) _doorOpened.tryEmit(System.currentTimeMillis())
        }

        override fun onFailed(e: Exception) { logError("door", e) }
    }

    private val drivingTimeListener = object : DrivingTimeListener {
        override fun onDrivingTimeUpdated(time: Int?) {
            _tripDrivingTime.value = time
        }

        override fun onFailed(e: Exception) { logError("drivingTime", e) }
    }

    /** @return SDK 연결 성공 여부. 실패해도 앱은 DEMO 모드로 계속 동작한다 */
    fun start(): Boolean {
        if (started) return true
        return runCatching {
            vehicle.initialize()
            vehicle.getGear().apply {
                getCurrentGearSelectionState({ _gear.value = it }, { logError("gear", it) })
                registerGearSelection(gearListener)
            }
            frontDoors.forEach { vehicle.getDoor().registerDoorPosition(it, doorListener) }
            vehicle.getOdometer().apply {
                getTripCurrentDrivingTime({ _tripDrivingTime.value = it }, { logError("drivingTime", it) })
                registerTripCurrentDrivingTime(drivingTimeListener)
            }
            started = true
        }.onFailure { logError("start", it) }.isSuccess
    }

    fun stop() {
        if (!started) return
        runCatching {
            vehicle.getGear().unregisterGearSelection(gearListener)
            frontDoors.forEach { vehicle.getDoor().unregisterDoorPosition(it, doorListener) }
            vehicle.getOdometer().unregisterTripCurrentDrivingTime(drivingTimeListener)
            vehicle.release()
        }.onFailure { logError("stop", it) }
        started = false
    }

    private fun logError(where: String, t: Throwable) = Log.w(TAG, "Vehicle $where failed", t)

    private companion object {
        const val TAG = "PleosVehicle"
    }
}
