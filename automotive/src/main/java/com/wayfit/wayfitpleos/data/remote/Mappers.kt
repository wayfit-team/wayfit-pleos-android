package com.wayfit.wayfitpleos.data.remote

import com.wayfit.wayfitpleos.data.remote.dto.CouponDto
import com.wayfit.wayfitpleos.data.remote.dto.EvidenceDto
import com.wayfit.wayfitpleos.data.remote.dto.NearbyStoreDto
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.GeoPoint
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.StoreCategory
import java.time.Instant
import kotlin.math.ceil

/** 우회 거리 → 시간 환산 속도 (시내 주행 + 주차 감안, 약 15km/h) */
private const val DETOUR_M_PER_MIN = 250.0

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

fun NearbyStoreDto.toModel(): Store = Store(
    id = storeId,
    name = name,
    category = StoreCategory.entries.firstOrNull { it.name == category } ?: StoreCategory.ETC,
    benefit = benefitText,
    location = GeoPoint(lat, lng),
    aheadKm = distanceM / 1000.0,
    detourMin = ceil(detourM / DETOUR_M_PER_MIN).toInt().coerceAtLeast(1),
)

fun CouponDto.toModel(nowMillis: Long): Coupon {
    val leftMillis = Instant.parse(expiresAt).toEpochMilli() - nowMillis
    return Coupon(
        id = id,
        storeName = storeNameSnapshot,
        benefit = benefitSnapshot,
        code = code6,
        expiresInDays = ceil(leftMillis.toDouble() / DAY_MILLIS).toInt().coerceAtLeast(0),
        redeemed = status == "REDEEMED",
        expired = status == "EXPIRED" || leftMillis <= 0,
    )
}

fun Evidence.toDto(): EvidenceDto = EvidenceDto(
    type = type.name,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis).toString(),
    lat = location?.lat,
    lng = location?.lng,
    source = source?.name,
)
