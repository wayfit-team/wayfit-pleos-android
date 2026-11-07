package com.wayfit.wayfitpleos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateVisitRequest(val storeId: String, val tripId: String)

@Serializable
data class VisitResponse(val visit: VisitDto)

@Serializable
data class VisitDto(
    val id: String,
    val storeId: String,
    val tripId: String,
    val status: String,
    val createdAt: String,
    val confidence: Int? = null,
    val rejectReason: String? = null,
)

@Serializable
data class VerifyVisitRequest(val evidence: List<EvidenceDto>)

@Serializable
data class EvidenceDto(
    /** LOCATION · GEOFENCE_ENTER · WAYPOINT_ARRIVED · GEAR_PARK · DOOR_OPEN */
    val type: String,
    /** ISO-8601 UTC */
    val occurredAt: String,
    val lat: Double? = null,
    val lng: Double? = null,
    /** FUSED · NAVI (위치 증거만) */
    val source: String? = null,
)

@Serializable
data class VerifyVisitResponse(val visit: VisitDto, val coupon: CouponDto? = null)
