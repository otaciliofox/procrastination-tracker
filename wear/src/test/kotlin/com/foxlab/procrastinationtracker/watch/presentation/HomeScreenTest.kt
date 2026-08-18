package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The watch's entry screen, rendered on a round display without a watch attached.
 *
 * The qualifier is what makes this meaningful: a layout that fits a phone can still lose its
 * corners on a circle, and rendering at the real 454dp round size is what would catch that.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    // Galaxy Watch6-sized round screen.
    qualifiers = "w227dp-h227dp-small-notlong-round-watch-xhdpi-keyshidden-nonav",
    application = android.app.Application::class
)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val screenshots = "build/reports/screenshots"

    @Test
    fun `offers both modes on the round screen`() {
        compose.setContent { HomeScreen(onOpenTimer = {}, onOpenTracker = {}) }

        compose.onNodeWithText("Tracker").assertIsDisplayed()
        compose.onNodeWithText("Timer").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/watch-home.png")
    }

    @Test
    fun `each card opens its own mode`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            HomeScreen(
                onOpenTimer = { opened += "timer" },
                onOpenTracker = { opened += "tracker" }
            )
        }

        compose.onNodeWithText("Tracker").performClick()
        compose.onNodeWithText("Timer").performClick()

        assertEquals(listOf("tracker", "timer"), opened)
    }
}
