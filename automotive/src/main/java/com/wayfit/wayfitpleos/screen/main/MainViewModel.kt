package com.wayfit.wayfitpleos.screen.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wayfit.wayfitpleos.data.di.IoDispatcher
import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DemoCommand
import com.wayfit.wayfitpleos.data.repository.DemoRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import com.wayfit.wayfitpleos.model.SdkStatus
import com.wayfit.wayfitpleos.model.TripPolicy
import com.wayfit.wayfitpleos.screen.base.BaseViewModel
import com.wayfit.wayfitpleos.screen.base.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** SDK 연결 수명, 앱 공통 상태(주차·쿠폰 수·SDK 상태), DEMO 조작을 담당 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val naviRepository: NaviRepository,
    private val locationRepository: LocationRepository,
    private val voiceRepository: VoiceRepository,
    private val couponRepository: CouponRepository,
    private val pairingRepository: PairingRepository,
    private val demoRepository: DemoRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseViewModel<MainState, Event>() {

    override fun getInitialState(): MainState = MainState()

    private var sdkStarted = false

    init {
        viewModelScope.launch {
            combine(
                vehicleRepository.parked,
                vehicleRepository.drivingMinutes,
                couponRepository.coupons,
            ) { parked, minutes, coupons -> Triple(parked, minutes, coupons) }
                .collect { (parked, minutes, coupons) ->
                    // sdk 필드는 startSdks 가 따로 채우므로 덮어쓰지 않게 원자적으로 갱신
                    updateState {
                        it.copy(
                            parked = parked == true,
                            longDrive = (minutes ?: 0) >= TripPolicy.LONG_DRIVE_MINUTES,
                            activeCouponCount = coupons.count { c -> !c.redeemed },
                        )
                    }
                }
        }
    }

    /** 권한 요청이 끝난 뒤 MainActivity 가 호출 */
    fun startSdks(fusedLocationGranted: Boolean) {
        if (sdkStarted) return
        sdkStarted = true
        viewModelScope.launch {
            val sdk = withContext(ioDispatcher) {
                val vehicle = vehicleRepository.start()
                val navi = naviRepository.start()
                val location = locationRepository.start(fusedLocationGranted)
                voiceRepository.start()
                SdkStatus(
                    vehicle = vehicle,
                    navi = navi,
                    location = location,
                    tts = voiceRepository.isTtsAvailable,
                    stt = voiceRepository.isSttAvailable,
                )
            }
            Log.i(TAG, "SDK status: $sdk")
            updateState { it.copy(sdk = sdk) }
        }
    }

    // region DEMO (debug 빌드 패널)

    fun demoToggleParked() = vehicleRepository.demoSetParked(!stateFlow.value.parked)

    fun demoToggleLongDrive() =
        vehicleRepository.demoSetDrivingMinutes(if (stateFlow.value.longDrive) 35 else 130)

    fun demoRecommend() = demoRepository.send(DemoCommand.RECOMMEND)

    fun demoArriveAndPark() = demoRepository.send(DemoCommand.ARRIVE_AND_PARK)

    fun demoPhonePaired() = pairingRepository.demoPhonePaired()

    // endregion

    override fun onCleared() {
        voiceRepository.stop()
        vehicleRepository.stop()
        naviRepository.stop()
        locationRepository.stop()
        super.onCleared()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
