package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Evidence
import com.wayfit.wayfitpleos.model.VerifyResult

interface VisitRepository {

    /** "들를게요" → 서버 방문 생성. 방문 ID 를 돌려준다 */
    suspend fun create(storeId: String, tripId: String): String

    suspend fun cancel(visitId: String)

    /** 모은 증거로 인증 요청. 조건 미달은 예외가 아니라 [VerifyResult.Rejected] */
    suspend fun verify(visitId: String, evidence: List<Evidence>): VerifyResult
}
