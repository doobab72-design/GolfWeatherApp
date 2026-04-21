package com.golfweather.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 날씨 API
 * Base URL: https://api.open-meteo.com/v1/
 *
 * - 무료, API 키 불필요
 * - WMO 표준 날씨 코드 사용
 * - timezone=Asia/Seoul 으로 한국 시간 기준 반환
 */
interface OpenMeteoApiService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String? = null,
        @Query("daily") daily: String? = null,
        @Query("timezone") timezone: String = "Asia/Seoul",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null
)

data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m")
    val temperature2m: List<Double?> = emptyList(),
    @SerializedName("precipitation_probability")
    val precipitationProbability: List<Int?> = emptyList(),
    @SerializedName("windspeed_10m")
    val windspeed10m: List<Double?> = emptyList(),
    @SerializedName("winddirection_10m")
    val winddirection10m: List<Int?> = emptyList(),
    val weathercode: List<Int?> = emptyList(),
    @SerializedName("relativehumidity_2m")
    val relativehumidity2m: List<Int?> = emptyList()
)

data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m_max")
    val temperatureMax: List<Double?> = emptyList(),
    @SerializedName("temperature_2m_min")
    val temperatureMin: List<Double?> = emptyList(),
    @SerializedName("precipitation_probability_max")
    val precipProbMax: List<Int?> = emptyList(),
    val weathercode: List<Int?> = emptyList()
)
