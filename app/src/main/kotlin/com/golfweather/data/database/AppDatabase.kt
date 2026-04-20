package com.golfweather.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.golfweather.data.model.GolfCourse

@Database(
    entities = [GolfCourse::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun golfCourseDao(): GolfCourseDao

    companion object {
        const val DATABASE_NAME = "golf_weather.db"
    }
}
