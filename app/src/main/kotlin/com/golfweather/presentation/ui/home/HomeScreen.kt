package com.golfweather.presentation.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfweather.presentation.ui.components.GolfCourseSearchSheet
import com.golfweather.presentation.viewmodel.HomeViewModel
import com.golfweather.presentation.viewmodel.SharedGolfCourseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── 디자인 상수 ─────────────────────────────────────────────────────────────
private val GreenDark    = Color(0xFF1B5E20)
private val GreenMid     = Color(0xFF2E7D32)
private val GreenLight   = Color(0xFF388E3C)
private val BlueDark     = Color(0xFF1565C0)
private val HeroGradient = Brush.linearGradient(listOf(GreenDark, GreenMid, BlueDark))
private val BtnGradient  = Brush.horizontalGradient(listOf(GreenDark, GreenLight))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedViewModel: SharedGolfCourseViewModel,
    onNavigateToWeather: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // ── 골프장 검색 BottomSheet ──────────────────────────────────────────────
    if (uiState.showSearchSheet) {
        GolfCourseSearchSheet(
            query          = uiState.searchQuery,
            results        = uiState.searchResults,
            isSearching    = uiState.isSearching,
            searchError    = uiState.searchError,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onCourseSelected = viewModel::onCourseSelected,
            onDismiss      = viewModel::closeSearchSheet
        )
    }

    // ── 날짜 선택 다이얼로그 ─────────────────────────────────────────────────
    if (showDatePicker) {
        val today       = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val initMillis  = uiState.selectedDate
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onDateSelected(selected)
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        "티오프 날짜 선택",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                headline      = null,
                showModeToggle = false
            )
        }
    }

    // ── 루트 레이아웃 ────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── 히어로 영역 ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(HeroGradient)
            ) {
                // 장식용 원형 그라디언트 오브
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-30).dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 30.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // 앱 이름 + 날짜
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GolfCourse,
                            contentDescription = null,
                            tint   = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "골프 날씨",
                            color      = Color.White,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)")),
                        color    = Color.White.copy(alpha = 0.80f),
                        fontSize = 13.sp
                    )
                }
            }

            // ── 바디 카드 (히어로와 24dp 겹침) ─────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = (-24).dp),
                shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color  = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── 골프장 선택 ─────────────────────────────────────────
                    FormSectionLabel("골프장")
                    val course = uiState.selectedCourse

                    if (course == null) {
                        // 미선택 — 검색 트리거
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.openSearchSheet() }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color(0xFF1B5E20).copy(alpha = 0.10f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint   = GreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "골프장 검색",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "탭하여 골프장을 검색하세요",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 선택됨 — 코스 정보 카드
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape  = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1B5E20).copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(GreenDark, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.GolfCourse,
                                        contentDescription = null,
                                        tint   = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text       = course.name,
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = GreenDark
                                    )
                                    if (course.address.isNotEmpty()) {
                                        Text(
                                            text  = course.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (course.holeCount > 0) {
                                        Text(
                                            text  = "${course.holeCount}홀",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.openSearchSheet() }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "골프장 변경",
                                        tint = GreenDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── 날짜 / 시간 (2열) ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DateTimeCard(
                            modifier  = Modifier.weight(1f),
                            icon      = Icons.Default.CalendarToday,
                            label     = "티오프 날짜",
                            value     = uiState.selectedDate.format(
                                DateTimeFormatter.ofPattern("MM/dd (E)")
                            ),
                            onClick   = { showDatePicker = true }
                        )
                        DateTimeCard(
                            modifier  = Modifier.weight(1f),
                            icon      = Icons.Default.AccessTime,
                            label     = "티오프 시간",
                            value     = uiState.selectedTime.format(
                                DateTimeFormatter.ofPattern("HH:mm")
                            ),
                            onClick   = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        viewModel.onTimeSelected(
                                            java.time.LocalTime.of(hour, minute)
                                        )
                                    },
                                    uiState.selectedTime.hour,
                                    uiState.selectedTime.minute,
                                    true
                                ).show()
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── 날씨 확인 CTA 버튼 ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (uiState.canProceed)
                                    Modifier.background(BtnGradient)
                                else
                                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            .clickable(enabled = uiState.canProceed) {
                                val schedule = viewModel.buildTeeOffSchedule() ?: return@clickable
                                sharedViewModel.setSchedule(schedule)
                                onNavigateToWeather()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = null,
                                tint     = if (uiState.canProceed) Color.White
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "날씨 확인하기",
                                color      = if (uiState.canProceed) Color.White
                                             else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Surface의 offset(-24dp) 때문에 생기는 하단 여백 보정
            Spacer(Modifier.height(0.dp))
        }

        // ── 스낵바 ────────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── 섹션 레이블 ──────────────────────────────────────────────────────────────
@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp
    )
}

// ── 날짜/시간 선택 카드 ──────────────────────────────────────────────────────
@Composable
private fun DateTimeCard(
    modifier : Modifier,
    icon     : ImageVector,
    label    : String,
    value    : String,
    onClick  : () -> Unit
) {
    Card(
        modifier  = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = GreenDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
