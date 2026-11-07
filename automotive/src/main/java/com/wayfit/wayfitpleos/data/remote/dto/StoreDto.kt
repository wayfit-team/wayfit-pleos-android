package com.wayfit.wayfitpleos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NearbyStoresResponse(val stores: List<NearbyStoreDto>)

@Serializable
data class NearbyStoreDto(
    val storeId: String,
    val name: String,
    val category: String,
    val benefitText: String,
    val lat: Double,
    val lng: Double,
    /** 조회 위치에서 직선 거리(m) */
    val distanceM: Long,
    /** 경로 대비 우회 거리(m) */
    val detourM: Long,
)

@Serializable
data class CreateImpressionRequest(val storeId: String, val tripId: String)

@Serializable
data class CreateImpressionResponse(val recorded: Boolean, val impressionId: String)
