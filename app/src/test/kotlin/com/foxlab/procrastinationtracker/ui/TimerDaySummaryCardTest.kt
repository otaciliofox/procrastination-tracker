package com.foxlab.procrastinationtracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import com.foxlab.procrastinationtracker.core.TimerMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The productive vs. procrastinated card, fed by the same `:core` arithmetic the app uses at
 * runtime. This is where a unit test and a screenshot test meet: `TimerDaySummary.from` decides
 * the numbers and has its own tests, and this one proves those numbers reach the screen intact.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, application = android.app.Application::class)
class TimerDaySummaryCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val screenshots = "build/reports/screenshots"

    private fun session(phase: Phase, minutes: Long, completed: Boolean = true) = Session(
        mode = TimerMode.FIFTY_TWO_SEVENTEEN,
        phase = phase,
        startTimeMillis = 0,
        endTimeMillis = minutes * 60_000L,
        completedFully = completed
    )

    @Test
    fun `a day of work reports focus, break and closed cycles`() {
        val summary = TimerDaySummary.from(
            listOf(
                session(Phase.FOCUS, 52),
                session(Phase.SHORT_BREAK, 17),
                session(Phase.FOCUS, 52),
                session(Phase.SHORT_BREAK, 17),
                session(Phase.FOCUS, 31, completed = false)
            )
        )

        compose.setContent { TimerDaySummaryCard(summary) }

        compose.onNodeWithText("HOJE NO TIMER").assertIsDisplayed()
        // 52 + 52 + 31 minutes of focus, the interrupted block included in the time...
        compose.onNodeWithText("2h 15m").assertIsDisplayed()
        compose.onNodeWithText("34m").assertIsDisplayed()
        // ...but only the two finished blocks counted as cycles.
        compose.onNodeWithText("2 ciclos concluídos").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/timer-summary-card.png")
    }

    @Test
    fun `a single closed cycle is written in the singular`() {
        val summary = TimerDaySummary.from(listOf(session(Phase.FOCUS, 52)))

        compose.setContent { TimerDaySummaryCard(summary) }

        compose.onNodeWithText("1 ciclo concluído").assertIsDisplayed()
    }

    @Test
    fun `an empty day says so instead of showing zeros`() {
        compose.setContent { TimerDaySummaryCard(TimerDaySummary()) }

        compose.onNodeWithText("Nenhum ciclo concluído hoje ainda.").assertIsDisplayed()

        compose.onRoot().captureRoboImage("$screenshots/timer-summary-card-empty.png")
    }
}
