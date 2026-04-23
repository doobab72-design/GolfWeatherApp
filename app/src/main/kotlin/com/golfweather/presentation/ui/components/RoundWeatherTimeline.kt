package com.golfweather.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.data.model.WeatherForecast
import java.time.format.DateTimeFormatter

/**
 * 티오프 시각부터 4~5시간 라운드 날씨 타임라인
 */
@Composable
fun RoundWeatherTimeline(
    schedule: TeeOffSchedule,
    forecasts: List<WeatherForecast>,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)")

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = schedule.golfCourse.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Text(
            text = schedule.date.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        Text(
            text = "티오프 ${schedule.time} ~ 예상 종료 ${schedule.estimatedEndTime}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        if (forecasts.isEmpty()) {
            Text(
                text = "해당 시간대 날씨 정보가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(forecasts) { forecast ->
                    WeatherCard(forecast = forecast)
                }
            }
        }
    }
}
