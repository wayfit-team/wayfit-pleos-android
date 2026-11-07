package com.wayfit.wayfitpleos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceResponse(val deviceId: String, val deviceToken: String)

@Serializable
data class CreatePairingCodeResponse(
    /** 8자리 숫자, 10분 유효, 1회용 */
    val code: String,
    val expiresAt: String,
    /** QR 에 담을 PWA 연결 주소 */
    val pairingUrl: String,
)

@Serializable
data class DevicePairingStatusResponse(val deviceId: String, val linkedUserCount: Int)

@Serializable
data class ErrorResponse(val error: ErrorDetail)

@Serializable
data class ErrorDetail(val code: String, val message: String)
