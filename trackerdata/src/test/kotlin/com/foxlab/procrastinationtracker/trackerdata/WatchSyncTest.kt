package com.foxlab.procrastinationtracker.trackerdata

import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The watch, faked at the only seam that matters.
 *
 * On a real pair of devices the watch builds a [TrackerSyncPayload], the Data Layer carries it,
 * and `ActivitySyncListenerService` hands it to `mergeSyncPayload`. Everything in the middle is
 * Google's transport -- there is nothing of ours to test in it, and automating a paired watch
 * emulator is famously unreliable. So these tests play the watch's part directly: they build the
 * payload the watch would send, put it through the real codec, and assert what the phone's
 * database does with it.
 *
 * That covers every merge rule the app owns, in about a second, on any machine including CI.
 */
@RunWith(RobolectricTestRunner::class)
class WatchSyncTest {

    @get:Rule
    val db = TrackerDatabaseRule()

    private val now = 1_750_000_000_000L

    /** A session the watch recorded, as it would arrive here. */
    private fun watchSession(
        id: String = "watch-session-1",
        sliceId: String = seededSliceId("duo", 0),
        startTime: Long = now,
        endTime: Long? = now + 25 * 60_000L
    ) = ActivitySessionEntity(
        id = id,
        sliceId = sliceId,
        subtaskId = null,
        startTime = startTime,
        endTime = endTime,
        lastHeartbeatAt = null,
        sourceDevice = SOURCE_WATCH,
        createdAt = startTime
    )

    /** Round-trips through the real codec, the way the Data Layer would deliver it. */
    private fun overTheWire(payload: TrackerSyncPayload): TrackerSyncPayload =
        TrackerSyncCodec.decode(TrackerSyncCodec.encode(payload))

    // -----------------------------------------------------------------
    // The handshake: the watch says hello and the phone takes it in
    // -----------------------------------------------------------------

    @Test
    fun `a session recorded on the watch lands in the phone database`() = runTest {
        db.repository.ensureSeeded()

        val payload = overTheWire(
            TrackerSyncPayload(
                profiles = emptyList(),
                slices = emptyList(),
                sessions = listOf(watchSession())
            )
        )
        db.repository.mergeSyncPayload(payload)

        val stored = db.database.activitySessionDao().getAllSince(0)
        assertEquals(1, stored.size)
        assertEquals("watch-session-1", stored.single().id)
        assertEquals(SOURCE_WATCH, stored.single().sourceDevice)
        assertEquals(25 * 60_000L, stored.single().endTime!! - stored.single().startTime)
    }

    @Test
    fun `the payload survives the wire format intact`() {
        val original = TrackerSyncPayload(
            profiles = listOf(
                LayoutProfileEntity(
                    id = "custom-1",
                    type = ProfileType.CUSTOM,
                    title = "Rotina da manhã",
                    isActive = true,
                    forkedFromProfileId = "tri",
                    createdAt = now,
                    updatedAt = now
                )
            ),
            slices = emptyList(),
            sessions = listOf(watchSession()),
            deletedSessionIds = listOf("gone-1", "gone-2")
        )

        val decoded = overTheWire(original)

        assertEquals(original.profiles, decoded.profiles)
        assertEquals(original.sessions, decoded.sessions)
        assertEquals(original.deletedSessionIds, decoded.deletedSessionIds)
    }

    @Test
    fun `a session still running on the watch is not treated as finished here`() = runTest {
        db.repository.ensureSeeded()

        val payload = overTheWire(
            TrackerSyncPayload(
                profiles = emptyList(),
                slices = emptyList(),
                sessions = listOf(watchSession(endTime = null))
            )
        )
        db.repository.mergeSyncPayload(payload)

        val stored = db.database.activitySessionDao().getAllSince(0).single()
        assertNull(stored.endTime)
    }

    // -----------------------------------------------------------------
    // Merge rules the app owns
    // -----------------------------------------------------------------

    @Test
    fun `which profile is active stays a local decision`() = runTest {
        db.repository.ensureSeeded()
        db.repository.switchActiveProfile("tri")

        // The watch is on Duo and says so.
        val duoOnWatch = db.database.layoutProfileDao().getById("duo")!!
            .copy(isActive = true, updatedAt = now + 1)
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(listOf(duoOnWatch), emptyList(), emptyList()))
        )

        assertTrue("Tri must stay active here", db.database.layoutProfileDao().getById("tri")!!.isActive)
        assertFalse("Duo must not steal focus", db.database.layoutProfileDao().getById("duo")!!.isActive)
    }

    @Test
    fun `a profile unknown here arrives switched off`() = runTest {
        db.repository.ensureSeeded()

        val watchProfile = LayoutProfileEntity(
            id = "custom-watch",
            type = ProfileType.CUSTOM,
            title = "Feito no relógio",
            isActive = true,
            forkedFromProfileId = null,
            createdAt = now,
            updatedAt = now
        )
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(listOf(watchProfile), emptyList(), emptyList()))
        )

        val stored = db.database.layoutProfileDao().getById("custom-watch")
        assertNotNull(stored)
        assertEquals("Feito no relógio", stored!!.title)
        assertFalse(stored.isActive)
    }

    @Test
    fun `a newer rename from the watch wins`() = runTest {
        db.repository.ensureSeeded()
        val local = db.database.layoutProfileDao().getById("duo")!!

        val renamedOnWatch = local.copy(title = "Renomeado no relógio", updatedAt = local.updatedAt + 5_000)
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(listOf(renamedOnWatch), emptyList(), emptyList()))
        )

        assertEquals("Renomeado no relógio", db.database.layoutProfileDao().getById("duo")!!.title)
    }

    @Test
    fun `a stale rename from the watch is ignored`() = runTest {
        db.repository.ensureSeeded()
        val local = db.database.layoutProfileDao().getById("duo")!!

        val staleOnWatch = local.copy(title = "Nome antigo", updatedAt = local.updatedAt - 5_000)
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(listOf(staleOnWatch), emptyList(), emptyList()))
        )

        assertEquals(local.title, db.database.layoutProfileDao().getById("duo")!!.title)
    }

    // -----------------------------------------------------------------
    // Tombstones: the rule that makes deletions stick
    // -----------------------------------------------------------------

    @Test
    fun `a session deleted here is not resurrected by the watch`() = runTest {
        db.repository.ensureSeeded()
        val session = watchSession(id = "doomed")

        // It arrived once, then the user deleted it on the phone.
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(emptyList(), emptyList(), listOf(session)))
        )
        db.repository.mergeSyncPayload(
            overTheWire(
                TrackerSyncPayload(emptyList(), emptyList(), emptyList(), deletedSessionIds = listOf("doomed"))
            )
        )

        // The watch, not knowing that, pushes its copy again on the next full sync.
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(emptyList(), emptyList(), listOf(session)))
        )

        assertTrue(
            "A tombstoned session must never come back",
            db.database.activitySessionDao().getAllSince(0).none { it.id == "doomed" }
        )
    }

    @Test
    fun `a deletion made on the watch is applied here`() = runTest {
        db.repository.ensureSeeded()
        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(emptyList(), emptyList(), listOf(watchSession(id = "remove-me"))))
        )
        assertEquals(1, db.database.activitySessionDao().getAllSince(0).size)

        db.repository.mergeSyncPayload(
            overTheWire(
                TrackerSyncPayload(emptyList(), emptyList(), emptyList(), deletedSessionIds = listOf("remove-me"))
            )
        )

        assertTrue(db.database.activitySessionDao().getAllSince(0).isEmpty())
        assertTrue("The phone must pass the deletion on", "remove-me" in db.database.deletedSessionDao().getAllIds())
    }

    // -----------------------------------------------------------------
    // The property that makes the periodic full push safe
    // -----------------------------------------------------------------

    @Test
    fun `merging the same payload twice changes nothing`() = runTest {
        db.repository.ensureSeeded()
        val payload = overTheWire(
            TrackerSyncPayload(
                profiles = emptyList(),
                slices = emptyList(),
                sessions = listOf(watchSession(id = "a"), watchSession(id = "b", startTime = now + 1))
            )
        )

        db.repository.mergeSyncPayload(payload)
        val afterFirst = db.database.activitySessionDao().getAllSince(0)
        db.repository.mergeSyncPayload(payload)
        val afterSecond = db.database.activitySessionDao().getAllSince(0)

        assertEquals(2, afterFirst.size)
        assertEquals(afterFirst, afterSecond)
    }

    @Test
    fun `a session already recorded here is not duplicated by the watch`() = runTest {
        db.repository.ensureSeeded()
        val shared = watchSession(id = "same-id-both-sides")
        db.database.activitySessionDao().insert(shared.copy(sourceDevice = SOURCE_PHONE))

        db.repository.mergeSyncPayload(
            overTheWire(TrackerSyncPayload(emptyList(), emptyList(), listOf(shared)))
        )

        val stored = db.database.activitySessionDao().getAllSince(0)
        assertEquals(1, stored.size)
        assertEquals("the local row wins", SOURCE_PHONE, stored.single().sourceDevice)
    }
}
