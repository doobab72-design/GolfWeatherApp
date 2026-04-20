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
    primary = GolfGreen60,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFB8F5B8),
    secondary = SkyBlue60,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFBBDEFB),
    tertiary = FairwayGold60,
    background = SurfaceLight,
    surface = SurfaceLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C19),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C19)
)

@Composable
fun GolfWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
