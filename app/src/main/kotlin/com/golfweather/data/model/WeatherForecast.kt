package com.golfweather.data.model

/**
 * 시간별 날씨 예보
 */
data class WeatherForecast(
    val dateTime: String,           // "yyyyMMddHHmm" 형식
    val temperature: Float,         // 기온 (°C)
    val precipitationProbability: Int, // 강수확률 (%)
    val windSpeed: Float,           // 풍속 (m/s)
    val windDirection: Int,         // 풍향 (deg, 0~360)
    val humidity: Int,              // 습도 (%)
    val skyCondition: SkyCondition, // 하늘상태
    val precipitationType: PrecipitationType = PrecipitationType.NONE
)

enum class SkyCondition(val label: String) {
    CLEAR("맑음"),
    PARTLY_CLOUDY("구름조금"),
    CLOUDY("흐림"),
    RAIN("비"),
    SNOW("눈"),
    RAIN_SNOW("비/눈");

    companion object {
        /**
         * 기상청 SKY 코드 → SkyCondition
         * SKY: 1=맑음, 3=구름많음, 4=흐림
         * PTY (강수형태): 0=없음, 1=비, 2=비/눈, 3=눈, 4=소나기
         */
        fun from(skyCode: Int, ptyCode: Int): SkyCondition = when {
            ptyCode == 1 || ptyCode == 4 -> RAIN
            ptyCode == 2 -> RAIN_SNOW
            ptyCode == 3 -> SNOW
            skyCode == 1 -> CLEAR
            skyCode == 3 -> PARTLY_CLOUDY
            skyCode == 4 -> CLOUDY
            else -> CLEAR
        }
    }
}

enum class PrecipitationType(val label: String) {
    NONE("없음"),
    RAIN("비"),
    RAIN_SNOW("비/눈"),
    SNOW("눈"),
    SHOWER("소나기")
}

/**
 * 중기예보 일별 정보 (4~10일)
 */
data class MidTermForecast(
    val date: String,               // "yyyyMMdd"
    val minTemperature: Float,
    val maxTemperature: Float,
    val skyConditionAm: SkyCondition,
    val skyConditionPm: SkyCondition,
    val precipProbAm: Int,
    val precipProbPm: Int
)
