package com.foxlab.procrastinationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foxlab.procrastinationtracker.ui.HistoryScreen
import com.foxlab.procrastinationtracker.ui.HomeScreen
import com.foxlab.procrastinationtracker.ui.TimerScreen
import com.foxlab.procrastinationtracker.ui.TrackerScreen
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette
import com.foxlab.procrastinationtracker.ui.theme.ProcrastinationTrackerTheme

/**
 * No permission is asked here on purpose: tracking works without notifications, so the prompt
 * only shows up where it buys the user something (see [rememberNotificationPermission]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app is dark-canvas everywhere, so the system bars belong to it too.
        window.statusBarColor = BoardPalette.CanvasTop.toArgb()
        window.navigationBarColor = BoardPalette.Canvas.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            ProcrastinationTrackerTheme {
                RootApp()
            }
        }
    }
}

private object Route {
    const val HOME = "home"
    const val TIMER_MODE = "timer_mode"
    const val TRACKER_MODE = "tracker_mode"
}

@Composable
private fun RootApp() {
    val navController = rememberNavController()
    // Jumps straight to Home regardless of how deep the current screen's own back stack is.
    val goHome: () -> Unit = { navController.popBackStack(Route.HOME, inclusive = false) }

    NavHost(navController = navController, startDestination = Route.HOME) {
        composable(Route.HOME) {
            HomeScreen(
                onOpenTimer = { navController.navigate(Route.TIMER_MODE) },
                onOpenTracker = { navController.navigate(Route.TRACKER_MODE) }
            )
        }
        composable(Route.TIMER_MODE) { TimerModeApp(onGoHome = goHome) }
        composable(Route.TRACKER_MODE) { TrackerScreen(onGoHome = goHome) }
    }
}

private enum class TimerDest(val label: String) {
    Timer("Timer"),
    History("Histórico")
}

/**
 * The original Spec 001 experience: 52/17 / Pomodoro / 45/15 countdown + history, unchanged.
 *
 * This used to be a second, nested NavHost for the Timer/Histórico tabs. That relied on Compose
 * Navigation's own automatic back-press handling correctly out-prioritizing the *outer* NavHost's
 * (RootApp's) automatic handling -- which in practice was not reliable here: pressing back from
 * Histórico jumped straight to Home instead of landing on Timer first. Plain local state plus a
 * single explicit [BackHandler] (the same mechanism already used successfully for the Tracker
 * back-menu) makes the two-level behavior deterministic instead of depending on that ordering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerModeApp(onGoHome: () -> Unit) {
    var currentTab by remember { mutableStateOf(TimerDest.Timer) }

    BackHandler {
        if (currentTab == TimerDest.History) currentTab = TimerDest.Timer else onGoHome()
    }

    Scaffold(
        containerColor = BoardPalette.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Modo Timer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BoardPalette.CanvasTop,
                    titleContentColor = BoardPalette.Ink,
                    navigationIconContentColor = BoardPalette.InkMuted
                ),
                navigationIcon = {
                    IconButton(onClick = onGoHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Início")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BoardPalette.CanvasTop) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BoardPalette.Blue,
                    selectedTextColor = BoardPalette.Blue,
                    indicatorColor = BoardPalette.Blue.copy(alpha = 0.16f),
                    unselectedIconColor = BoardPalette.InkMuted,
                    unselectedTextColor = BoardPalette.InkMuted
                )
                NavigationBarItem(
                    selected = currentTab == TimerDest.Timer,
                    onClick = { currentTab = TimerDest.Timer },
                    colors = itemColors,
                    icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                    label = { Text(TimerDest.Timer.label) }
                )
                NavigationBarItem(
                    selected = currentTab == TimerDest.History,
                    onClick = { currentTab = TimerDest.History },
                    colors = itemColors,
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text(TimerDest.History.label) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                TimerDest.Timer -> TimerScreen()
                TimerDest.History -> HistoryScreen()
            }
        }
    }
}
