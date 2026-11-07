package com.wayfit.wayfitpleos.model

/** 방문 인증 증거 종류 (백엔드 EvidenceType 과 같은 이름) */
enum class EvidenceType { LOCATION, GEOFENCE_ENTER, WAYPOINT_ARRIVED, GEAR_PARK, DOOR_OPEN }

/** 위치를 어디서 얻었는지 (서버는 분석용으로만 저장) */
enum class LocationSource { FUSED, NAVI }

data class Evidence(
    val type: EvidenceType,
    val occurredAtMillis: Long,
    /** 서버 좌표계 위치. 모르면 null */
    val location: GeoPoint? = null,
    val source: LocationSource? = null,
)

/** 서버 방문 인증 결과 */
sealed interface VerifyResult {
    data class Verified(val coupon: Coupon) : VerifyResult

    /** 인증 조건 미달·취소된 방문. [message] 는 서버가 준 안내 문구 */
    data class Rejected(val code: String?, val message: String) : VerifyResult
}
