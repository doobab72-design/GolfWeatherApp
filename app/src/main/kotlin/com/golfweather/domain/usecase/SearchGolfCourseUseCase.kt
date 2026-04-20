package com.golfweather.domain.usecase

import com.golfweather.data.model.GolfCourse
import com.golfweather.data.repository.GolfCourseRepository
import javax.inject.Inject

class SearchGolfCourseUseCase @Inject constructor(
    private val repository: GolfCourseRepository
) {
    /**
     * 키워드로 골프장 검색 (최소 2글자 이상)
     */
    suspend operator fun invoke(keyword: String): Result<List<GolfCourse>> {
        if (keyword.length < 2) return Result.success(emptyList())
        return repository.searchGolfCourses(keyword.trim())
    }
}
