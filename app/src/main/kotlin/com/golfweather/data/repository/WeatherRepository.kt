package com.golfweather.data.repository

import com.golfweather.data.api.MidLandItem
import com.golfweather.data.api.MidTaItem
import com.golfweather.data.api.VilageFcstItem
import com.golfweather.data.api.WeatherApiService
import com.golfweather.data.model.MidTermForecast
import com.golfweather.data.model.PrecipitationType
import com.golfweather.data.model.SkyCondition
import com.golfweather.data.model.WeatherForecast
import com.golfweather.util.ApiKeyValidator
import com.golfweather.util.CoordConverter
import com.golfweather.util.KmaGridPoint
import com.golfweather.util.MidTermRegionMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService
) {
    /**
     * 단기예보 조회 (오늘~3일)
     * 요청 시각에 따라 가장 가까운 발표 시각을 계산
     */
    suspend fun getShortTermForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate,
        targetTime: LocalTime
    ): Result<List<WeatherForecast>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!ApiKeyValidator.isKmaKeySet()) {
                error("기상청 API 키가 설정되지 않았습니다. app/build.gradle.kts의 KMA_API_KEY를 확인하세요.")
            }
            val grid: KmaGridPoint = CoordConverter.toGrid(latitude, longitude)
            val now = LocalDateTime.now()
            val baseDateTime = calcBaseDateTime(now)
            val baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HHmm"))

            val response = weatherApi.getShortTermForecast(
                baseDate = baseDate,
                baseTime = baseTime,
                nx = grid.nx,
                ny = grid.ny
            )

            val items = response.response?.body?.items?.item ?: emptyList()
            parseShortTermForecast(items, targetDate, targetTime)
        }
    }

    /**
     * 중기예보 조회 (4~10일)
     */
    suspend fun getMidTermForecast(
        latitude: Double,
        longitude: Double,
        targetDate: LocalDate
    ): Result<List<MidTermForecast>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!ApiKeyValidator.isKmaKeySet()) {
                error("기상청 API 키가 설정되지 않았습니다. app/build.gradle.kts의 KMA_API_KEY를 확인하세요.")
            }
            val now = LocalDateTime.now()
            val tmFc = calcMidTermTmFc(now)
            val landRegId = MidTermRegionMapper.getLandRegionId(latitude, longitude)
            val taRegId = MidTermRegionMapper.getTaRegionId(latitude, longitude)

            val landResponse = weatherApi.getMidTermLandForecast(regId = landRegId, tmFc = tmFc)
            val taResponse = weatherApi.getMidTermTemperature(regId = taRegId, tmFc = tmFc)

            val landItem = landResponse.response?.body?.items?.item?.firstOrNull()
            val taItem = taResponse.response?.body?.items?.item?.firstOrNull()

            parseMidTermForecast(landItem, taItem, now.toLocalDate())
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Parsing helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 단기예보 아이템 리스트 → 시간별 WeatherForecast 변환
     * 티오프 시각부터 4시간 동안만 필터링
     */
    private fun parseShortTermForecast(
        items: List<VilageFcstItem>,
        targetDate: LocalDate,
        targetTime: LocalTime
    ): List<WeatherForecast> {
        val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val startHour = targetTime.hour
        val endHour = targetTime.plusHours(4).hour

        // 시간대별 카테고리 맵 구성: fcstDate+fcstTime → (category → value)
        val groupedByTime = items
            .filter { it.fcstDate == targetDateStr }
            .groupBy { "${it.fcstDate}${it.fcstTime}" }

        return groupedByTime.entries
            .sortedBy { it.key }
            .filter { entry ->
                val hour = entry.key.substring(8, 10).toIntOrNull() ?: 0
                if (endHour >= startHour) hour in startHour..endHour
                else hour >= startHour || hour <= endHour // 자정 넘어가는 경우
            }
            .mapNotNull { (dateTime, timeItems) ->
                val catMap = timeItems.associate { it.category to it.fcstValue }
                val skyCode = catMap["SKY"]?.toIntOrNull() ?: 1
                val ptyCode = catMap["PTY"]?.toIntOrNull() ?: 0
                WeatherForecast(
                    dateTime = dateTime,
                    temperature = catMap["TMP"]?.toFloatOrNull() ?: 0f,
                    precipitationProbability = catMap["POP"]?.toIntOrNull() ?: 0,
                    windSpeed = catMap["WSD"]?.toFloatOrNull() ?: 0f,
                    windDirection = catMap["VEC"]?.toIntOrNull() ?: 0,
                    humidity = catMap["REH"]?.toIntOrNull() ?: 0,
                    skyCondition = SkyCondition.from(skyCode, ptyCode),
                    precipitationType = when (ptyCode) {
                        1 -> PrecipitationType.RAIN
                        2 -> PrecipitationType.RAIN_SNOW
                        3 -> PrecipitationType.SNOW
                        4 -> PrecipitationType.SHOWER
                        else -> PrecipitationType.NONE
                    }
                )
            }
    }

    /**
     * 중기예보 아이템 → MidTermForecast 리스트 변환
     */
    private fun parseMidTermForecast(
        landItem: MidLandItem?,
        taItem: MidTaItem?,
        today: LocalDate
    ): List<MidTermForecast> {
        val forecasts = mutableListOf<MidTermForecast>()
        for (day in 3..10) {
            val date = today.plusDays(day.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            val minTemp = when (day) {
                3 -> taItem?.taMin3; 4 -> taItem?.taMin4; 5 -> taItem?.taMin5
                6 -> taItem?.taMin6; 7 -> taItem?.taMin7; 8 -> taItem?.taMin8
                9 -> taItem?.taMin9; 10 -> taItem?.taMin10; else -> null
            }
            val maxTemp = when (day) {
                3 -> taItem?.taMax3; 4 -> taItem?.taMax4; 5 -> taItem?.taMax5
                6 -> taItem?.taMax6; 7 -> taItem?.taMax7; 8 -> taItem?.taMax8
                9 -> taItem?.taMax9; 10 -> taItem?.taMax10; else -> null
            }

            // 기상청 중기육상예보 필드 구조:
            //  3~7일: wfNAm / wfNPm (오전/오후 구분), rnStNAm / rnStNPm
            //  8~10일: wfN (단일), rnStN (단일) - Am/Pm 구분 없음
            val (amSky, pmSky, amPop, pmPop) = when (day) {
                3 -> Quadruple(landItem?.wf3Am, landItem?.wf3Pm, landItem?.rnSt3Am, landItem?.rnSt3Pm)
                4 -> Quadruple(landItem?.wf4Am, landItem?.wf4Pm, landItem?.rnSt4Am, landItem?.rnSt4Pm)
                5 -> Quadruple(landItem?.wf5Am, landItem?.wf5Pm, landItem?.rnSt5Am, landItem?.rnSt5Pm)
                6 -> Quadruple(landItem?.wf6Am, landItem?.wf6Pm, landItem?.rnSt6Am, landItem?.rnSt6Pm)
                7 -> Quadruple(landItem?.wf7Am, landItem?.wf7Pm, landItem?.rnSt7Am, landItem?.rnSt7Pm)
                // 8~10일: 단일 값을 Am/Pm 모두에 적용
                8 -> Quadruple(landItem?.wf8, landItem?.wf8, landItem?.rnSt8, landItem?.rnSt8)
                9 -> Quadruple(landItem?.wf9, landItem?.wf9, landItem?.rnSt9, landItem?.rnSt9)
                10 -> Quadruple(landItem?.wf10, landItem?.wf10, landItem?.rnSt10, landItem?.rnSt10)
                else -> Quadruple(null, null, null, null)
            }

            forecasts.add(
                MidTermForecast(
                    date = dateStr,
                    minTemperature = minTemp?.toFloat() ?: 0f,
                    maxTemperature = maxTemp?.toFloat() ?: 0f,
                    skyConditionAm = wfCodeToSkyCondition(amSky),
                    skyConditionPm = wfCodeToSkyCondition(pmSky),
                    precipProbAm = amPop ?: 0,
                    precipProbPm = pmPop ?: 0
                )
            )
        }
        return forecasts
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Time calculation helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 현재 시각으로부터 가장 최근 단기예보 발표 시각 계산
     * 발표 시각: 02, 05, 08, 11, 14, 17, 20, 23시 (발표 후 약 10분 뒤 제공)
     *
     * 엣지 케이스:
     *  - 00:00~02:09: 데이터 미발표 구간 → 전날 23시 기준
     *  - 각 발표시각 +0~9분: 아직 데이터 업로드 중 → 이전 발표시각 기준
     */
    internal fun calcBaseDateTime(now: LocalDateTime): LocalDateTime {
        val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)

        // 00:00~00:09 처리: currentHour=0, minute<10 → effectiveHour=-1 방지
        // 대신 "전날 23시" 폴백으로 직접 처리
        val currentHour = now.hour
        val currentMinute = now.minute

        // 데이터 제공 기준: 발표시각 + 10분 이후에야 안정적으로 사용 가능
        // effectiveHour: 현재 시각에서 유효한 마지막 발표 시간 (정각 기준)
        val effectiveMinutes = currentHour * 60 + currentMinute

        // 각 base time의 "데이터 제공 시작" 시각(분 단위): baseHour*60 + 10
        val lastAvailableBase = baseTimes.lastOrNull { it * 60 + 10 <= effectiveMinutes }

        return when {
            lastAvailableBase != null -> {
                // 오늘 해당 발표 시각
                now.toLocalDate().atTime(lastAvailableBase, 0)
            }
            else -> {
                // 00:00~02:09 구간: 전날 23시 기준
                now.toLocalDate().minusDays(1).atTime(23, 0)
            }
        }
    }

    /**
     * 중기예보 기준시각 (tmFc): 오전 6시 또는 18시 발표
     *
     * 엣지 케이스:
     *  - 00:00~06:00: 오늘 0600 미발표 → 전날 1800 기준
     *  - 06:00~18:00: 오늘 0600 기준
     *  - 18:00~: 오늘 1800 기준
     */
    internal fun calcMidTermTmFc(now: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return when {
            now.hour >= 18 -> "${now.format(formatter)}1800"
            now.hour >= 6  -> "${now.format(formatter)}0600"
            else           -> "${now.toLocalDate().minusDays(1).format(formatter)}1800" // 자정~06시: 전날 18시
        }
    }

    private fun wfCodeToSkyCondition(code: String?): SkyCondition = when {
        code == null -> SkyCondition.CLEAR
        code.contains("맑") -> SkyCondition.CLEAR
        code.contains("구름많") -> SkyCondition.PARTLY_CLOUDY
        code.contains("흐") -> SkyCondition.CLOUDY
        code.contains("비") && code.contains("눈") -> SkyCondition.RAIN_SNOW
        code.contains("비") -> SkyCondition.RAIN
        code.contains("눈") -> SkyCondition.SNOW
        else -> SkyCondition.CLEAR
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
