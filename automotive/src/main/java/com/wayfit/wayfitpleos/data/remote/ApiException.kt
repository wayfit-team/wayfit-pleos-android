package com.wayfit.wayfitpleos.data.remote

import com.wayfit.wayfitpleos.data.remote.dto.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** 서버가 공통 에러 형식(`{"error":{"code","message"}}`)으로 돌려준 실패 */
class ApiException(
    val httpStatus: Int,
    /** 서버 ErrorCode (예: VISIT_NO_PARKING). 형식이 다른 응답이면 null */
    val code: String?,
    message: String,
) : Exception(message)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Retrofit 의 [HttpException] 을 [ApiException] 으로 바꾼다. 네트워크 오류(IOException)는 그대로 던진다 */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    val body = e.response()?.errorBody()?.string()
    val error = body?.let { runCatching { errorJson.decodeFromString<ErrorResponse>(it).error }.getOrNull() }
    throw ApiException(e.code(), error?.code, error?.message ?: "HTTP ${e.code()}")
}
