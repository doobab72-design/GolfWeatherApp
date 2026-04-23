package com.golfweather.presentation.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import kotlin.math.abs

private val WheelGreen = Color(0xFF1B5E20)
private val ITEM_H: Dp = 54.dp   // 아이템 1개 높이
private const val VISIBLE = 5    // 보이는 아이템 수 (홀수여야 중앙 선택 가능)
private const val REPEAT  = 300  // 무한 스크롤 효과용 반복 수

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelTimePickerSheet(
    initialTime    : LocalTime,
    onTimeSelected : (LocalTime) -> Unit,
    onDismiss      : () -> Unit
) {
    var selectedHour   by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = null
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 타이틀 ─────────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Text(
                "티오프 시간",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1C19)
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))

            Spacer(Modifier.height(8.dp))

            // ── 드럼롤 영역 ────────────────────────────────────────────────
            // contentPadding 없이 높이만 고정 → 중앙 = firstVisible + 2
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(ITEM_H * VISIBLE),
                contentAlignment = Alignment.Center
            ) {
                // 선택 하이라이트 바 (가장 아래 레이어)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .fillMaxWidth()
                        .height(ITEM_H)
                        .background(
                            WheelGreen.copy(alpha = 0.08f),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.5.dp,
                            WheelGreen.copy(alpha = 0.22f),
                            RoundedCornerShape(14.dp)
                        )
                )

                // 시·분 컬럼
                Row(
                    modifier          = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    WheelColumn(
                        count          = 24,
                        initialIndex   = initialTime.hour,
                        label          = { "%02d".format(it) },
                        onIndexChanged = { selectedHour = it },
                        modifier       = Modifier.width(110.dp)
                    )
                    Text(
                        ":",
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color      = WheelGreen,
                        modifier   = Modifier.padding(horizontal = 6.dp)
                    )
                    WheelColumn(
                        count          = 60,
                        initialIndex   = initialTime.minute,
                        label          = { "%02d".format(it) },
                        onIndexChanged = { selectedMinute = it },
                        modifier       = Modifier.width(110.dp)
                    )
                }

                // 위아래 페이드 오버레이 (가장 위 레이어)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.White,
                                0.20f to Color.Transparent,
                                0.80f to Color.Transparent,
                                1.00f to Color.White
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))

            // ── 하단 버튼 ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        "취소",
                        color      = Color(0xFF6B7280),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick  = {
                        onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                        .background(WheelGreen, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        "확인",
                        color      = Color.White,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── 드럼롤 단일 컬럼 ─────────────────────────────────────────────────────────
// contentPadding 없이 초기 스크롤 위치를 (startAbs - midOffset) 으로 설정하면
// → 가장 위에 보이는 아이템 = firstVisibleItemIndex
// → 중앙 아이템  = firstVisibleItemIndex + midOffset  ← 하이라이트와 정확히 일치
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    count          : Int,
    initialIndex   : Int,
    label          : (Int) -> String,
    onIndexChanged : (Int) -> Unit,
    modifier       : Modifier = Modifier
) {
    val midOffset = VISIBLE / 2                          // = 2
    val startAbs  = initialIndex + (REPEAT / 2) * count // 리스트 한가운데쯤에서 시작

    // 초기 위치: 선택 아이템이 가운데 오도록 midOffset만큼 위쪽 아이템부터 시작
    val listState     = rememberLazyListState(startAbs - midOffset)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    // 중앙 아이템의 절대 인덱스 = 첫 번째 보이는 아이템 + midOffset
    val selectedAbs by remember {
        derivedStateOf { listState.firstVisibleItemIndex + midOffset }
    }

    // 스크롤이 멈추면 선택값 콜백
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling) {
        if (!isScrolling) {
            onIndexChanged(selectedAbs % count)
        }
    }

    // contentPadding 제거 → 아이템이 y=0 부터 시작 → 중앙 = firstVisible + 2
    LazyColumn(
        state         = listState,
        flingBehavior = flingBehavior,
        modifier      = modifier.height(ITEM_H * VISIBLE)
    ) {
        items(count * REPEAT) { i ->
            val dist = abs(i - selectedAbs)

            val alpha  = when (dist) { 0 -> 1f;  1 -> 0.50f; else -> 0.20f }
            val tsp    = when (dist) { 0 -> 28.sp; 1 -> 21.sp; else -> 17.sp }
            val weight = if (dist == 0) FontWeight.Bold else FontWeight.Normal
            val color  = if (dist == 0) WheelGreen else Color(0xFF9E9E9E)

            Box(
                modifier         = Modifier
                    .height(ITEM_H)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label(i % count),
                    fontSize   = tsp,
                    fontWeight = weight,
                    color      = color,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.alpha(alpha)
                )
            }
        }
    }
}
