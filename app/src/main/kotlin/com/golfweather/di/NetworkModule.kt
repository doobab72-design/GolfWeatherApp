package com.golfweather.di

import com.golfweather.data.api.GolfCourseApiService
import com.golfweather.data.api.GooglePlacesApiService
import com.golfweather.data.api.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("publicData")
    fun providePublicDataRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.odcloud.kr/api/15118920/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("googlePlaces")
    fun provideGooglePlacesRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("kma")
    fun provideKmaRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://apihub.kma.go.kr/api/typ02/openApi/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGolfCourseApiService(@Named("publicData") retrofit: Retrofit): GolfCourseApiService =
        retrofit.create(GolfCourseApiService::class.java)

    @Provides
    @Singleton
    fun provideGooglePlacesApiService(@Named("googlePlaces") retrofit: Retrofit): GooglePlacesApiService =
        retrofit.create(GooglePlacesApiService::class.java)

    @Provides
    @Singleton
    fun provideWeatherApiService(@Named("kma") retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)
}
