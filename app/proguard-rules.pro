# ============================================================
# GolfWeather App - ProGuard / R8 Rules
# ============================================================

# ── 1. Retrofit ──────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Retrofit interface 메서드 유지 (인터페이스 자체 + 어노테이션)
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── 2. OkHttp ────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── 3. Gson (JSON 직렬화/역직렬화) ───────────────────────────
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# @SerializedName 어노테이션이 붙은 필드 유지
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── 4. 앱 데이터 모델 (Gson 파싱 대상) ──────────────────────
# API 응답 데이터 클래스 필드명 변경 시 JSON 파싱 실패 → 전체 유지
-keep class com.golfweather.data.api.** { *; }
-keep class com.golfweather.data.model.** { *; }

# ── 5. Room Database ─────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# ── 6. Hilt (DI) ─────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# ── 7. Kotlin Coroutines ──────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── 8. Kotlin 리플렉션 & data class ──────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keep class kotlin.Metadata { *; }

# ── 9. BuildConfig ────────────────────────────────────────────
-keep class com.golfweather.BuildConfig { *; }

# ── 10. 일반적인 Android 유지 규칙 ───────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
