package com.golfweather.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Google Places API (New)
 * Base URL: https://places.googleapis.com/v1/
 *
 * 레거시 Places API(maps.googleapis.com)는 프로젝트에서 비활성화됨(REQUEST_DENIED).
 * Places API (New)는 활성화되어 있어 정상 동작.
 */
interface GooglePlacesApiService {

    @POST("/v1/places:searchText")
    @Headers("X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.location")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Body request: PlacesTextSearchRequest
    ): PlacesTextSearchResponse
}

// Request

data class PlacesTextSearchRequest(
    val textQuery: String,
    val languageCode: String = "ko"
)

// Response

data class PlacesTextSearchResponse(
    val places: List<PlaceNew> = emptyList()
)

data class PlaceNew(
    val id: String,
    val displayName: DisplayName?,
    val formattedAddress: String?,
    val location: LatLngNew?
)

data class DisplayName(
    val text: String,
    val languageCode: String?
)

data class LatLngNew(
    val latitude: Double,
    val longitude: Double
)
