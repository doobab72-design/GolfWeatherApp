package com.golfweather.data.api

import com.golfweather.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 공공데이터포털 전국 골프장 현황 API
 * Base URL: https://api.odcloud.kr/api/15118920/v1/
 */
interface GolfCourseApiService {

    @GET("uddi:xxxxxxxx")
    suspend fun getGolfCourses(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100,
        @Query("serviceKey") serviceKey: String = BuildConfig.PUBLIC_DATA_API_KEY,
        @Query("returnType") returnType: String = "JSON"
    ): PublicDataResponse

    @GET("uddi:xxxxxxxx")
    suspend fun searchGolfCourse(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 20,
        @Query("serviceKey") serviceKey: String = BuildConfig.PUBLIC_DATA_API_KEY,
        @Query("returnType") returnType: String = "JSON",
        @Query("cond[사업장명::LIKE]") keyword: String
    ): PublicDataResponse
}

data class PublicDataResponse(
    val currentCount: Int,
    val data: List<GolfCourseDto>,
    val matchCount: Int,
    val page: Int,
    val perPage: Int,
    val totalCount: Int
)

data class GolfCourseDto(
    val 사업장명: String?,
    val 도로명주소: String?,
    val 위도: String?,
    val 경도: String?,
    val 홀수: String?,
    val 전화번호: String?
)
