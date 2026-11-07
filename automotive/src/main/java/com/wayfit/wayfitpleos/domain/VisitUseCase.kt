package com.wayfit.wayfitpleos.domain

import android.util.Log
import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VisitRepository
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.EvidenceType
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.VerifyResult
import com.wayfit.wayfitpleos.model.VisitStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

enum class VisitEvidence(val type: EvidenceType) {
    WAYPOINT_ARRIVED(EvidenceType.WAYPOINT_ARRIVED),
    GEOFENCE_ENTER(EvidenceType.GEOFENCE_ENTER),
    DOOR_OPEN(EvidenceType.DOOR_OPEN),
}

data class VisitStart(
    /** 경유지 단계를 완료로 볼지 (SDK 가 없으면 DEMO 로 보고 완료) */
    val waypointStepDone: Boolean,
    /** 내비는 있는데 경유지 추가에 실패 → 운전자에게 알린다 (Spec REQ-A9) */
    val waypointFailed: Boolean,
)

sealed interface VisitResult {
    data class Verified(val coupon: Coupon, val phonePaired: Boolean) : VisitResult

    /** 서버가 인증을 거절했거나 진행 중인 방문이 없다. [message] 를 운전자에게 보여준다 */
    data class Rejected(val message: String) : VisitResult
}

/**
 * 방문 도착 및 주차 인증 (Spec REQ-A8~A13, A24).
 * 경유지·지오펜스·내비·차량 상태를 묶어 "도착 증거 + 기어 P단" 이면 서버에 인증을 요청한다.
 * 방문 중 생긴 증거는 시각·위치와 함께 모아 두었다가 인증 요청에 한꺼번에 보낸다 (서버 REQ-B8).
 */
class VisitUseCase @Inject constructor(
    private val naviRepository: NaviRepository,
    private val locationRepository: LocationRepository,
    private val vehicleRepository: VehicleRepository,
    private val couponRepository: CouponRepository,
    private val pairingRepository: PairingRepository,
    private val visitRepository: VisitRepository,
    private val tripSession: TripSession,
    private val currentLocation: CurrentLocation,
) {
    private class Session(val visitId: String, val store: Store) {
        val evidence = mutableListOf<Evidence>()

        /** DEMO 로 도착시켰으면 차량은 실제로 움직이지 않았으므로 매장 위치를 증거 위치로 쓴다 */
        var simulatedArrival = false
    }

    private var session: Session? = null

    /** 도착·하차 증거 (경유지 도착, 지오펜스 진입, 문 열림). 방문 중이면 증거로 모은다 */
    val evidence: Flow<VisitEvidence> = merge(
        naviRepository.waypointArrived.map { VisitEvidence.WAYPOINT_ARRIVED },
        locationRepository.geofenceEntered.map { VisitEvidence.GEOFENCE_ENTER },
        vehicleRepository.doorOpened.map { VisitEvidence.DOOR_OPEN },
    ).onEach { record(it.type) }

    val parked: Flow<Boolean> = vehicleRepository.parked.map { it == true }

    fun isParked(): Boolean = vehicleRepository.parked.value == true

    /** 서버에 방문을 만들고 경유지·지오펜스를 건다. 서버 요청이 실패하면 예외 (경유지도 걸지 않는다) */
    suspend fun start(store: Store): VisitStart {
        session = Session(visitRepository.create(store.id, tripSession.tripId), store)
        val added = naviRepository.addWaypoint(store)
        locationRepository.watch(store)
        return VisitStart(
            waypointStepDone = added || !naviRepository.isAvailable,
            waypointFailed = naviRepository.isAvailable && !added,
        )
    }

    suspend fun cancel() {
        naviRepository.removeWaypoint()
        locationRepository.clearWatch()
        val current = session ?: return
        session = null
        // 취소가 서버에 안 닿아도 다음 방문 생성 때 서버가 이전 방문을 자동 취소한다
        runCatching { visitRepository.cancel(current.visitId) }
            .onFailure { Log.w(TAG, "cancel failed: ${current.visitId}", it) }
    }

    /** 매장까지 남은 거리(m). 위치를 모르면 null */
    fun distanceTo(store: Store): Int? = currentLocation.get()?.distanceTo(store.location)?.toInt()

    /** 지오펜스가 안 될 때 앱이 직접 반경 진입을 판정 (Spec EDGE-A13). 반경 안이면 위치 증거를 남긴다 */
    fun checkWithinRadius(store: Store, distanceM: Int): Boolean {
        if (distanceM > store.radiusM) return false
        record(EvidenceType.LOCATION)
        return true
    }

    /** 인증 조건: 도착했고, P단이고, 아직 인증 중이 아님 (Spec REQ-A12) */
    fun canVerify(done: Set<VisitStep>, verifying: Boolean, parked: Boolean): Boolean =
        VisitStep.ARRIVED in done && parked && !verifying

    /** P단 증거를 더해 서버에 인증 요청 → 쿠폰. 네트워크 오류는 예외 (방문은 그대로 남아 다시 시도할 수 있다) */
    suspend fun verify(): VisitResult {
        val current = session ?: return VisitResult.Rejected("진행 중인 방문이 없어요")
        record(EvidenceType.GEAR_PARK)
        return when (val result = visitRepository.verify(current.visitId, current.evidence.toList())) {
            is VerifyResult.Verified -> {
                session = null
                locationRepository.clearWatch()
                couponRepository.add(result.coupon)
                VisitResult.Verified(result.coupon, phonePaired = pairingRepository.phones.value.isNotEmpty())
            }
            is VerifyResult.Rejected -> {
                Log.i(TAG, "verify rejected: ${result.code}")
                VisitResult.Rejected(result.message)
            }
        }
    }

    /** DEMO: 매장에 도착한 것으로 보고 매장 위치를 위치 증거로 남긴다 */
    fun demoArrive() {
        session?.simulatedArrival = true
        record(EvidenceType.LOCATION)
    }

    /** DEMO: 실제 SDK 처럼 기어를 P단으로 — 인증은 parked 스트림을 통해 같은 경로로 진행된다 */
    fun demoPark() = vehicleRepository.demoSetParked(true)

    private fun record(type: EvidenceType) {
        val current = session ?: return
        val here = if (current.simulatedArrival) current.store.location else currentLocation.get()
        current.evidence += Evidence(
            type = type,
            occurredAtMillis = System.currentTimeMillis(),
            location = here?.let(current.store::toServerPoint),
            source = if (current.simulatedArrival) null else currentLocation.source(),
        )
    }

    private companion object {
        const val TAG = "VisitUseCase"
    }
}
