package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.remote.ApiException
import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.apiCall
import com.wayfit.wayfitpleos.data.remote.dto.CreateVisitRequest
import com.wayfit.wayfitpleos.data.remote.dto.VerifyVisitRequest
import com.wayfit.wayfitpleos.data.remote.toDto
import com.wayfit.wayfitpleos.data.remote.toModel
import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.VerifyResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVisitRepository @Inject constructor(
    private val api: WayfitApi,
) : VisitRepository {

    override suspend fun create(storeId: String, tripId: String): String =
        apiCall { api.createVisit(CreateVisitRequest(storeId, tripId)) }.visit.id

    override suspend fun cancel(visitId: String) {
        apiCall { api.cancelVisit(visitId) }
    }

    override suspend fun verify(visitId: String, evidence: List<Evidence>): VerifyResult = try {
        val response = apiCall { api.verifyVisit(visitId, VerifyVisitRequest(evidence.map { it.toDto() })) }
        val coupon = checkNotNull(response.coupon) { "인증됐는데 쿠폰이 없어요: $visitId" }
        VerifyResult.Verified(coupon.toModel(System.currentTimeMillis()))
    } catch (e: ApiException) {
        // 422 인증 조건 미달, 409 이미 취소된 방문 → 운전자에게 서버 안내 문구를 보여준다
        if (e.httpStatus != 422 && e.httpStatus != 409) throw e
        VerifyResult.Rejected(e.code, e.message.orEmpty())
    }
}
