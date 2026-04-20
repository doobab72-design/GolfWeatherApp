package com.golfweather.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 골프장 정보 모델
 * Room DB 캐싱 + API 응답 공용
 */
@Entity(tableName = "golf_courses")
data class GolfCourse(
    @PrimaryKey
    val id: String,
    val name: String,           // 골프장명
    val address: String,        // 주소
    val latitude: Double,       // 위도
    val longitude: Double,      // 경도
    val holeCount: Int = 18,    // 홀 수
    val phoneNumber: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
