package com.golfweather.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.golfweather.presentation.ui.home.HomeScreen
import com.golfweather.presentation.ui.weather.RadarScreen
import com.golfweather.presentation.ui.weather.WeatherScreen
import com.golfweather.presentation.viewmodel.SharedGolfCourseViewModel

object Routes {
    const val HOME    = "home"
    const val WEATHER = "weather"
    const val RADAR   = "radar"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Activity 생명주기를 공유하는 SharedViewModel – 두 화면이 같은 인스턴스를 참조
    val activity = LocalContext.current as ComponentActivity
    val sharedViewModel: SharedGolfCourseViewModel = hiltViewModel(activity)

    NavHost(
        navController    = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                sharedViewModel     = sharedViewModel,
                onNavigateToWeather = {
                    navController.navigate(Routes.WEATHER)
                }
            )
        }

        composable(Routes.WEATHER) {
            WeatherScreen(
                sharedViewModel   = sharedViewModel,
                onBack            = { navController.popBackStack() },
                onNavigateToRadar = { navController.navigate(Routes.RADAR) }
            )
        }

        composable(Routes.RADAR) {
            RadarScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
