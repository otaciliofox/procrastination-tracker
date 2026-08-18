package com.foxlab.procrastinationtracker.data

import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class SessionRepository(private val dao: SessionDao) {

    suspend fun record(session: Session) {
        dao.insert(session.toEntity())
    }

    fun observeAll(): Flow<List<Session>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<List<Session>> =
        dayStartFlow().flatMapLatest { startOfDay ->
            dao.observeSince(startOfDay).map { list -> list.map { it.toDomain() } }
        }

    /** Today's cycle report -- the aggregation is a `:core` rule, so the watch can reuse it. */
    fun observeTodaySummary(): Flow<TimerDaySummary> = observeToday().map { TimerDaySummary.from(it) }

    /**
     * Sum of focused vs break time, in millis. Interrupted blocks count here -- they were still
     * time spent; what they don't count as is a finished cycle.
     */
    fun totals(sessions: List<Session>): Pair<Long, Long> {
        var focused = 0L
        var breaks = 0L
        for (s in sessions) {
            if (s.phase == com.foxlab.procrastinationtracker.core.Phase.FOCUS) focused += s.durationMillis else breaks += s.durationMillis
        }
        return focused to breaks
    }

    /** Same midnight-safe window the tracker uses: recheck the boundary, emit only when it moves. */
    private fun dayStartFlow(): Flow<Long> = flow {
        while (true) {
            emit(startOfTodayMillis())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
