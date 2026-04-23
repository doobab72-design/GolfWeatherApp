package com.golfweather.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.WeatherForecast

// ── 하늘상태 → 이모지 (패키지 내 private) ───────────────────────────────────
private fun SkyCondition.emoji(): String = when (this) {
    SkyCondition.CLEAR         -> "☀️"
    SkyCondition.PARTLY_CLOUDY -> "⛅"
    SkyCondition.CLOUDY        -> "☁️"
    SkyCondition.RAIN          -> "🌧️"
    SkyCondition.SNOW          -> "❄️"
    SkyCondition.RAIN_SNOW     -> "🌨️"
}

@Composable
fun HourlyWeatherCard(
    forecast : WeatherForecast,
    modifier : Modifier = Modifier
) {
    // "yyyyMMddHHmm" → "HH:mm"
    val timeStr = forecast.dateTime.let {
        if (it.length >= 12) "${it.substring(8, 10)}:${it.substring(10, 12)}" else it
    }

    val isHighRain = forecast.precipitationProbability >= 40

    Card(
        modifier  = modifier.width(90.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 시각
            Text(
                text      = timeStr,
                fontSize  = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(2.dp))

            // 날씨 이모지
            Text(
                text     = forecast.skyCondition.emoji(),
                fontSize = 28.sp
            )

            // 기온
            Text(
                text       = "${forecast.temperature.toInt()}°",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            // 강수확률 (40% 이상이면 파란색)
            Text(
                text      = "${forecast.precipitationProbability}%",
                fontSize  = 11.sp,
                color     = if (isHighRain) Color(0xFF42A5F5)
                            else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 풍속
            Text(
                text     = "${forecast.windSpeed}m/s",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
