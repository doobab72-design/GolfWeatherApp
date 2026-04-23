package com.golfweather.util

import kotlin.math.*

data class KmaGridPoint(val nx: Int, val ny: Int)

/**
 * 위도/경도 ↔ 기상청 격자 좌표 변환 유틸리티
 *
 * 기상청 공식 Lambert Conformal Conic Projection 변환 알고리즘
 * 참고: 기상청 수치예보모델 격자 정보 (5km 해상도)
 * https://www.kma.go.kr/kma/idex/sub02_0608.jsp
 *
 * 파라미터 (기상청 공식 값):
 *  - RE    : 지구 반경 = 6371.00877 km
 *  - GRID  : 격자 크기 = 5.0 km
 *  - SLAT1 : 표준위도 1 = 30.0°N
 *  - SLAT2 : 표준위도 2 = 60.0°N
 *  - OLON  : 기준점 경도 = 126.0°E
 *  - OLAT  : 기준점 위도 = 38.0°N
 *  - XO    : 기준점 격자 X = 43
 *  - YO    : 기준점 격자 Y = 136
 *
 * 검증 기준점 (기상청 공개 샘플):
 *  - 서울 (37.5665°N, 126.9780°E) → nx=60, ny=127
 *  - 제주 (33.4890°N, 126.4983°E) → nx=52, ny=38
 */
object CoordConverter {

    private const val RE = 6371.00877    // 지구 반경 (km)
    private const val GRID = 5.0         // 격자 간격 (km)
    private const val SLAT1 = 30.0       // 표준위도 1 (°)
    private const val SLAT2 = 60.0       // 표준위도 2 (°)
    private const val OLON = 126.0       // 기준점 경도 (°)
    private const val OLAT = 38.0        // 기준점 위도 (°)
    private const val XO = 43.0          // 기준점 X 격자
    private const val YO = 136.0         // 기준점 Y 격자

    private val PI = Math.PI
    private val DEGRAD = PI / 180.0
    private val RADDEG = 180.0 / PI

    // Lambert 투영 상수 사전 계산 (기상청 공식과 동일한 순서)
    private val re: Double = RE / GRID
    private val slat1rad: Double = SLAT1 * DEGRAD
    private val slat2rad: Double = SLAT2 * DEGRAD
    private val olonRad: Double = OLON * DEGRAD
    private val olatRad: Double = OLAT * DEGRAD

    // sn = ln(cos(φ1)/cos(φ2)) / ln(tan(π/4+φ2/2)/tan(π/4+φ1/2))
    private val sn: Double = run {
        val tanRatio = tan(PI * 0.25 + slat2rad * 0.5) / tan(PI * 0.25 + slat1rad * 0.5)
        ln(cos(slat1rad) / cos(slat2rad)) / ln(tanRatio)
    }

    // sf = tan(π/4+φ1/2)^sn * cos(φ1) / sn
    private val sf: Double = run {
        tan(PI * 0.25 + slat1rad * 0.5).pow(sn) * cos(slat1rad) / sn
    }

    // ro = re * sf / tan(π/4+φ0/2)^sn  (기준점의 투영 반경)
    private val ro: Double = re * sf / tan(PI * 0.25 + olatRad * 0.5).pow(sn)

    /**
     * 위도/경도 → 기상청 격자 (nx, ny)
     *
     * 기상청 공식 알고리즘과 동일한 수식 적용.
     * floor(value + 0.5) = round(value) 방식으로 반올림하여 격자 좌표 산출.
     */
    fun toGrid(lat: Double, lon: Double): KmaGridPoint {
        // ra = re * sf / tan(π/4 + lat_rad/2)^sn
        val ra = re * sf / tan(PI * 0.25 + lat * DEGRAD * 0.5).pow(sn)

        var theta = lon * DEGRAD - olonRad
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        // floor(value + 0.5): 기상청 레퍼런스 구현의 Math.floor(... + 0.5) 와 동일
        val x = floor(ra * sin(theta) + XO + 0.5).toInt()
        val y = floor(ro - ra * cos(theta) + YO + 0.5).toInt()

        return KmaGridPoint(nx = x, ny = y)
    }

    /**
     * 기상청 격자 (nx, ny) → 위도/경도
     */
    fun toLatLon(nx: Int, ny: Int): Pair<Double, Double> {
        val xn = nx - XO
        val yn = ro - ny + YO
        val ra = sqrt(xn * xn + yn * yn)

        var alat = (re * sf / ra).pow(1.0 / sn)
        alat = 2.0 * atan(alat) - PI * 0.5

        val theta = when {
            abs(xn) <= 0.0 -> 0.0
            abs(yn) <= 0.0 -> if (xn > 0) PI * 0.5 else -PI * 0.5
            else -> atan2(xn, yn)
        }

        val alon = theta / sn + olonRad

        return Pair(alat * RADDEG, alon * RADDEG)
    }
}
