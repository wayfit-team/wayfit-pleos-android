package com.wayfit.wayfitpleos.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GeoPointTest {

    private val ajou = GeoPoint(37.2830, 127.0443)

    @Test
    fun `같은 지점은 거리 0`() {
        assertEquals(0.0, ajou.distanceTo(ajou), 0.001)
    }

    @Test
    fun `위도 0도 01은 약 1_1km`() {
        assertEquals(1_112.0, ajou.distanceTo(GeoPoint(37.2930, 127.0443)), 5.0)
    }

    @Test
    fun `서울-부산은 약 325km`() {
        val seoul = GeoPoint(37.5665, 126.9780)
        val busan = GeoPoint(35.1796, 129.0756)
        assertEquals(325_000.0, seoul.distanceTo(busan), 3_000.0)
    }

    @Test
    fun `northEastFrom 은 offset 의 역`() {
        val moved = ajou.offset(northM = 250.0, eastM = -400.0)
        val (north, east) = moved.northEastFrom(ajou)
        assertEquals(250.0, north, 0.5)
        assertEquals(-400.0, east, 0.5)
    }

    @Test
    fun `offset 으로 옮긴 거리만큼 떨어진다`() {
        assertEquals(700.0, ajou.distanceTo(ajou.offset(northM = 700.0, eastM = 0.0)), 1.0)
        assertEquals(500.0, ajou.distanceTo(ajou.offset(northM = 300.0, eastM = 400.0)), 1.0)
    }
}
