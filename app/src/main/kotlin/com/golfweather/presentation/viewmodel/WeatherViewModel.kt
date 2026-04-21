package com.golfweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.data.model.WeatherForecast
import com.golfweather.domain.usecase.GetWeatherForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WeatherUiState {
    data object Idle : WeatherUiState()
    data object Loading : WeatherUiState()
    data class ShortTermSuccess(
        val schedule: TeeOffSchedule,
        val hourlyForecasts: List<WeatherForecast>,
        val suitabilityScore: Int = 0
    ) : WeatherUiState()
    data class MidTermSuccess(
        val schedule: TeeOffSchedule,
        val dayForecast: MidTermForecast?,
        val suitabilityScore: Int = 0
    ) : WeatherUiState()
    data object OutOfRange : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun loadWeather(schedule: TeeOffSchedule) {
        viewModelScope.launch {
            _uiState.update { WeatherUiState.Loading }
            try {
                val result = getWeatherForecastUseCase(schedule)
                _uiState.update {
                    when (result) {
                        is GetWeatherForecastUseCase.WeatherResult.ShortTerm ->
                            WeatherUiState.ShortTermSuccess(
                                schedule = schedule,
                                hourlyForecasts = result.hourlyForecasts,
                                suitabilityScore = calculateShortTermSuitability(result.hourlyForecasts)
                            )

                        is GetWeatherForecastUseCase.WeatherResult.MidTerm ->
                            WeatherUiState.MidTermSuccess(
                                schedule = schedule,
                                dayForecast = result.dailyForecast,
                                suitabilityScore = calculateMidTermSuitability(result.dailyForecast)
                            )

                        is GetWeatherForecastUseCase.WeatherResult.OutOfRange ->
                            WeatherUiState.OutOfRange

                        is GetWeatherForecastUseCase.WeatherResult.Error ->
                            WeatherUiState.Error(result.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WeatherViewModel", "loadWeather 예외", e)
                _uiState.update {
                    WeatherUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
                }
            }
        }
    }

    fun retry(schedule: TeeOffSchedule) = loadWeather(schedule)

    // ── 라운드 적합도 계산 (단기: 시간별) ─────────────────────────────────────

    private fun calculateShortTermSuitability(forecasts: List<WeatherForecast>): Int {
        if (forecasts.isEmpty()) return 0

        val avgTemp  = forecasts.map { it.temperature.toDouble() }.average()
        val maxWind  = forecasts.maxOf { it.windSpeed }
        val maxRain  = forecasts.maxOf { it.precipitationProbability }

        val tempScore: Int = when {
            avgTemp < 0   -> 10
            avgTemp < 5   -> 35
            avgTemp < 10  -> 65
            avgTemp < 15  -> 85
            avgTemp <= 25 -> 100
            avgTemp <= 30 -> 85
            avgTemp <= 35 -> 55
            else          -> 25
        }
        val windScore: Int = when {
            maxWind < 3f  -> 100
            maxWind < 5f  -> 90
            maxWind < 7f  -> 75
            maxWind < 10f -> 55
            maxWind < 13f -> 30
            else          -> 10
        }
        val rainScore: Int = when {
            maxRain < 10 -> 100
            maxRain < 20 -> 85
            maxRain < 40 -> 65
            maxRain < 60 -> 35
            else         -> 15
        }

        return (tempScore * 0.30 + windScore * 0.45 + rainScore * 0.25)
            .toInt().coerceIn(0, 100)
    }

    // ── 라운드 적합도 계산 (중기: 일별) ──────────────────────────────────────

    private fun calculateMidTermSuitability(forecast: MidTermForecast?): Int {
        if (forecast == null) return 0

        val maxPop = maxOf(forecast.precipProbAm, forecast.precipProbPm)

        val skyScore: Int = when {
            forecast.skyConditionAm == SkyCondition.CLEAR
                || forecast.skyConditionPm == SkyCondition.CLEAR      -> 100
            forecast.skyConditionAm == SkyCondition.PARTLY_CLOUDY     -> 85
            forecast.skyConditionAm == SkyCondition.CLOUDY            -> 65
            forecast.skyConditionAm == SkyCondition.RAIN
                || forecast.skyConditionPm == SkyCondition.RAIN       -> 20
            forecast.skyConditionAm == SkyCondition.SNOW              -> 25
            else                                                       -> 50
        }
        val rainScore: Int = when {
            maxPop < 20 -> 100
            maxPop < 40 -> 70
            maxPop < 60 -> 40
            else        -> 15
        }
        val tempScore: Int = when {
            forecast.minTemperature < 0f  -> 30
            forecast.minTemperature < 5f  -> 60
            forecast.maxTemperature > 35f -> 55
            else                          -> 100
        }

        return (skyScore * 0.30 + rainScore * 0.40 + tempScore * 0.30)
            .toInt().coerceIn(0, 100)
    }
}
