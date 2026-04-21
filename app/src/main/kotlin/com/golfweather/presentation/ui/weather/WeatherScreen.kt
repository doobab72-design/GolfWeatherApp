package com.golfweather.presentation.ui.weather

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.TeeOffSchedule
import com.golfweather.data.model.WeatherForecast
import com.golfweather.presentation.ui.components.HourlyWeatherCard
import com.golfweather.presentation.viewmodel.SharedGolfCourseViewModel
import com.golfweather.presentation.viewmodel.WeatherUiState
import com.golfweather.presentation.viewmodel.WeatherViewModel
import com.golfweather.util.WindDirectionConverter
import java.time.format.DateTimeFormatter

// ── 하늘상태 → 배경 그라디언트 ────────────────────────────────────────────
private fun skyGradient(sky: SkyCondition): Brush = Brush.linearGradient(
    when (sky) {
        SkyCondition.CLEAR        -> listOf(Color(0xFF0288D1), Color(0xFF0277BD), Color(0xFF01579B))
        SkyCondition.PARTLY_CLOUDY -> listOf(Color(0xFF0288D1), Color(0xFF546E7A), Color(0xFF37474F))
        SkyCondition.CLOUDY       -> listOf(Color(0xFF546E7A), Color(0xFF455A64), Color(0xFF37474F))
        SkyCondition.RAIN         -> listOf(Color(0xFF1A237E), Color(0xFF1565C0), Color(0xFF0D47A1))
        SkyCondition.SNOW         -> listOf(Color(0xFF90CAF9), Color(0xFF5C8FD6), Color(0xFF1565C0))
        SkyCondition.RAIN_SNOW    -> listOf(Color(0xFF1565C0), Color(0xFF7986CB), Color(0xFF3949AB))
    }
)

// ── 하늘상태 → 이모지 ──────────────────────────────────────────────────────
private fun SkyCondition.toEmoji(): String = when (this) {
    SkyCondition.CLEAR         -> "☀️"
    SkyCondition.PARTLY_CLOUDY -> "⛅"
    SkyCondition.CLOUDY        -> "☁️"
    SkyCondition.RAIN          -> "🌧️"
    SkyCondition.SNOW          -> "❄️"
    SkyCondition.RAIN_SNOW     -> "🌨️"
}

// ── 라운드 적합도 → 색상 ─────────────────────────────────────────────────
private fun scoreColor(score: Int): Color = when {
    score >= 90 -> Color(0xFF1B5E20)
    score >= 75 -> Color(0xFF2E7D32)
    score >= 60 -> Color(0xFF1565C0)
    score >= 40 -> Color(0xFFE65100)
    else        -> Color(0xFFC62828)
}

// ── 라운드 적합도 → 라벨 ────────────────────────────────────────────────
private fun scoreLabel(score: Int): String = when {
    score >= 90 -> "완벽한 라운드 ⛳"
    score >= 75 -> "좋은 날씨 😊"
    score >= 60 -> "괜찮은 날씨 🙂"
    score >= 40 -> "주의 필요 😐"
    else        -> "라운드 어려움 😟"
}

@Composable
fun WeatherScreen(
    sharedViewModel: SharedGolfCourseViewModel,
    onBack: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val schedule by sharedViewModel.schedule.collectAsState()

    LaunchedEffect(schedule) {
        schedule?.let { viewModel.loadWeather(it) }
    }

    when (val state = uiState) {
        is WeatherUiState.Idle    -> LoadingOverlay(onBack)
        is WeatherUiState.Loading -> LoadingOverlay(onBack)

        is WeatherUiState.ShortTermSuccess ->
            ShortTermWeatherContent(
                schedule  = state.schedule,
                forecasts = state.hourlyForecasts,
                score     = state.suitabilityScore,
                onBack    = onBack
            )

        is WeatherUiState.MidTermSuccess ->
            MidTermWeatherContent(
                schedule = state.schedule,
                forecast = state.dayForecast,
                score    = state.suitabilityScore,
                onBack   = onBack
            )

        is WeatherUiState.OutOfRange ->
            OutOfRangeContent(onBack)

        is WeatherUiState.Error ->
            ErrorContent(
                message = state.message,
                onBack  = onBack,
                onRetry = { schedule?.let { viewModel.retry(it) } }
            )
    }
}

// ── 로딩 ─────────────────────────────────────────────────────────────────────
@Composable
private fun LoadingOverlay(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF1565C0))))
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("날씨 불러오는 중…", color = Color.White, fontSize = 15.sp)
        }
    }
}

// ── 단기 예보 화면 ────────────────────────────────────────────────────────────
@Composable
private fun ShortTermWeatherContent(
    schedule  : TeeOffSchedule,
    forecasts : List<WeatherForecast>,
    score     : Int,
    onBack    : () -> Unit
) {
    val dominantSky = forecasts.maxByOrNull { 1 }?.skyCondition ?: SkyCondition.CLEAR
    val avgTemp     = if (forecasts.isEmpty()) 0f
                      else forecasts.map { it.temperature }.average().toFloat()
    val dateFmt     = DateTimeFormatter.ofPattern("MM/dd (E)")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 히어로 ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(skyGradient(dominantSky))
            ) {
                // 뒤로가기
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White)
                }

                // 장식 원
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-40).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dominantSky.toEmoji(), fontSize = 56.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${avgTemp.toInt()}°C",
                        color      = Color.White,
                        fontSize   = 64.sp,
                        fontWeight = FontWeight.Thin
                    )
                    Text(
                        dominantSky.label,
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${schedule.golfCourse.name}  ·  ${schedule.date.format(dateFmt)}",
                        color    = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp
                    )
                }
            }

            // ── 바디 ─────────────────────────────────────────────────────────
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
                shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color           = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 단기 배지
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0288D1).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "📅 단기예보 (3일 이내)",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize  = 12.sp,
                            color     = Color(0xFF0277BD),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 라운드 적합도
                    SuitabilityScoreCard(score = score)

                    // 시간별 예보
                    if (forecasts.isNotEmpty()) {
                        SectionTitle("시간별 예보")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding        = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(forecasts) { forecast ->
                                HourlyWeatherCard(forecast = forecast)
                            }
                        }
                    } else {
                        Text(
                            "해당 시간대 예보 데이터가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }

                    // 날씨 상세 정보
                    if (forecasts.isNotEmpty()) {
                        SectionTitle("라운드 상세 정보")
                        val first = forecasts.first()
                        WeatherDetailGrid(
                            windSpeed = first.windSpeed,
                            windDir   = first.windDirection,
                            humidity  = first.humidity,
                            rain      = first.precipitationProbability,
                            teeTime   = "${schedule.time} ~ ${schedule.estimatedEndTime}"
                        )
                    }
                }
            }
            // offset(-24dp) 보정
            Spacer(Modifier.height(0.dp))
        }
    }
}

// ── 중기 예보 화면 ────────────────────────────────────────────────────────────
@Composable
private fun MidTermWeatherContent(
    schedule : TeeOffSchedule,
    forecast : MidTermForecast?,
    score    : Int,
    onBack   : () -> Unit
) {
    val sky     = forecast?.skyConditionAm ?: SkyCondition.CLOUDY
    val dateFmt = DateTimeFormatter.ofPattern("MM/dd (E)")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 히어로 ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(skyGradient(sky))
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-40).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(sky.toEmoji(), fontSize = 56.sp)
                    Spacer(Modifier.height(4.dp))

                    val tempText = forecast?.let {
                        "${it.minTemperature.toInt()}° / ${it.maxTemperature.toInt()}°C"
                    } ?: "-- / --°C"

                    Text(
                        tempText,
                        color      = Color.White,
                        fontSize   = 40.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        sky.label,
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${schedule.golfCourse.name}  ·  ${schedule.date.format(dateFmt)}",
                        color    = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp
                    )
                }
            }

            // ── 바디 ─────────────────────────────────────────────────────────
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
                shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color           = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF546E7A).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "🗓 중기예보 (4~10일)",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize  = 12.sp,
                            color     = Color(0xFF37474F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 라운드 적합도
                    SuitabilityScoreCard(score = score)

                    // 오전 / 오후 예보
                    if (forecast != null) {
                        SectionTitle("오전 / 오후 예보")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MidTermHalfCard(
                                modifier  = Modifier.weight(1f),
                                label     = "오전",
                                emoji     = forecast.skyConditionAm.toEmoji(),
                                sky       = forecast.skyConditionAm.label,
                                rain      = forecast.precipProbAm
                            )
                            MidTermHalfCard(
                                modifier  = Modifier.weight(1f),
                                label     = "오후",
                                emoji     = forecast.skyConditionPm.toEmoji(),
                                sky       = forecast.skyConditionPm.label,
                                rain      = forecast.precipProbPm
                            )
                        }

                        // 기온 카드
                        SectionTitle("기온 범위")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Thermostat,
                                    null,
                                    tint     = Color(0xFFE57373),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "${forecast.minTemperature.toInt()}°C  ~  ${forecast.maxTemperature.toInt()}°C",
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "최저 / 최고 기온",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}

// ── 라운드 적합도 카드 ────────────────────────────────────────────────────────
@Composable
private fun SuitabilityScoreCard(score: Int) {
    val color     = scoreColor(score)
    val label     = scoreLabel(score)
    val animScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(800),
        label = "score"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "라운드 적합도",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = color
                    )
                }
                // 점수 서클
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$score",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = color
                    )
                }
            }
            // 진행 바
            LinearProgressIndicator(
                progress          = { animScore },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color             = color,
                trackColor        = color.copy(alpha = 0.15f),
                strokeCap         = StrokeCap.Round
            )
        }
    }
}

// ── 날씨 상세 그리드 ──────────────────────────────────────────────────────────
@Composable
private fun WeatherDetailGrid(
    windSpeed : Float,
    windDir   : Int,
    humidity  : Int,
    rain      : Int,
    teeTime   : String
) {
    val dirKor = WindDirectionConverter.toKorean(windDir)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailItem(
                modifier = Modifier.weight(1f),
                emoji    = "💨",
                label    = "풍속",
                value    = "${windSpeed}m/s",
                sub      = "$dirKor (${windDir}°)"
            )
            DetailItem(
                modifier = Modifier.weight(1f),
                emoji    = "💧",
                label    = "습도",
                value    = "${humidity}%",
                sub      = "상대습도"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailItem(
                modifier = Modifier.weight(1f),
                emoji    = "🌧",
                label    = "강수확률",
                value    = "${rain}%",
                sub      = if (rain < 30) "우산 불필요" else "우산 챙기세요"
            )
            DetailItem(
                modifier = Modifier.weight(1f),
                emoji    = "⏱",
                label    = "라운드 시간",
                value    = "",
                sub      = teeTime
            )
        }
    }
}

@Composable
private fun DetailItem(
    modifier : Modifier,
    emoji    : String,
    label    : String,
    value    : String,
    sub      : String
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (value.isNotEmpty()) {
                Text(
                    value,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── 중기 오전/오후 카드 ───────────────────────────────────────────────────────
@Composable
private fun MidTermHalfCard(
    modifier : Modifier,
    label    : String,
    emoji    : String,
    sky      : String,
    rain     : Int
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(emoji, fontSize = 32.sp)
            Text(
                sky,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "강수 $rain%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF42A5F5)
            )
        }
    }
}

// ── 섹션 타이틀 ──────────────────────────────────────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text       = text,
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface
    )
}

// ── 범위 초과 ─────────────────────────────────────────────────────────────────
@Composable
private fun OutOfRangeContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF546E7A), Color(0xFF37474F))))
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.EventBusy,
                null,
                modifier = Modifier.size(72.dp),
                tint     = Color.White.copy(alpha = 0.80f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "예보 불가",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "예보는 최대 10일 이내만 제공됩니다.\n날짜를 오늘부터 10일 이내로 선택해주세요.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// ── 오류 ─────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(
    message  : String,
    onBack   : () -> Unit,
    onRetry  : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                null,
                modifier = Modifier.size(72.dp),
                tint     = Color.White.copy(alpha = 0.80f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "날씨 정보를 불러올 수 없습니다",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    null,
                    tint = Color(0xFF1A237E)
                )
                Spacer(Modifier.width(6.dp))
                Text("다시 시도", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
            }
        }
    }
}
