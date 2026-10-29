package com.wayfit.wayfitpleos.screen.main

import com.wayfit.wayfitpleos.model.SdkStatus
import com.wayfit.wayfitpleos.screen.base.State

data class MainState(
    val sdk: SdkStatus = SdkStatus(),
    /** 기어 P단 여부. 정차 중에만 쿠폰함·폰 연결을 연다 */
    val parked: Boolean = false,
    val activeCouponCount: Int = 0,
    val longDrive: Boolean = false,
) : State
