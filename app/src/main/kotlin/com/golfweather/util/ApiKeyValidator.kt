package com.golfweather.util

import com.golfweather.BuildConfig

/**
 * API 키 유효성 검사 유틸리티
 *
 * 앱 실행 전 또는 각 API 호출 전에 키가 placeholder 상태인지 확인.
 * 실제 키 없이 네트워크 호출하면 즉시 인증 오류가 발생하므로
 * 사용자에게 명확한 안내 메시지를 제공하기 위해 사전 검증.
 */
object ApiKeyValidator {

    private const val PLACEHOLDER_PUBLIC_DATA = "YOUR_PUBLIC_DATA_API_KEY"
    private const val PLACEHOLDER_GOOGLE_PLACES = "YOUR_GOOGLE_PLACES_API_KEY"
    private const val PLACEHOLDER_KMA = "YOUR_KMA_API_KEY"

    data class ValidationResult(
        val isValid: Boolean,
        val missingKeys: List<ApiKeyType>
    ) {
        val errorMessage: String
            get() = if (isValid) ""
            else "다음 API 키가 설정되지 않았습니다:\n" +
                missingKeys.joinToString("\n") { "• ${it.displayName}" } +
                "\n\napp/build.gradle.kts의 buildConfigField 항목을 확인하세요."
    }

    enum class ApiKeyType(val displayName: String) {
        PUBLIC_DATA("공공데이터포털 (골프장 현황)"),
        GOOGLE_PLACES("Google Places (골프장 자동완성)"),
        KMA("기상청 API허브 (날씨 예보)")
    }

    /**
     * 모든 API 키 검증
     */
    fun validate(): ValidationResult {
        val missing = mutableListOf<ApiKeyType>()
        if (BuildConfig.PUBLIC_DATA_API_KEY == PLACEHOLDER_PUBLIC_DATA) {
            missing.add(ApiKeyType.PUBLIC_DATA)
        }
        if (BuildConfig.GOOGLE_PLACES_API_KEY == PLACEHOLDER_GOOGLE_PLACES) {
            missing.add(ApiKeyType.GOOGLE_PLACES)
        }
        if (BuildConfig.KMA_API_KEY == PLACEHOLDER_KMA) {
            missing.add(ApiKeyType.KMA)
        }
        return ValidationResult(isValid = missing.isEmpty(), missingKeys = missing)
    }

    /**
     * 기상청 API 키만 검증 (날씨 조회 직전 호출)
     */
    fun isKmaKeySet(): Boolean =
        BuildConfig.KMA_API_KEY != PLACEHOLDER_KMA

    /**
     * Google Places API 키만 검증 (골프장 검색 직전 호출)
     */
    fun isPlacesKeySet(): Boolean =
        BuildConfig.GOOGLE_PLACES_API_KEY != PLACEHOLDER_GOOGLE_PLACES

    /**
     * 공공데이터 API 키만 검증 (fallback 검색 직전 호출)
     */
    fun isPublicDataKeySet(): Boolean =
        BuildConfig.PUBLIC_DATA_API_KEY != PLACEHOLDER_PUBLIC_DATA
}
