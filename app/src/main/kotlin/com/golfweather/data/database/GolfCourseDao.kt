package com.golfweather.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.golfweather.data.model.GolfCourse
import kotlinx.coroutines.flow.Flow

@Dao
interface GolfCourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<GolfCourse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: GolfCourse)

    @Query("SELECT * FROM golf_courses WHERE name LIKE '%' || :keyword || '%' ORDER BY name ASC LIMIT 20")
    suspend fun searchByName(keyword: String): List<GolfCourse>

    @Query("SELECT * FROM golf_courses WHERE id = :id")
    suspend fun findById(id: String): GolfCourse?

    @Query("SELECT * FROM golf_courses ORDER BY name ASC")
    fun getAllFlow(): Flow<List<GolfCourse>>

    @Query("DELETE FROM golf_courses WHERE cachedAt < :threshold")
    suspend fun deleteOldCache(threshold: Long)

    @Query("SELECT COUNT(*) FROM golf_courses")
    suspend fun count(): Int
}
