package com.foxlab.procrastinationtracker.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/**
 * Same policy as the phone: nothing is asked at launch. Tracking runs in the foreground service
 * either way, and the pre-configured Timer asks only when the user starts a block that has to
 * alert (see [TimerScreen]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProcrastinationTrackerWatchTheme {
                WatchApp()
            }
        }
    }
}

@Composable
private fun WatchApp() {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenTimer = { navController.navigate("timer") },
                onOpenTracker = { navController.navigate("tracker") }
            )
        }
        composable("timer") {
            TimerScreen(onOpenModeSelect = { navController.navigate("mode") })
        }
        composable("mode") {
            ModeSelectScreen(onModeChosen = { navController.popBackStack() })
        }
        composable("tracker") {
            TrackerScreen(
                onOpenProfiles = { navController.navigate("tracker_profiles") },
                onOpenSummary = { navController.navigate("tracker_summary") }
            )
        }
        composable("tracker_profiles") {
            TrackerProfileSelectScreen(onProfileChosen = { navController.popBackStack() })
        }
        composable("tracker_summary") {
            TrackerSummaryScreen()
        }
    }
}
