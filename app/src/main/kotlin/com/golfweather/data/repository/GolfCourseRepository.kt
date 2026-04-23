package com.golfweather.data.repository

import android.util.Log
import com.golfweather.BuildConfig
import com.golfweather.data.api.GolfCourseApiService
import com.golfweather.data.api.GooglePlacesApiService
import com.golfweather.data.api.PlacesTextSearchRequest
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
        private const val TAG = "GolfCourseRepo"
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }

    /**
     * 골프장 이름 검색
     *
     * 우선순위:
     *  1. Room DB 캐시 (7일 TTL)
     *  2. Google Places API (New) – searchText
     *  3. 공공데이터 API fallback
     *
     * Places API (New)는 레거시와 달리 POST + Header 인증 방식이며
     * 프로젝트에서 활성화되어 있어 정상 동작함.
     */
    suspend fun searchGolfCourses(keyword: String): Result<List<GolfCourse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 로컬 DB 캐시
                val cached = dao.searchByName(keyword)
                if (cached.isNotEmpty()) {
                    Log.d(TAG, "캐시 히트: '$keyword' → ${cached.size}건")
                    return@runCatching cached
                }

                // 2. Google Places API (New)
                if (!ApiKeyValidator.isPlacesKeySet()) {
                    Log.w(TAG, "Places API 키 미설정")
                    return@runCatching fetchFromPublicDataOrEmpty(keyword)
                }

                val query = "$keyword 골프장"
                Log.d(TAG, "Places(New) 호출: textQuery='$query'")

                val response = placesApi.searchText(
                    apiKey = BuildConfig.GOOGLE_PLACES_API_KEY,
                    request = PlacesTextSearchRequest(textQuery = query)
                )

                val places = response.places
                Log.d(TAG, "Places(New) 응답: ${places.size}건")

                if (places.isNotEmpty()) {
                    val courses = places.mapNotNull { place ->
                        val loc = place.location ?: run {
                            Log.w(TAG, "location 누락: ${place.displayName?.text}")
                            return@mapNotNull null
                        }
                        GolfCourse(
                            id = place.id,
                            name = place.displayName?.text ?: return@mapNotNull null,
                            address = place.formattedAddress ?: "",
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    }
                    Log.d(TAG, "파싱 완료: ${courses.size}건")
                    if (courses.isNotEmpty()) dao.insertAll(courses)
                    courses
                } else {
                    Log.d(TAG, "결과 없음 → 공공데이터 fallback")
                    fetchFromPublicDataOrEmpty(keyword)
                }
            }
        }

    private suspend fun fetchFromPublicDataOrEmpty(keyword: String): List<GolfCourse> =
        if (ApiKeyValidator.isPublicDataKeySet()) fetchFromPublicData(keyword) else emptyList()

    private suspend fun fetchFromPublicData(keyword: String): List<GolfCourse> {
        Log.d(TAG, "공공데이터 API 호출: '$keyword'")
        val response = publicDataApi.searchGolfCourse(keyword = keyword)
        val courses = response.data.mapNotNull { dto ->
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
        }
        Log.d(TAG, "공공데이터 결과: ${courses.size}건")
        if (courses.isNotEmpty()) dao.insertAll(courses)
        return courses
    }

    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        dao.deleteOldCache(System.currentTimeMillis() - CACHE_TTL_MS)
    }
}
