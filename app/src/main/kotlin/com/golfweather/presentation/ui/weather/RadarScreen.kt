package com.golfweather.presentation.ui.weather

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class RadarRegion(val name: String, val url: String)

private val RADAR_REGIONS = listOf(
    RadarRegion("전국",   "https://www.kma.go.kr/repositary/image/rdr/img/rdr_main_img.gif"),
    RadarRegion("수도권", "https://www.kma.go.kr/repositary/image/rdr/img/rdr_ctr_img.gif"),
    RadarRegion("영남",   "https://www.kma.go.kr/repositary/image/rdr/img/rdr_se_img.gif"),
    RadarRegion("호남",   "https://www.kma.go.kr/repositary/image/rdr/img/rdr_sw_img.gif"),
)

private val BgDark = Color(0xFF0D1B2A)
private val AccentBlue = Color(0xFF90CAF9)

@Composable
fun RadarScreen(onBack: () -> Unit) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var reloadKey     by remember { mutableIntStateOf(0) }
    var isLoading     by remember { mutableStateOf(true) }
    var lastUpdated   by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
    }

    val currentUrl = RADAR_REGIONS[selectedIndex].url

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── 헤더 ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color.White)
            }
            Text(
                "기상 레이더",
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
                modifier   = Modifier.weight(1f)
            )
            IconButton(onClick = {
                isLoading = true
                reloadKey++
                lastUpdated = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            }) {
                Icon(Icons.Default.Refresh, "새로고침", tint = Color.White)
            }
        }

        // ── 지역 탭 ───────────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor   = BgDark,
            contentColor     = AccentBlue,
            edgePadding      = 8.dp
        ) {
            RADAR_REGIONS.forEachIndexed { i, region ->
                Tab(
                    selected = selectedIndex == i,
                    onClick  = {
                        selectedIndex = i
                        isLoading = true
                    },
                    text = {
                        Text(
                            region.name,
                            color      = if (selectedIndex == i) AccentBlue
                                         else Color.White.copy(alpha = 0.55f),
                            fontWeight = if (selectedIndex == i) FontWeight.SemiBold
                                         else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ── 레이더 이미지 WebView ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            key(currentUrl, reloadKey) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ) = false

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                            }
                            // baseUrl을 기상청 도메인으로 설정해 Referer 헤더 우회
                            loadDataWithBaseURL(
                                "https://www.kma.go.kr/",
                                """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { margin:0; padding:0; background:#0D1B2A;
         display:flex; justify-content:center; align-items:center;
         min-height:100vh; }
  img  { max-width:100%; max-height:100vh; object-fit:contain; }
</style>
</head>
<body>
  <img src="$currentUrl" alt="기상 레이더" />
</body>
</html>""",
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color    = AccentBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // ── 하단 안내 ─────────────────────────────────────────────────────────
        Text(
            "마지막 업데이트: $lastUpdated  ·  출처: 기상청",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            color    = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp
        )
    }
}
