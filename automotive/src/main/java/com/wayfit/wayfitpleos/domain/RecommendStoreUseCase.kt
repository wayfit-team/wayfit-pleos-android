package com.wayfit.wayfitpleos.domain

import com.wayfit.wayfitpleos.data.repository.StoreRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.model.Store
import com.wayfit.wayfitpleos.model.TripPolicy
import javax.inject.Inject

data class Recommendation(
    val store: Store,
    val distanceKm: Double,
    /** 추천 이유 (예: 장시간 운전). 없으면 null */
    val reason: String?,
)

/** 다음에 추천할 매장과 거리·이유를 정하고 노출을 기록한다 (Spec REQ-A2, A5) */
class RecommendStoreUseCase @Inject constructor(
    private val storeRepository: StoreRepository,
    private val vehicleRepository: VehicleRepository,
    private val currentLocation: CurrentLocation,
    private val tripSession: TripSession,
) {
    private var nextIndex = 0

    /** 경로 위에 추천할 매장이 없으면 null. 서버 조회에 실패하면 예외 */
    suspend operator fun invoke(): Recommendation? {
        val here = currentLocation.get()
        val stores = storeRepository.getRecommendableStores(here)
        if (stores.isEmpty()) return null
        val store = stores[nextIndex++ % stores.size]
        storeRepository.recordImpression(store, tripSession.tripId)
        val longDrive = (vehicleRepository.drivingMinutes.value ?: 0) >= TripPolicy.LONG_DRIVE_MINUTES
        return Recommendation(
            store = store,
            distanceKm = here?.distanceTo(store.location)?.div(1000) ?: store.aheadKm,
            reason = if (longDrive) LONG_DRIVE_REASON else null,
        )
    }

    companion object {
        const val LONG_DRIVE_REASON = "2시간째 운전 중이에요 · 잠깐 쉬어가세요"
    }
}
