package com.foxlab.procrastinationtracker.core

/**
 * Pure, platform-independent countdown state machine. It knows nothing about
 * Android services, notifications or coroutines -- callers (a foreground service
 * on phone or watch) drive it with [tick] every second and react to the returned
 * [TimerEngine.Event].
 *
 * It runs on a [TimerPlan] rather than on [TimerMode] directly, so a user-defined
 * mode is the same machine with different numbers.
 */
class TimerEngine(plan: TimerPlan) {

    constructor(mode: TimerMode) : this(TimerPlan.of(mode))

    sealed class Event {
        /** Nothing notable happened this tick. */
        data object None : Event()
        /** The running phase just finished; engine has already advanced to the next phase. */
        data class PhaseCompleted(val finishedPhase: Phase, val nextPhase: Phase) : Event()
    }

    var plan: TimerPlan = plan.sanitized()
        private set

    val mode: TimerMode get() = plan.mode

    var phase: Phase = Phase.FOCUS
        private set

    var isRunning: Boolean = false
        private set

    /** Milliseconds left in the current phase. */
    var remainingMillis: Long = plan.focusMinutes.minutesToMillis()
        private set

    /** How many FOCUS phases have been completed since the last long break. */
    var focusStreak: Int = 0
        private set

    var phaseStartTimeMillis: Long = 0L
        private set

    /** How much of the current phase is already spent -- used to save an interrupted block. */
    val elapsedInPhaseMillis: Long get() = (durationFor(phase) - remainingMillis).coerceAtLeast(0L)

    fun start(nowMillis: Long) {
        if (isRunning) return
        isRunning = true
        phaseStartTimeMillis = nowMillis
    }

    fun pause() {
        isRunning = false
    }

    fun reset() = applyPlan(plan)

    fun changeMode(newMode: TimerMode) = applyPlan(TimerPlan.of(newMode))

    fun changePlan(newPlan: TimerPlan) = applyPlan(newPlan)

    private fun applyPlan(newPlan: TimerPlan) {
        plan = newPlan.sanitized()
        phase = Phase.FOCUS
        isRunning = false
        focusStreak = 0
        remainingMillis = plan.focusMinutes.minutesToMillis()
        phaseStartTimeMillis = 0L
    }

    /**
     * Advance the clock by [deltaMillis]. Returns [Event.PhaseCompleted] the tick that
     * crosses zero, so the caller can persist a [Session] and fire a notification.
     */
    fun tick(deltaMillis: Long): Event {
        if (!isRunning) return Event.None
        remainingMillis -= deltaMillis
        if (remainingMillis > 0) return Event.None

        val finished = phase
        if (finished == Phase.FOCUS) focusStreak++

        val next = nextPhase()
        phase = next
        remainingMillis = durationFor(next)
        if (finished == Phase.LONG_BREAK) focusStreak = 0

        return Event.PhaseCompleted(finishedPhase = finished, nextPhase = next)
    }

    private fun nextPhase(): Phase = when (phase) {
        Phase.FOCUS -> if (plan.hasLongBreak && focusStreak % plan.focusSessionsBeforeLongBreak == 0) {
            Phase.LONG_BREAK
        } else {
            Phase.SHORT_BREAK
        }
        Phase.SHORT_BREAK, Phase.LONG_BREAK -> Phase.FOCUS
    }

    fun durationFor(p: Phase): Long = when (p) {
        Phase.FOCUS -> plan.focusMinutes.minutesToMillis()
        Phase.SHORT_BREAK -> plan.shortBreakMinutes.minutesToMillis()
        Phase.LONG_BREAK -> plan.longBreakMinutes.minutesToMillis()
    }
}
