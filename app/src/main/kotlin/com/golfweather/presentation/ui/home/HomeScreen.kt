package com.golfweather.presentation.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.golfweather.presentation.ui.components.GolfCourseSearchBar
import com.golfweather.presentation.viewmodel.HomeViewModel
import com.golfweather.presentation.viewmodel.SharedGolfCourseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // 에러 메시지 스낵바 표시
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // ── Material3 DatePickerDialog ────────────────────────────────────────
    // 과거 날짜만 비활성화, 미래 날짜는 제한 없음
    if (showDatePicker) {
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val initialMillis = uiState.selectedDate
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayMillis
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
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
                headline = null,
                showModeToggle = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GolfCourse,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("골프 날씨", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 골프장 선택 섹션 ────────────────────────────────────────────
            SectionCard(title = "골프장") {
                val course = uiState.selectedCourse
                if (course == null) {
                    // 미선택 상태: 콤보박스 검색
                    GolfCourseSearchBar(
                        query = uiState.searchQuery,
                        results = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        isDropdownExpanded = uiState.isDropdownExpanded,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        onCourseSelected = viewModel::onCourseSelected,
                        onDismiss = { viewModel.onDropdownExpandedChange(false) }
                    )
                } else {
                    // 선택된 상태: 코스 정보 + 변경 버튼
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GolfCourse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (course.address.isNotEmpty()) {
                                    Text(
                                        text = course.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (course.holeCount > 0) {
                                    Text(
                                        text = "${course.holeCount}홀",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // 변경 버튼
                            TextButton(onClick = { viewModel.clearSelectedCourse() }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("변경")
                            }
                        }
                    }
                }
            }

            // ── 날짜 선택 ────────────────────────────────────────────────────
            SectionCard(title = "티오프 날짜") {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        uiState.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)"))
                    )
                }
                Text(
                    text = "오늘 이후 날짜를 자유롭게 선택할 수 있습니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── 시간 선택 ────────────────────────────────────────────────────
            SectionCard(title = "티오프 시간") {
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.onTimeSelected(java.time.LocalTime.of(hour, minute))
                            },
                            uiState.selectedTime.hour,
                            uiState.selectedTime.minute,
                            true   // 24시간 형식
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        uiState.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                }
                Text(
                    text = "1분 단위로 입력 가능합니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── 날씨 확인 버튼 ───────────────────────────────────────────────
            Button(
                onClick = {
                    val schedule = viewModel.buildTeeOffSchedule() ?: return@Button
                    sharedViewModel.setSchedule(schedule)
                    onNavigateToWeather()
                },
                enabled = uiState.canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.WbSunny, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    "날씨 확인",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}
