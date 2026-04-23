package com.golfweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfweather.data.model.GolfCourse
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.domain.usecase.SearchGolfCourseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val searchQuery: String = "",
    val searchResults: List<GolfCourse> = emptyList(),
    val selectedCourse: GolfCourse? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.of(7, 0),
    val isSearching: Boolean = false,
    val showSearchSheet: Boolean = false,
    val searchError: String? = null,    // 검색 시트 안에 표시 (스낵바 대신)
    val errorMessage: String? = null    // 날씨 조회 등 전역 오류용
) {
    val canProceed: Boolean
        get() = selectedCourse != null

    val isDateOutOfRange: Boolean
        get() = selectedDate.toEpochDay() - LocalDate.now().toEpochDay() > 10
}

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchGolfCourseUseCase: SearchGolfCourseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    // ── 검색 시트 제어 ────────────────────────────────────────────────────────

    fun openSearchSheet() {
        _uiState.update {
            it.copy(
                showSearchSheet = true,
                searchQuery = "",
                searchResults = emptyList(),
                searchError = null
            )
        }
        _searchQuery.value = ""
    }

    fun closeSearchSheet() {
        _uiState.update { it.copy(showSearchSheet = false) }
    }

    // ── 검색 ─────────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = if (query.length < 2) emptyList() else it.searchResults
            )
        }
        _searchQuery.value = query
    }

    // ── 선택 ─────────────────────────────────────────────────────────────────

    fun onCourseSelected(course: GolfCourse) {
        _uiState.update {
            it.copy(
                selectedCourse = course,
                showSearchSheet = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun clearSelectedCourse() {
        _uiState.update {
            it.copy(
                selectedCourse = null,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        _searchQuery.value = ""
    }

    // ── 날짜·시간 ─────────────────────────────────────────────────────────────

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun buildTeeOffSchedule(): TeeOffSchedule? {
        val state = _uiState.value
        val course = state.selectedCourse ?: return null
        return TeeOffSchedule(
            golfCourse = course,
            date = state.selectedDate,
            time = state.selectedTime
        )
    }

    // ── 내부 ─────────────────────────────────────────────────────────────────

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            searchGolfCourseUseCase(query).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(searchResults = results, isSearching = false)
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("HomeViewModel", "검색 실패", error)
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            // 오류를 Sheet 안에 표시 (BottomSheet 뒤에 가려지는 스낵바 대신)
                            searchError = error.message ?: "검색 중 오류가 발생했습니다."
                        )
                    }
                }
            )
        }
    }
}
