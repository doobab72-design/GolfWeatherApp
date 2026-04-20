package com.golfweather.data.api

import com.golfweather.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Places API – 골프장 텍스트 검색
 * Base URL: https://maps.googleapis.com/maps/api/
 */
interface GooglePlacesApiService {

    /**
     * 텍스트 검색으로 골프장 후보 목록 획득
     */
    @GET("place/textsearch/json")
    suspend fun searchPlaces(
        @Query("query") query: String,
        @Query("type") type: String = "golf_course",
        @Query("language") language: String = "ko",
        @Query("region") region: String = "kr",
        @Query("key") apiKey: String = BuildConfig.GOOGLE_PLACES_API_KEY
    ): PlacesSearchResponse

    /**
     * Place Autocomplete – 실시간 자동완성
     */
    @GET("place/autocomplete/json")
    suspend fun getAutocompleteSuggestions(
        @Query("input") input: String,
        @Query("types") types: String = "establishment",
        @Query("components") components: String = "country:kr",
        @Query("language") language: String = "ko",
        @Query("key") apiKey: String = BuildConfig.GOOGLE_PLACES_API_KEY
    ): PlacesAutocompleteResponse

    /**
     * Place Details – Place ID로 위도/경도 획득
     */
    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,geometry,formatted_address",
        @Query("language") language: String = "ko",
        @Query("key") apiKey: String = BuildConfig.GOOGLE_PLACES_API_KEY
    ): PlaceDetailsResponse
}

// --- Response DTOs ---

data class PlacesSearchResponse(
    val results: List<PlaceResult>,
    val status: String,
    val next_page_token: String?
)

data class PlacesAutocompleteResponse(
    val predictions: List<AutocompletePrediction>,
    val status: String
)

data class AutocompletePrediction(
    val place_id: String,
    val description: String,
    val structured_formatting: StructuredFormatting?
)

data class StructuredFormatting(
    val main_text: String,
    val secondary_text: String?
)

data class PlaceDetailsResponse(
    val result: PlaceDetailResult?,
    val status: String
)

data class PlaceDetailResult(
    val name: String,
    val formatted_address: String?,
    val geometry: PlaceGeometry?
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val formatted_address: String?,
    val geometry: PlaceGeometry?
)

data class PlaceGeometry(
    val location: PlaceLocation
)

data class PlaceLocation(
    val lat: Double,
    val lng: Double
)
