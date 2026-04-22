package com.golfweather.domain.usecase

import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.data.model.WeatherForecast
import com.golfweather.data.repository.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 단기(3일 이내) 또는 중기(4~10일) 예보를 TeeOffSchedule 에 맞게 조회하는 유스케이스
 */
class GetWeatherForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    sealed class WeatherResult {
        data class ShortTerm(val hourlyForecasts: List<WeatherForecast>) : WeatherResult()
        data class MidTerm(
            val dailyForecast: MidTermForecast?,
            val hourlyForecasts: List<WeatherForecast>   // 시간별 데이터 추가
        ) : WeatherResult()
        data object OutOfRange : WeatherResult()
        data class Error(val message: String) : WeatherResult()
    }

    suspend operator fun invoke(schedule: TeeOffSchedule): WeatherResult {
        return when {
            schedule.isOutOfForecastRange -> WeatherResult.OutOfRange

            schedule.isShortTermRange -> {
                weatherRepository.getShortTermForecast(
                    latitude  = schedule.golfCourse.latitude,
                    longitude = schedule.golfCourse.longitude,
                    targetDate = schedule.date,
                    targetTime = schedule.time
                ).fold(
                    onSuccess = { WeatherResult.ShortTerm(it) },
                    onFailure = { WeatherResult.Error(it.message ?: "날씨 정보를 불러올 수 없습니다.") }
                )
            }

            schedule.isMidTermRange -> coroutineScope {
                // 일별·시간별 동시에 요청
                val dailyDeferred = async {
                    weatherRepository.getMidTermForecast(
                        latitude  = schedule.golfCourse.latitude,
                        longitude = schedule.golfCourse.longitude,
                        targetDate = schedule.date
                    )
                }
                val hourlyDeferred = async {
                    weatherRepository.getMidTermHourlyForecast(
                        latitude  = schedule.golfCourse.latitude,
                        longitude = schedule.golfCourse.longitude,
                        targetDate = schedule.date,
                        targetTime = schedule.time
                    )
                }

                val dailyResult  = dailyDeferred.await()
                val hourlyResult = hourlyDeferred.await()

                if (dailyResult.isFailure && hourlyResult.isFailure) {
                    return@coroutineScope WeatherResult.Error(
                        dailyResult.exceptionOrNull()?.message ?: "중기 예보를 불러올 수 없습니다."
                    )
                }

                val targetDateStr = schedule.date.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                )
                WeatherResult.MidTerm(
                    dailyForecast  = dailyResult.getOrNull()
                        ?.firstOrNull { it.date == targetDateStr },
                    hourlyForecasts = hourlyResult.getOrElse { emptyList() }
                )
            }

            else -> WeatherResult.OutOfRange
        }
    }
}
