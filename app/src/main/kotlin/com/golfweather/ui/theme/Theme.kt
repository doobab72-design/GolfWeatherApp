package com.golfweather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GolfGreen80,
    secondary = SkyBlue80,
    tertiary = FairwayGold80,
    background = SurfaceDark,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = GolfGreen40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD7F0D7),
    onPrimaryContainer = GolfGreen40,
    secondary = SkyBlue60,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
    onSecondaryContainer = SkyBlue40,
    tertiary = FairwayGold60,
    background = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F4F0),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C19),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C19),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6B7280),
    outline = androidx.compose.ui.graphics.Color(0xFFD1D5DB),
    error = androidx.compose.ui.graphics.Color(0xFFE53935)
)

@Composable
fun GolfWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // 커스텀 골프 테마 항상 적용
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
