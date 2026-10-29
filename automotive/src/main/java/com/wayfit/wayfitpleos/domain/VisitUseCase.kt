package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.VisitStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

enum class VisitEvidence { WAYPOINT_ARRIVED, GEOFENCE_ENTER, DOOR_OPEN }

data class VisitStart(
    /** 경유지 단계를 완료로 볼지 (SDK 가 없으면 DEMO 로 보고 완료) */
    val waypointStepDone: Boolean,
    /** 내비는 있는데 경유지 추가에 실패 → 운전자에게 알린다 (Spec REQ-A9) */
    val waypointFailed: Boolean,
)

data class VerifiedVisit(val coupon: Coupon, val phonePaired: Boolean)

/**
 * 방문 도착 및 주차 인증 판정 (Spec REQ-A8~A13, A24).
 * 경유지·지오펜스·내비·차량 상태를 묶고, "도착 증거 + 기어 P단 = 인증" 규칙을 한곳에 둔다.
 */
class VisitUseCase @Inject constructor(
    private val naviRepository: NaviRepository,
    private val locationRepository: LocationRepository,
    private val vehicleRepository: VehicleRepository,
    private val couponRepository: CouponRepository,
    private val pairingRepository: PairingRepository,
    private val currentLocation: CurrentLocation,
) {
    /** 도착·하차 증거 (경유지 도착, 지오펜스 진입, 문 열림) */
    val evidence: Flow<VisitEvidence> = merge(
        naviRepository.waypointArrived.map { VisitEvidence.WAYPOINT_ARRIVED },
        locationRepository.geofenceEntered.map { VisitEvidence.GEOFENCE_ENTER },
        vehicleRepository.doorOpened.map { VisitEvidence.DOOR_OPEN },
    )

    val parked: Flow<Boolean> = vehicleRepository.parked.map { it == true }

    fun isParked(): Boolean = vehicleRepository.parked.value == true

    fun start(store: Store): VisitStart {
        val added = naviRepository.addWaypoint(store)
        locationRepository.watch(store)
        return VisitStart(
            waypointStepDone = added || !naviRepository.isAvailable,
            waypointFailed = naviRepository.isAvailable && !added,
        )
    }

    fun cancel() {
        naviRepository.removeWaypoint()
        locationRepository.clearWatch()
    }

    /** 매장까지 남은 거리(m). 위치를 모르면 null */
    fun distanceTo(store: Store): Int? = currentLocation.get()?.distanceTo(store.location)?.toInt()

    /** 지오펜스가 안 될 때 앱이 직접 반경 진입을 판정 (Spec EDGE-A13) */
    fun isWithinRadius(store: Store, distanceM: Int): Boolean = distanceM <= store.radiusM

    /** 인증 조건: 도착했고, P단이고, 아직 인증 중이 아님 (Spec REQ-A12) */
    fun canVerify(done: Set<VisitStep>, verifying: Boolean, parked: Boolean): Boolean =
        VisitStep.ARRIVED in done && parked && !verifying

    /** 인증 → 쿠폰 발급 (백엔드 연동 전: 로컬 발급) */
    suspend fun verify(store: Store): VerifiedVisit {
        locationRepository.clearWatch()
        val coupon = couponRepository.issue(store)
        return VerifiedVisit(coupon, phonePaired = pairingRepository.phones.value.isNotEmpty())
    }

    /** DEMO: 실제 SDK 처럼 기어를 P단으로 — 인증은 parked 스트림을 통해 같은 경로로 진행된다 */
    fun demoPark() = vehicleRepository.demoSetParked(true)
}
