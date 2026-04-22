package com.golfweather.data.repository

import android.util.Log
import com.golfweather.data.api.OpenMeteoApiService
import com.golfweather.data.api.OpenMeteoHourly
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.PrecipitationType
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.WeatherForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val openMeteoApi: OpenMeteoApiService
) {
    companion object {
        private const val TAG = "WeatherRepository"

        // Open-Meteo hourly 파라미터
        private const val HOURLY_PARAMS =
            "temperature_2m,precipitation_probability,windspeed_10m,winddirection_10m,weathercode,relativehumidity_2m"

        // Open-Meteo daily 파라미터
        private const val DAILY_PARAMS =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode"
    }

    /**
     * 단기예보 조회 (오늘~3일)
     * Open-Meteo hourly 데이터에서 티오프 시각 기준 4시간 필터링
     */
    suspend fun getShortTermForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate
    ): Result<List<WeatherForecast>> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "단기예보 요청 (Open-Meteo): lat=$latitude lon=$longitude date=$targetDate")

            val response = openMeteoApi.getForecast(
                latitude = latitude,
                longitude = longitude,
                hourly = HOURLY_PARAMS,
                timezone = "Asia/Seoul",
                forecastDays = 4   // 오늘 포함 최대 4일
            )

            val hourly = response.hourly
                ?: error("시간별 예보 데이터가 없습니다.")

            Log.d(TAG, "단기예보 응답: ${hourly.time.size}개 시간대")

            parseHourlyForecast(hourly, targetDate)
        }
    }

    /**
     * 중기예보 조회 (4~10일)
     * Open-Meteo daily 데이터에서 해당 날짜 추출
     */
    suspend fun getMidTermForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate
    ): Result<List<MidTermForecast>> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "중기예보 요청 (Open-Meteo): lat=$latitude lon=$longitude date=$targetDate")

            val response = openMeteoApi.getForecast(
                latitude = latitude,
                longitude = longitude,
                daily = DAILY_PARAMS,
                timezone = "Asia/Seoul",
                forecastDays = 11   // 오늘 포함 최대 11일 (10일 후까지)
            )

            val daily = response.daily
                ?: error("일별 예보 데이터가 없습니다.")

            Log.d(TAG, "중기예보 응답: ${daily.time.size}일치")

            parseDailyForecast(daily)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Parsing helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Open-Meteo hourly → WeatherForecast 변환
     * 해당 날짜의 전체 24시간 데이터 반환
     */
    private fun parseHourlyForecast(
        hourly: OpenMeteoHourly,
        targetDate: LocalDate
    ): List<WeatherForecast> {
        val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val forecasts = mutableListOf<WeatherForecast>()

        hourly.time.forEachIndexed { i, timeStr ->
            // "2026-04-21T07:00" 형식 파싱
            if (!timeStr.startsWith(targetDateStr)) return@forEachIndexed

            val wmoCode = hourly.weathercode.getOrNull(i)
            // "yyyy-MM-ddTHH:mm" → "yyyyMMddHHmm"
            val dateTimeFmt = timeStr.replace("-", "").replace("T", "").replace(":", "")

            forecasts.add(
                WeatherForecast(
                    dateTime = dateTimeFmt,
                    temperature = (hourly.temperature2m.getOrNull(i) ?: 0.0).toFloat(),
                    precipitationProbability = hourly.precipitationProbability.getOrNull(i) ?: 0,
                    windSpeed = (hourly.windspeed10m.getOrNull(i) ?: 0.0).toFloat(),
                    windDirection = hourly.winddirection10m.getOrNull(i) ?: 0,
                    humidity = hourly.relativehumidity2m.getOrNull(i) ?: 0,
                    skyCondition = wmoToSkyCondition(wmoCode),
                    precipitationType = wmoToPrecipType(wmoCode)
                )
            )
        }

        Log.d(TAG, "단기예보 파싱: ${forecasts.size}건 (하루 전체)")
        return forecasts
    }

    /**
     * Open-Meteo daily → MidTermForecast 변환
     */
    private fun parseDailyForecast(
        daily: com.golfweather.data.api.OpenMeteoDaily
    ): List<MidTermForecast> {
        val forecasts = mutableListOf<MidTermForecast>()

        daily.time.forEachIndexed { i, dateStr ->
            // "2026-04-25" → "20260425"
            val dateFmt = dateStr.replace("-", "")
            val wmoCode = daily.weathercode.getOrNull(i)
            val sky = wmoToSkyCondition(wmoCode)
            val pop = daily.precipProbMax.getOrNull(i) ?: 0

            forecasts.add(
                MidTermForecast(
                    date = dateFmt,
                    minTemperature = (daily.temperatureMin.getOrNull(i) ?: 0.0).toFloat(),
                    maxTemperature = (daily.temperatureMax.getOrNull(i) ?: 0.0).toFloat(),
                    skyConditionAm = sky,
                    skyConditionPm = sky,
                    precipProbAm = pop,
                    precipProbPm = pop
                )
            )
        }

        Log.d(TAG, "중기예보 파싱: ${forecasts.size}일치")
        return forecasts
    }

    // ──────────────────────────────────────────────────────────────────────────
    // WMO weather code helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * WMO 날씨 코드 → SkyCondition
     * https://open-meteo.com/en/docs#weathervariables
     */
    private fun wmoToSkyCondition(code: Int?): SkyCondition = when (code) {
        0         -> SkyCondition.CLEAR
        1, 2      -> SkyCondition.PARTLY_CLOUDY
        3, 45, 48 -> SkyCondition.CLOUDY
        in 51..57 -> SkyCondition.RAIN   // 이슬비, 어는 이슬비
        in 61..67 -> SkyCondition.RAIN   // 비, 어는 비
        in 71..77 -> SkyCondition.SNOW   // 눈
        in 80..82 -> SkyCondition.RAIN   // 소나기
        85, 86    -> SkyCondition.SNOW   // 눈 소나기
        in 95..99 -> SkyCondition.RAIN   // 뇌우
        else      -> SkyCondition.CLEAR
    }

    /**
     * WMO 날씨 코드 → PrecipitationType
     */
    private fun wmoToPrecipType(code: Int?): PrecipitationType = when (code) {
        in 51..57 -> PrecipitationType.RAIN
        in 61..65 -> PrecipitationType.RAIN
        66, 67    -> PrecipitationType.RAIN_SNOW
        in 71..77 -> PrecipitationType.SNOW
        in 80..82 -> PrecipitationType.SHOWER
        85, 86    -> PrecipitationType.SNOW
        in 95..99 -> PrecipitationType.RAIN
        else      -> PrecipitationType.NONE
    }
}
