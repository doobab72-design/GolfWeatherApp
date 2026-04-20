package com.golfweather.data.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * 티오프 일정
 */
data class TeeOffSchedule(
    val golfCourse: GolfCourse,
    val date: LocalDate,
    val time: LocalTime
) {
    /** 라운드 종료 예상 시간 (4시간 30분 후) */
    val estimatedEndTime: LocalTime
        get() = time.plusHours(4).plusMinutes(30)

    /** 기상청 단기예보 기준일 (오늘~3일 이내 여부) */
    val isShortTermRange: Boolean
        get() {
            val today = LocalDate.now()
            val diff = date.toEpochDay() - today.toEpochDay()
            return diff in 0..3
        }

    /** 중기예보 범위 (4~10일 이내 여부) */
    val isMidTermRange: Boolean
        get() {
            val today = LocalDate.now()
            val diff = date.toEpochDay() - today.toEpochDay()
            return diff in 4..10
        }

    /** 예보 불가 범위 (10일 초과) */
    val isOutOfForecastRange: Boolean
        get() {
            val today = LocalDate.now()
            val diff = date.toEpochDay() - today.toEpochDay()
            return diff > 10
        }
}
