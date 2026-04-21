package com.golfweather.data.repository

import android.util.Log
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
        private const val TAG = "GolfCourseRepo"
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7일
    }

    /**
     * 골프장 이름 검색
     *
     * 우선순위:
     *  1. Room DB 캐시 (7일 TTL)
     *  2. Google Places textsearch (단일 호출 — type 필터 없음)
     *  3. 공공데이터 API fallback
     *
     * [버그 수정] type=golf_course 필터 제거
     *  - 한국 골프장 다수가 Google Places에 golf_course 태그가 없어 결과 0건
     *  - query에 "골프장" 키워드를 포함시키는 것으로 충분히 필터링됨
     *
     * [버그 수정] autocomplete + getPlaceDetails(per-result) → textsearch 단일 호출
     *  - details 개별 호출이 실패하면 결과 전체가 null 처리되던 문제 해결
     */
    suspend fun searchGolfCourses(keyword: String): Result<List<GolfCourse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 로컬 DB 캐시 검색
                val cached = dao.searchByName(keyword)
                if (cached.isNotEmpty()) {
                    Log.d(TAG, "캐시 히트: '$keyword' → ${cached.size}건")
                    return@runCatching cached
                }

                // 2. Google Places textsearch
                if (!ApiKeyValidator.isPlacesKeySet()) {
                    Log.w(TAG, "Places API 키 미설정 → 공공데이터 API fallback")
                    return@runCatching if (ApiKeyValidator.isPublicDataKeySet()) {
                        fetchFromPublicData(keyword)
                    } else {
                        error("골프장 검색 API 키가 설정되지 않았습니다. local.properties를 확인하세요.")
                    }
                }

                val query = "$keyword 골프장"
                Log.d(TAG, "Places textsearch 호출: query='$query'")

                val response = placesApi.searchPlaces(
                    query = query,
                    language = "ko"
                    // type, region 제거 — type=golf_course는 한국 골프장 상당수 누락시킴
                )

                Log.d(TAG, "Places 응답: status=${response.status}, 결과=${response.results.size}건")

                when (response.status) {
                    "OK" -> {
                        val courses = response.results.mapNotNull { result ->
                            val location = result.geometry?.location
                            if (location == null) {
                                Log.w(TAG, "geometry 누락, 건너뜀: ${result.name}")
                                return@mapNotNull null
                            }
                            GolfCourse(
                                id = result.place_id,
                                name = result.name,
                                address = result.formatted_address ?: "",
                                latitude = location.lat,
                                longitude = location.lng
                            )
                        }
                        Log.d(TAG, "파싱 완료: ${courses.size}건")
                        if (courses.isNotEmpty()) dao.insertAll(courses)
                        courses
                    }

                    "ZERO_RESULTS" -> {
                        Log.d(TAG, "결과 없음(ZERO_RESULTS) → 공공데이터 fallback 시도")
                        if (ApiKeyValidator.isPublicDataKeySet()) fetchFromPublicData(keyword)
                        else emptyList()
                    }

                    "REQUEST_DENIED" -> {
                        Log.e(TAG, "REQUEST_DENIED: GCP에서 Places API 활성화 및 결제 설정 필요")
                        error("Google Places API 오류(REQUEST_DENIED): GCP 콘솔에서 Places API 활성화 및 결제 설정을 확인하세요.")
                    }

                    "INVALID_REQUEST" -> {
                        Log.e(TAG, "INVALID_REQUEST: 잘못된 요청 파라미터")
                        error("Places API 오류(INVALID_REQUEST): 검색어를 확인하세요.")
                    }

                    else -> {
                        Log.e(TAG, "알 수 없는 status: ${response.status}")
                        if (ApiKeyValidator.isPublicDataKeySet()) {
                            Log.d(TAG, "공공데이터 API fallback 시도")
                            fetchFromPublicData(keyword)
                        } else {
                            error("Places API 오류: ${response.status}")
                        }
                    }
                }
            }
        }

    /**
     * 공공데이터 API에서 골프장 목록 조회
     */
    private suspend fun fetchFromPublicData(keyword: String): List<GolfCourse> {
        Log.d(TAG, "공공데이터 API 호출: keyword='$keyword'")
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

    /**
     * 오래된 캐시 정리
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        dao.deleteOldCache(System.currentTimeMillis() - CACHE_TTL_MS)
    }
}
