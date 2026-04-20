package com.golfweather.di

import android.content.Context
import androidx.room.Room
import com.golfweather.data.database.AppDatabase
import com.golfweather.data.database.GolfCourseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideGolfCourseDao(database: AppDatabase): GolfCourseDao =
        database.golfCourseDao()
}
