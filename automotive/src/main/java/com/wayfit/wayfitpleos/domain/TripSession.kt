package com.wayfit.wayfitpleos.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이번 주행 ID. 서버는 노출을 기기·매장·트립 당 1회로 세고, 방문도 트립에 묶는다.
 * 시동(앱 실행) 한 번을 트립 하나로 본다.
 */
@Singleton
class TripSession @Inject constructor() {
    val tripId: String = "trip_${LocalDateTime.now().format(FORMAT)}_${UUID.randomUUID().toString().take(6)}"

    private companion object {
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
