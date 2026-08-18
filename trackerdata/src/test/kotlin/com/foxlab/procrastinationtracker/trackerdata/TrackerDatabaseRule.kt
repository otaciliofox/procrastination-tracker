package com.foxlab.procrastinationtracker.trackerdata

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/**
 * A real Room database, in memory, on the JVM. Robolectric supplies the Android context, so these
 * tests exercise the actual SQL, the actual entities and the actual repository -- the parts a
 * hand-written fake would quietly get wrong -- without a device or an emulator in the loop.
 */
class TrackerDatabaseRule : ExternalResource() {

    lateinit var database: TrackerDatabase
        private set

    lateinit var repository: TrackerRepository
        private set

    override fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TrackerRepository(database)
    }

    override fun after() {
        database.close()
    }
}
