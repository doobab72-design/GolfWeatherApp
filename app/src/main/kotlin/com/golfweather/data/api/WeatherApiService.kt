package com.golfweather.data.api

import com.golfweather.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 기상청 API허브
 * Base URL: https://apihub.kma.go.kr/api/typ02/openApi/
 */
interface WeatherApiService {

    /**
     * 단기예보 (오늘~3일 이내)
     * VilageFcstInfoService2.0/getVilageFcst
     */
    @GET("VilageFcstInfoService2.0/getVilageFcst")
    suspend fun getShortTermForecast(
        @Query("authKey") authKey: String = BuildConfig.KMA_API_KEY,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 300,
        @Query("dataType") dataType: String = "JSON",
        @Query("base_date") baseDate: String,   // "yyyyMMdd"
        @Query("base_time") baseTime: String,   // "0200","0500","0800","1100","1400","1700","2000","2300"
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): KmaShortTermResponse

    /**
     * 중기기온예보 (4~10일)
     * MidFcstInfoService/getMidTa
     */
    @GET("MidFcstInfoService/getMidTa")
    suspend fun getMidTermTemperature(
        @Query("authKey") authKey: String = BuildConfig.KMA_API_KEY,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("dataType") dataType: String = "JSON",
        @Query("regId") regId: String,          // 중기예보 지역 코드
        @Query("tmFc") tmFc: String             // "yyyyMMdd0600" or "yyyyMMdd1800"
    ): KmaMidTaResponse

    /**
     * 중기육상예보 (4~10일)
     * MidFcstInfoService/getMidLandFcst
     */
    @GET("MidFcstInfoService/getMidLandFcst")
    suspend fun getMidTermLandForecast(
        @Query("authKey") authKey: String = BuildConfig.KMA_API_KEY,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("dataType") dataType: String = "JSON",
        @Query("regId") regId: String,
        @Query("tmFc") tmFc: String
    ): KmaMidLandResponse
}

// --- 단기예보 Response ---

data class KmaShortTermResponse(
    val response: KmaResponse<VilageFcstBody>?
)

data class KmaResponse<T>(
    val header: KmaHeader,
    val body: T?
)

data class KmaHeader(
    val resultCode: String,
    val resultMsg: String
)

data class VilageFcstBody(
    val dataType: String?,
    val items: VilageFcstItems?,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class VilageFcstItems(
    val item: List<VilageFcstItem>
)

/**
 * 단기예보 아이템
 * category: TMP(기온), POP(강수확률), WSD(풍속), VEC(풍향), REH(습도), SKY(하늘상태), PTY(강수형태)
 */
data class VilageFcstItem(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String,
    val nx: Int,
    val ny: Int
)

// --- 중기기온 Response ---

data class KmaMidTaResponse(
    val response: KmaResponse<MidTaBody>?
)

data class MidTaBody(
    val dataType: String?,
    val items: MidTaItems?,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class MidTaItems(
    val item: List<MidTaItem>
)

data class MidTaItem(
    val regId: String,
    val taMin3: Int?, val taMax3: Int?,
    val taMin4: Int?, val taMax4: Int?,
    val taMin5: Int?, val taMax5: Int?,
    val taMin6: Int?, val taMax6: Int?,
    val taMin7: Int?, val taMax7: Int?,
    val taMin8: Int?, val taMax8: Int?,
    val taMin9: Int?, val taMax9: Int?,
    val taMin10: Int?, val taMax10: Int?
)

// --- 중기육상예보 Response ---

data class KmaMidLandResponse(
    val response: KmaResponse<MidLandBody>?
)

data class MidLandBody(
    val dataType: String?,
    val items: MidLandItems?,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class MidLandItems(
    val item: List<MidLandItem>
)

data class MidLandItem(
    val regId: String,
    val rnSt3Am: Int?, val rnSt3Pm: Int?,
    val rnSt4Am: Int?, val rnSt4Pm: Int?,
    val rnSt5Am: Int?, val rnSt5Pm: Int?,
    val rnSt6Am: Int?, val rnSt6Pm: Int?,
    val rnSt7Am: Int?, val rnSt7Pm: Int?,
    val rnSt8: Int?,   val rnSt9: Int?,
    val rnSt10: Int?,
    val wf3Am: String?, val wf3Pm: String?,
    val wf4Am: String?, val wf4Pm: String?,
    val wf5Am: String?, val wf5Pm: String?,
    val wf6Am: String?, val wf6Pm: String?,
    val wf7Am: String?, val wf7Pm: String?,
    val wf8: String?,   val wf9: String?,
    val wf10: String?
)
