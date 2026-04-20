# GolfWeatherApp Code Review Report
**Date:** 2026-04-13
**Reviewer:** Claude Code Review Agent

---

## Executive Summary

The codebase is well-structured and follows Clean Architecture principles reasonably well. The navigation layer already uses a SharedViewModel pattern with activity scope (no float NavArgument issue). The Lambert formula in CoordConverter is mathematically correct. The most impactful bugs are in the time calculation helpers in WeatherRepository: `calcBaseDateTime()` used fragile arithmetic that produced a negative `effectiveHour` at midnight, and `calcMidTermTmFc()` had a genuine logic error — between midnight and 06:00 it returned today's unpublished 0600 base time instead of yesterday's 1800. The MidTermRegionMapper had a region boundary overlap bug causing 서귀포 to never match. Three bugs were fixed directly in the source files.

---

## Critical Bugs Fixed

### 1. CoordConverter — Lambert Formula
- **File:** `app/src/main/kotlin/com/golfweather/util/CoordConverter.kt`
- **Bug:** No bug. The formula matches the KMA official algorithm exactly. Constants (RE, GRID, SLAT1, SLAT2, OLON, OLAT, XO, YO), and the sn/sf/ro/ra derivation are all correct. Kotlin's `kotlin.math.*` import provides the same `Double`-precision math as `java.lang.Math`, so no precision loss from missing `Math.` prefix.
- **Fix:** No change required.

### 2. NavGraph Float Precision Loss
- **File:** `app/src/main/kotlin/com/golfweather/navigation/AppNavGraph.kt`
- **Bug:** No bug. The app already uses activity-scoped `SharedGolfCourseViewModel` (via `hiltViewModel(activity)`) to pass `TeeOffSchedule` (which contains `Double` lat/lon) between HomeScreen and WeatherScreen. No `NavArgument` with Float type is used anywhere in the route definitions.
- **Fix:** No change required.

### 3. calcBaseDateTime() — Midnight Edge Case
- **File:** `app/src/main/kotlin/com/golfweather/data/repository/WeatherRepository.kt` — `calcBaseDateTime()` (line ~202)
- **Bug (original):** `effectiveHour = if (currentMinute >= 10) currentHour else currentHour - 1`. At 00:00–00:09, this produced `effectiveHour = -1`. While `lastOrNull { it <= -1 }` happened to return null (triggering yesterday's 23:00 fallback correctly), the logic was semantically wrong and fragile: the `currentHour - 1` arithmetic could cause incorrect behavior if hour-based comparisons were ever extended. More importantly, the `10-minute grace period` check was comparing hours rather than total minutes, making the boundary at each base time imprecise (e.g., at 02:05 the code subtracted 1 from the hour rather than checking that 2*60+5 < 2*60+10).
- **Fix:** Rewrote using total-minutes arithmetic: `effectiveMinutes = currentHour * 60 + currentMinute`, then `lastAvailableBase = baseTimes.lastOrNull { it * 60 + 10 <= effectiveMinutes }`. This cleanly handles all edge cases — midnight, just-before-publication windows, and normal hours — with no negative-number risk. Changed visibility to `internal` for testability.

### 4. calcMidTermTmFc() — Midnight-to-6AM Edge Case (Critical)
- **File:** `app/src/main/kotlin/com/golfweather/data/repository/WeatherRepository.kt` — `calcMidTermTmFc()` (line ~233)
- **Bug:** Original code: `if (now.hour >= 18) "${dateStr}1800" else "${dateStr}0600"`. Between 00:00 and 05:59, this returned today's `yyyyMMdd0600` — a base time that has not been published yet (KMA publishes the 0600 mid-term forecast at 06:00, and it takes additional minutes to become available). This would result in an HTTP error or empty response from the KMA API during the early morning hours.
- **Fix:** Added a third branch: `else → yesterday's date + "1800"`. New logic: `hour >= 18 → today1800`, `hour >= 6 → today0600`, `else → yesterday1800`. Changed visibility to `internal` for testability.

---

## Important Issues Fixed

### 5. MidTermRegionMapper — 서귀포 Never Matched (Region Boundary Overlap)
- **File:** `app/src/main/kotlin/com/golfweather/util/MidTermRegionMapper.kt`
- **Bug:** The `제주` region was defined as `lat: 33.1–33.6, lon: 126.1–126.9` and `서귀포` as `lat: 33.1–33.6, lon: 126.3–127.0`. Both regions had identical latitude ranges and overlapping longitude ranges. Since `findRegion()` uses `firstOrNull`, `제주` was always matched first for any coordinate in Jeju island, meaning `서귀포`'s temperature point code (`11G00401`) was never returned. This caused slightly less accurate temperature forecasts for the southern half of Jeju island (both use the same land region `11G00000`, so precipitation forecasts were unaffected).
- **Fix:** Split by latitude instead of longitude. Jeju City center is ~33.50°N, Seogwipo center is ~33.25°N. Using 33.35° as the dividing latitude: `제주 → lat 33.35–33.6`, `서귀포 → lat 33.1–33.35`.

### 6. MidTermRegionMapper — 경기 남부 Representative Point Missing
- **File:** `app/src/main/kotlin/com/golfweather/util/MidTermRegionMapper.kt`
- **Bug:** The entire 서울·인천·경기 region (land code `11B00000`) used Seoul (`11B10001`) as the temperature representative point, including for golf courses in southern Gyeonggi (e.g., Suwon, Icheon, Osan). Southern Gyeonggi temperatures can differ meaningfully from Seoul.
- **Fix:** Added a `경기남부` sub-region (lat 36.9–37.4, lon 126.7–127.6) that maps to Suwon (`11B20601`) as the temperature point, placed before the Seoul/북부경기 entry in the priority list.

### 7. OkHttp Write Timeout Missing
- **File:** `app/src/main/kotlin/com/golfweather/di/NetworkModule.kt`
- **Bug:** `OkHttpClient` had `connectTimeout` and `readTimeout` configured (30s each) but no `writeTimeout`. Although GET requests rarely encounter write timeout issues, POST requests (if ever added) and certain proxy/network scenarios could hang indefinitely.
- **Fix:** Added `.writeTimeout(30, TimeUnit.SECONDS)`.

---

## Minor Issues Not Fixed (Requires Manual Attention)

### A. HttpLoggingInterceptor Level in Release Build
- **File:** `NetworkModule.kt`
- **Issue:** `HttpLoggingInterceptor.Level.BODY` logs full request/response bodies including the API key in query parameters. This is acceptable in debug but should be `NONE` or `BASIC` in release.
- **Recommendation:** Gate the logging level on `BuildConfig.DEBUG`:
  ```kotlin
  level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
  ```

### B. No Retry Logic for Transient Network Failures
- **File:** `WeatherRepository.kt`, `GolfCourseRepository.kt`
- **Issue:** A single transient network failure (timeout, DNS failure) causes the entire `Result.failure()` to propagate. The UI shows an error with a retry button, but automatic retry with backoff (e.g., OkHttp `Interceptor` or Kotlin retry operator) would improve resilience.
- **Recommendation:** Add an OkHttp retry interceptor or use Kotlin's `retryWhen` operator on coroutine flows.

### C. GolfCourse is Both a Room Entity and a UI Model
- **File:** `data/model/GolfCourse.kt`
- **Issue:** `@Entity` annotation is on the same class used throughout the presentation layer. This violates Clean Architecture's layer separation — changes to DB schema (column names, indices) can ripple into domain/UI code.
- **Recommendation:** Create separate `GolfCourseEntity` (data layer) and `GolfCourse` domain model, with a mapper between them.

### D. Domain UseCase References `data.model` Directly
- **File:** `domain/usecase/GetWeatherForecastUseCase.kt`, `SearchGolfCourseUseCase.kt`
- **Issue:** Use cases import `com.golfweather.data.model.TeeOffSchedule`, `WeatherForecast`, etc. Ideally the domain layer should define its own model classes independent of the data layer.
- **Recommendation:** Move `TeeOffSchedule`, `WeatherForecast`, `MidTermForecast`, `GolfCourse` to a `domain/model` package and have the data layer map to/from them.

### E. Compose Recomposition — `remember {}` Missing on Lambda Callbacks
- **File:** `HomeScreen.kt`
- **Issue:** Lambdas passed to `GolfCourseSearchBar` (e.g., `viewModel::onSearchQueryChanged`, `viewModel::onCourseSelected`) are method references that create new lambda objects on each recomposition. For simple composables this is not critical, but wrapping in `remember` prevents unnecessary recomposition of child composables that take these as parameters.
- **Recommendation:** Wrap in `remember`:
  ```kotlin
  val onQueryChanged = remember { { q: String -> viewModel.onSearchQueryChanged(q) } }
  ```

### F. `SkyCondition.PARTLY_CLOUDY` Label Mismatch
- **File:** `data/model/WeatherForecast.kt`
- **Issue:** KMA SKY code 3 is officially "구름많음" (mostly cloudy), but `PARTLY_CLOUDY.label` is set to `"구름조금"` (partly cloudy). The `from()` mapping is correct (code 3 → `PARTLY_CLOUDY`), but the human-readable label shown in the UI is misleading.
- **Recommendation:** Change `PARTLY_CLOUDY("구름조금")` to `PARTLY_CLOUDY("구름많음")`.

### G. `RoundWeatherTimeline` Composable Contains Nested `LazyColumn` (Crash Risk)
- **File:** `presentation/ui/components/RoundWeatherTimeline.kt`
- **Issue:** `RoundWeatherTimeline` is a `Column` that contains a `LazyColumn`. If `RoundWeatherTimeline` is ever placed inside a `LazyColumn` or `ScrollableColumn`, this causes a nested infinite-height layout crash at runtime. It is not currently triggered because `WeatherScreen` uses its own `LazyColumn` and does not call `RoundWeatherTimeline`, but the component is dangerous to reuse.
- **Recommendation:** Remove `LazyColumn` from `RoundWeatherTimeline` and instead accept a `LazyListScope` or just render items directly.

### H. `calcBaseDateTime()` Does Not Account for Network Delay
- **File:** `WeatherRepository.kt`
- **Issue:** KMA documentation states data is available approximately 10 minutes after the base time, but in practice it can take up to 15–20 minutes during peak load. The current 10-minute buffer is the minimum; a more conservative 12–15 minute buffer would reduce the chance of receiving an empty response.
- **Recommendation:** Consider using 12 minutes: `it * 60 + 12 <= effectiveMinutes`.

---

## Architecture Assessment

| Area | Status | Notes |
|------|--------|-------|
| Presentation layer | Good | ViewModels use `viewModelScope`; no Activity references |
| Domain layer | Acceptable | Use cases are thin wrappers; no Android framework imports, but directly reference `data.model` classes |
| Data layer | Good | Repository pattern with Room caching and API fallback |
| DI (Hilt) | Good | Proper scoping with `@Singleton` and `@HiltViewModel` |
| Navigation | Good | Activity-scoped SharedViewModel avoids Float NavArgument precision loss |
| API key security | Good | Keys stored in `local.properties` (gitignored), loaded via BuildConfig, validated at runtime |
| Memory leaks | None found | No Context references in ViewModels; coroutines use `viewModelScope` or `Dispatchers.IO` |

---

## Testing Notes for QA

### Test Cases to Verify

1. **calcBaseDateTime() edge cases (critical)**
   - Set device time to 00:05 → should use **yesterday's 2300** base
   - Set device time to 02:09 → should use **yesterday's 2300** base
   - Set device time to 02:10 → should use **today's 0200** base
   - Set device time to 02:15 → should use **today's 0200** base
   - Set device time to 05:09 → should use **today's 0200** base
   - Set device time to 23:05 → should use **today's 2000** base (not 2300; 2300 not published yet)
   - Set device time to 23:10 → should use **today's 2300** base

2. **calcMidTermTmFc() midnight edge cases (critical)**
   - Set device time to 03:00 → mid-term tmFc should be **yesterday's date + 1800**
   - Set device time to 05:59 → mid-term tmFc should be **yesterday's date + 1800**
   - Set device time to 06:00 → mid-term tmFc should be **today's date + 0600**
   - Set device time to 17:59 → mid-term tmFc should be **today's date + 0600**
   - Set device time to 18:00 → mid-term tmFc should be **today's date + 1800**

3. **Jeju/Seogwipo forecast differentiation**
   - Search for golf course in Seogwipo area (lat ~33.25°N) → temperature forecast should use Seogwipo representative point
   - Search for golf course in Jeju City area (lat ~33.50°N) → temperature forecast should use Jeju representative point
   - Verify both return the same precipitation forecast (same land region `11G00000`)

4. **경기 남부 region mapping**
   - Search for Suwon/Icheon/Osan area golf course → mid-term temperature should use Suwon (`11B20601`) representative point, not Seoul

5. **Weather screen navigation precision**
   - Select a golf course with precise coordinates (e.g., 37.123456°N) → navigate to WeatherScreen → verify the correct grid point (nx, ny) is used (no float truncation)

6. **Short-term forecast time filtering**
   - Tee-off at 22:00 → verify forecasts wrap midnight correctly (22:00, 23:00, 00:00, 01:00, 02:00)

### Known Edge Cases

- **Island golf courses** (Jeju, coastal islands with lon < 125.8° or > 129.6°): fall through all region checks; default fallback is Seoul/경기 which will give wrong mid-term temperature forecasts. Manual intervention needed for these locations.
- **DMZ / northernmost courses** (lat > 38.3°): similarly fall through the region table.
- **API response with 0 items**: `parseShortTermForecast()` returns empty list; UI correctly shows "해당 시간대 예보 데이터가 없습니다" message — verified.
- **KMA API 429 / rate limit**: No handling. Will surface as a generic error message. Consider adding `429` specific user messaging.
