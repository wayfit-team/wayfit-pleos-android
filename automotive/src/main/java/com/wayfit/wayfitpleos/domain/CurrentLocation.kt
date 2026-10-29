package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.model.GeoPoint
import javax.inject.Inject

/** 차량 현재 위치: 융합 위치(FusedLocation) 우선, 없으면 내비 위치 (Spec REQ-A25) */
class CurrentLocation @Inject constructor(
    private val locationRepository: LocationRepository,
    private val naviRepository: NaviRepository,
) {
    fun get(): GeoPoint? = locationRepository.location.value ?: naviRepository.location.value
}
