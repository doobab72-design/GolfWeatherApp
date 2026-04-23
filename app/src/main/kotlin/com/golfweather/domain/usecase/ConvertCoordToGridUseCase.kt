package com.golfweather.domain.usecase

import com.golfweather.util.CoordConverter
import com.golfweather.util.KmaGridPoint
import javax.inject.Inject

/**
 * 위도/경도 → 기상청 격자(nx, ny) 변환 유스케이스
 */
class ConvertCoordToGridUseCase @Inject constructor() {
    operator fun invoke(latitude: Double, longitude: Double): KmaGridPoint {
        return CoordConverter.toGrid(latitude, longitude)
    }
}
