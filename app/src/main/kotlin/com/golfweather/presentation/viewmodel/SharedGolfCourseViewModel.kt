package com.golfweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.golfweather.data.model.TeeOffSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * HomeScreen → WeatherScreen 간 TeeOffSchedule 공유용 Activity-scoped ViewModel
 *
 * Navigation route에 위도/경도를 Float으로 전달하면 소수점 정밀도가 손실됨.
 * (예: 37.5665 → 37.566498 로 변환되어 기상청 격자 계산 오류 발생 가능)
 *
 * 해결: route에는 식별용 인자만 전달하고, 실제 GolfCourse 객체는
 * Activity 생명주기를 가진 이 ViewModel에 보관.
 */
@HiltViewModel
class SharedGolfCourseViewModel @Inject constructor() : ViewModel() {

    private val _schedule = MutableStateFlow<TeeOffSchedule?>(null)
    val schedule: StateFlow<TeeOffSchedule?> = _schedule.asStateFlow()

    fun setSchedule(schedule: TeeOffSchedule) {
        _schedule.value = schedule
    }

    fun clearSchedule() {
        _schedule.value = null
    }
}
