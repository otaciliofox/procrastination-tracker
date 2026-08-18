package com.foxlab.procrastinationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screen, drives it the way a user would, and leaves a PNG behind as proof.
 *
 * It runs on the JVM: Robolectric provides the Android runtime and Roborazzi rasterises Compose,
 * so there is no emulator and no attached device involved. That is what makes these safe to put
 * in a pipeline -- they finish in seconds and cannot fail because a device went to sleep.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application on purpose. The real ProcrastinationTrackerApp opens the Room database in
// `onCreate` and launches a sync loop that never ends, which outlives the test and then crashes
// the *next* one when the database closes underneath it. A screen this pure needs none of that.
// Removing that coupling is what the dependency injection item in the roadmap buys.
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, application = android.app.Application::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val screenshots = "build/reports/screenshots"

    @Test
    fun `offers both modes and captures the screen`() {
        compose.setContent { HomeScreen(onOpenTimer = {}, onOpenTracker = {}) }

        compose.onNodeWithText("Modo Tracker").assertIsDisplayed()
        compose.onNodeWithText("Modo Timer").assertIsDisplayed()
        compose.onNodeWithText("Cronômetro livre por categoria, sem alarme").assertIsDisplayed()
        compose.onNodeWithText("52/17, Pomodoro ou 45/15 com alarme").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/home-screen.png")
    }

    @Test
    fun `tapping the tracker card opens tracker mode`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            HomeScreen(
                onOpenTimer = { opened += "timer" },
                onOpenTracker = { opened += "tracker" }
            )
        }

        compose.onNodeWithText("Modo Tracker").performClick()

        assertEquals(listOf("tracker"), opened)
        compose.onRoot().captureRoboImage("$screenshots/home-screen-tracker-tapped.png")
    }

    @Test
    fun `tapping the timer card opens timer mode`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            HomeScreen(
                onOpenTimer = { opened += "timer" },
                onOpenTracker = { opened += "tracker" }
            )
        }

        compose.onNodeWithText("Modo Timer").performClick()

        assertEquals(listOf("timer"), opened)
    }
}
