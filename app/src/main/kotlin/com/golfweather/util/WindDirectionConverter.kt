package com.golfweather.util

/**
 * 풍향 각도(deg) → 16방위 문자열 변환
 */
object WindDirectionConverter {
    private val directions = arrayOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW"
    )

    private val directionsKo = arrayOf(
        "북", "북북동", "북동", "동북동",
        "동", "동남동", "남동", "남남동",
        "남", "남남서", "남서", "서남서",
        "서", "서북서", "북서", "북북서"
    )

    fun toKorean(degrees: Int): String {
        val idx = ((degrees + 11.25) / 22.5).toInt() % 16
        return directionsKo[idx]
    }

    fun toEnglish(degrees: Int): String {
        val idx = ((degrees + 11.25) / 22.5).toInt() % 16
        return directions[idx]
    }
}
