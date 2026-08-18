package com.foxlab.procrastinationtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
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
 * The tracker board -- the app's main screen, where each band's height is its share of the day.
 * It takes plain data and lambdas rather than a ViewModel, which is what makes it renderable here
 * with a day's worth of tracked time made up on the spot.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, application = android.app.Application::class)
class TrackerBoardTest {

    @get:Rule
    val compose = createComposeRule()

    private val screenshots = "build/reports/screenshots"

    private fun slice(id: String, title: String, position: Int, icon: String) = ActivitySliceEntity(
        id = id,
        profileId = "tri",
        title = title,
        color = null,
        position = position,
        timerModeId = null,
        iconKey = icon,
        createdAt = 0,
        updatedAt = 0
    )

    private val slices = listOf(
        slice("tri-slice-0", "Trabalho", 0, "work"),
        slice("tri-slice-1", "Estudo", 1, "school"),
        slice("tri-slice-2", "Procrastinando", 2, "coffee")
    )

    /** Three hours of work, an hour and twenty of study, forty-five minutes lost. */
    private val todayTotals = mapOf(
        "tri-slice-0" to 3 * 60 * 60_000L,
        "tri-slice-1" to 80 * 60_000L,
        "tri-slice-2" to 45 * 60_000L
    )

    @Composable
    private fun board(
        activeSliceId: String? = "tri-slice-0",
        isTracking: Boolean = true,
        liveElapsedMillis: Long = 0L,
        onSliceTap: (ActivitySliceEntity) -> Unit = {},
        onPlayPauseTap: () -> Unit = {}
    ) {
        TrackerBoard(
            slices = slices,
            activeSliceId = activeSliceId,
            resumeSliceId = null,
            isTracking = isTracking,
            liveElapsedMillis = liveElapsedMillis,
            todayTotals = todayTotals,
            todayAllProfilesMillis = todayTotals.values.sum(),
            showReportButton = true,
            onSliceTap = onSliceTap,
            onSliceLongPress = {},
            onPlayPauseTap = onPlayPauseTap,
            onOptionsTap = {},
            onReportTap = {}
        )
    }

    @Test
    fun `shows every activity with the time it holds`() {
        compose.setContent { board() }

        compose.onNodeWithText("Trabalho").assertIsDisplayed()
        compose.onNodeWithText("Estudo").assertIsDisplayed()
        compose.onNodeWithText("Procrastinando").assertIsDisplayed()
        compose.onNodeWithText("3:00:00").assertIsDisplayed()
        compose.onNodeWithText("1:20:00").assertIsDisplayed()
        compose.onNodeWithText("45:00").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/tracker-board.png")
    }

    @Test
    fun `the running activity counts the live time on top of what is banked`() {
        compose.setContent { board(liveElapsedMillis = 30 * 60_000L) }

        // Work is the active band: 3h banked plus half an hour running.
        compose.onNodeWithText("3:30:00").assertIsDisplayed()
        // The others are untouched by the live clock.
        compose.onNodeWithText("1:20:00").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/tracker-board-running.png")
    }

    @Test
    fun `tapping a band asks to track it`() {
        val tapped = mutableListOf<String>()
        compose.setContent { board(onSliceTap = { tapped += it.title }) }

        compose.onNodeWithText("Estudo").performClick()

        assertEquals(listOf("Estudo"), tapped)
    }

    @Test
    fun `a paused board still shows the day so far`() {
        compose.setContent { board(activeSliceId = null, isTracking = false) }

        compose.onNodeWithText("Trabalho").assertIsDisplayed()
        compose.onNodeWithText("3:00:00").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/tracker-board-paused.png")
    }
}
