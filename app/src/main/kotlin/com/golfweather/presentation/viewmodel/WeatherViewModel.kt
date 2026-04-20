package com.golfweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.data.model.WeatherForecast
import com.golfweather.domain.usecase.GetWeatherForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
        val hourlyForecasts: List<WeatherForecast>
    ) : WeatherUiState()
    data class MidTermSuccess(
        val schedule: TeeOffSchedule,
        val dayForecast: MidTermForecast?
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

            val result = getWeatherForecastUseCase(schedule)
            _uiState.update {
                when (result) {
                    is GetWeatherForecastUseCase.WeatherResult.ShortTerm ->
                        WeatherUiState.ShortTermSuccess(schedule, result.hourlyForecasts)

                    is GetWeatherForecastUseCase.WeatherResult.MidTerm ->
                        WeatherUiState.MidTermSuccess(schedule, result.dailyForecast)

                    is GetWeatherForecastUseCase.WeatherResult.OutOfRange ->
                        WeatherUiState.OutOfRange

                    is GetWeatherForecastUseCase.WeatherResult.Error ->
                        WeatherUiState.Error(result.message)
                }
            }
        }
    }

    fun retry(schedule: TeeOffSchedule) {
        loadWeather(schedule)
    }
}
