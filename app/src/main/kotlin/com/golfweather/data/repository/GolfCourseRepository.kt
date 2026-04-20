package com.golfweather.data.repository

import com.golfweather.data.api.GolfCourseApiService
import com.golfweather.data.api.GooglePlacesApiService
import com.golfweather.data.database.GolfCourseDao
import com.golfweather.data.model.GolfCourse
import com.golfweather.util.ApiKeyValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GolfCourseRepository @Inject constructor(
    private val publicDataApi: GolfCourseApiService,
    private val placesApi: GooglePlacesApiService,
    private val dao: GolfCourseDao
) {
    companion object {
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7일
    }

    /**
     * 골프장 이름 검색 (DB 캐시 우선, 없으면 Google Places + 공공데이터 API)
     */
    suspend fun searchGolfCourses(keyword: String): Result<List<GolfCourse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 로컬 DB 검색
                val cached = dao.searchByName(keyword)
                if (cached.isNotEmpty()) return@runCatching cached

                // 2. Google Places Autocomplete
                if (!ApiKeyValidator.isPlacesKeySet()) {
                    // Places API 키 미설정 시 공공데이터 API로 직접 시도
                    return@runCatching if (ApiKeyValidator.isPublicDataKeySet()) {
                        fetchFromPublicData(keyword)
                    } else {
                        error("골프장 검색 API 키가 설정되지 않았습니다. app/build.gradle.kts를 확인하세요.")
                    }
                }

                val autocomplete = placesApi.getAutocompleteSuggestions(
                    input = "$keyword 골프장"
                )
                // status가 OK가 아니거나 결과가 없으면 공공데이터 API fallback
                if (autocomplete.status != "OK" || autocomplete.predictions.isEmpty()) {
                    return@runCatching if (ApiKeyValidator.isPublicDataKeySet()) {
                        fetchFromPublicData(keyword)
                    } else {
                        emptyList()
                    }
                }

                val courses = autocomplete.predictions.mapNotNull { prediction ->
                    val details = runCatching {
                        placesApi.getPlaceDetails(prediction.place_id)
                    }.getOrNull()

                    val location = details?.result?.geometry?.location ?: return@mapNotNull null
                    GolfCourse(
                        id = prediction.place_id,
                        name = details.result?.name ?: prediction.structured_formatting?.main_text ?: "",
                        address = details.result?.formatted_address ?: prediction.structured_formatting?.secondary_text ?: "",
                        latitude = location.lat,
                        longitude = location.lng
                    )
                }

                // 3. DB 캐싱
                if (courses.isNotEmpty()) dao.insertAll(courses)
                courses
            }
        }

    /**
     * 공공데이터 API에서 골프장 목록 조회
     */
    private suspend fun fetchFromPublicData(keyword: String): List<GolfCourse> {
        val response = publicDataApi.searchGolfCourse(keyword = keyword)
        return response.data.mapNotNull { dto ->
            val lat = dto.위도?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = dto.경도?.toDoubleOrNull() ?: return@mapNotNull null
            GolfCourse(
                id = "${dto.사업장명}_${dto.도로명주소}".hashCode().toString(),
                name = dto.사업장명 ?: return@mapNotNull null,
                address = dto.도로명주소 ?: "",
                latitude = lat,
                longitude = lng,
                holeCount = dto.홀수?.toIntOrNull() ?: 18,
                phoneNumber = dto.전화번호 ?: ""
            )
        }.also { if (it.isNotEmpty()) dao.insertAll(it) }
    }

    /**
     * 오래된 캐시 정리
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        dao.deleteOldCache(System.currentTimeMillis() - CACHE_TTL_MS)
    }
}
