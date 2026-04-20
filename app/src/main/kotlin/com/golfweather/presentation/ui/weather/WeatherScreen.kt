package com.golfweather.presentation.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.presentation.ui.components.WeatherCard
import com.golfweather.presentation.viewmodel.SharedGolfCourseViewModel
import com.golfweather.presentation.viewmodel.WeatherUiState
import com.golfweather.presentation.viewmodel.WeatherViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    sharedViewModel: SharedGolfCourseViewModel,
    onBack: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val schedule by sharedViewModel.schedule.collectAsState()

    // SharedViewModel에서 schedule을 받으면 날씨 로드
    LaunchedEffect(schedule) {
        schedule?.let { viewModel.loadWeather(it) }
    }

    val courseName = schedule?.golfCourse?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(courseName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is WeatherUiState.Idle -> {}

                is WeatherUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is WeatherUiState.ShortTermSuccess -> {
                    ShortTermWeatherContent(
                        schedule = state.schedule,
                        forecasts = state.hourlyForecasts
                    )
                }

                is WeatherUiState.MidTermSuccess -> {
                    MidTermWeatherContent(
                        schedule = state.schedule,
                        forecast = state.dayForecast
                    )
                }

                is WeatherUiState.OutOfRange -> {
                    OutOfRangeContent()
                }

                is WeatherUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { schedule?.let { viewModel.retry(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortTermWeatherContent(
    schedule: TeeOffSchedule,
    forecasts: List<com.golfweather.data.model.WeatherForecast>
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("단기예보 (오늘~3일)", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = schedule.date.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "티오프 ${schedule.time} ~ ${schedule.estimatedEndTime}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = schedule.golfCourse.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (forecasts.isEmpty()) {
            item {
                Text(
                    text = "해당 시간대 예보 데이터가 없습니다.\n잠시 후 다시 시도해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            items(forecasts) { forecast ->
                WeatherCard(forecast = forecast)
            }
        }
    }
}

@Composable
private fun MidTermWeatherContent(
    schedule: TeeOffSchedule,
    forecast: MidTermForecast?
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("중기예보 (4~10일)", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = schedule.date.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "* 중기예보는 일별 예보만 제공됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            if (forecast == null) {
                Text(
                    text = "중기예보 데이터가 없습니다.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                MidTermForecastCard(forecast = forecast)
            }
        }
    }
}

@Composable
private fun MidTermForecastCard(forecast: MidTermForecast) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "오전",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text("하늘: ${forecast.skyConditionAm.label}")
            Text("강수확률: ${forecast.precipProbAm}%")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "오후",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text("하늘: ${forecast.skyConditionPm.label}")
            Text("강수확률: ${forecast.precipProbPm}%")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "기온: ${forecast.minTemperature.toInt()}°C ~ ${forecast.maxTemperature.toInt()}°C",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OutOfRangeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "예보 불가",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "기상청 예보는 최대 10일 이내만 제공됩니다.\n날짜를 오늘부터 10일 이내로 선택해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "날씨 정보를 불러올 수 없습니다",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text("다시 시도")
        }
    }
}
