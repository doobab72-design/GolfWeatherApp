package com.golfweather.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.WeatherForecast
import com.golfweather.util.WindDirectionConverter

@Composable
fun WeatherCard(
    forecast: WeatherForecast,
    modifier: Modifier = Modifier
) {
    val timeStr = forecast.dateTime.let {
        if (it.length >= 12) "${it.substring(8, 10)}:${it.substring(10, 12)}"
        else it
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 시각 + 날씨 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = forecast.skyCondition.toIcon(),
                    contentDescription = forecast.skyCondition.label,
                    tint = forecast.skyCondition.toColor(),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = forecast.skyCondition.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = forecast.skyCondition.toColor()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 기상 정보 그리드
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherInfoItem(
                    icon = Icons.Default.Thermostat,
                    label = "기온",
                    value = "${forecast.temperature.toInt()}°C",
                    tint = Color(0xFFE57373)
                )
                WeatherInfoItem(
                    icon = Icons.Default.BeachAccess,
                    label = "강수확률",
                    value = "${forecast.precipitationProbability}%",
                    tint = Color(0xFF64B5F6)
                )
                WeatherInfoItem(
                    icon = Icons.Default.Opacity,
                    label = "습도",
                    value = "${forecast.humidity}%",
                    tint = Color(0xFF4FC3F7)
                )
                WeatherInfoItem(
                    icon = Icons.Default.Air,
                    label = "풍속",
                    value = "${forecast.windSpeed}m/s",
                    tint = Color(0xFF81C784)
                )
            }

            // 풍향
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "풍향: ${WindDirectionConverter.toKorean(forecast.windDirection)} (${forecast.windDirection}°)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeatherInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun SkyCondition.toIcon(): ImageVector = when (this) {
    SkyCondition.CLEAR -> Icons.Default.WbSunny
    SkyCondition.PARTLY_CLOUDY -> Icons.Default.WbCloudy
    SkyCondition.CLOUDY -> Icons.Default.Cloud
    SkyCondition.RAIN -> Icons.Default.BeachAccess
    SkyCondition.SNOW -> Icons.Default.Grain
    SkyCondition.RAIN_SNOW -> Icons.Default.Grain
}

private fun SkyCondition.toColor(): Color = when (this) {
    SkyCondition.CLEAR -> Color(0xFFFFA726)
    SkyCondition.PARTLY_CLOUDY -> Color(0xFF78909C)
    SkyCondition.CLOUDY -> Color(0xFF607D8B)
    SkyCondition.RAIN -> Color(0xFF42A5F5)
    SkyCondition.SNOW -> Color(0xFF90CAF9)
    SkyCondition.RAIN_SNOW -> Color(0xFF64B5F6)
}
