package com.foxlab.procrastinationtracker.trackerdata

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.foxlab.procrastinationtracker.trackerdata.dao.ActivitySessionDao
import com.foxlab.procrastinationtracker.trackerdata.dao.ActivitySliceDao
import com.foxlab.procrastinationtracker.trackerdata.dao.DeletedSessionDao
import com.foxlab.procrastinationtracker.trackerdata.dao.LayoutProfileDao
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.DeletedSessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity

@Database(
    entities = [
        LayoutProfileEntity::class,
        ActivitySliceEntity::class,
        ActivitySessionEntity::class,
        DeletedSessionEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackerDatabase : RoomDatabase() {

    abstract fun layoutProfileDao(): LayoutProfileDao
    abstract fun activitySliceDao(): ActivitySliceDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun deletedSessionDao(): DeletedSessionDao

    companion object {
        /**
         * v2 -> v3 adds the tombstone table. Written as a real migration instead of leaning on the
         * destructive fallback: by now people have weeks of tracked time in here, and wiping it to
         * add one table would be an absurd trade.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `deleted_session` (
                        `sessionId` TEXT NOT NULL,
                        `deletedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v3 -> v4 rewrites the seeded activities of Duo/Tri to deterministic ids (`duo-slice-0`
         * and friends) and repoints their sessions. Before this, each device had its own random
         * UUID for "Foco", so the first sync would have merged them into duplicates instead of
         * recognising them as the same activity.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("duo" to 2, "tri" to 3).forEach { (profileId, sliceCount) ->
                    for (position in 0 until sliceCount) {
                        val canonicalId = "$profileId-slice-$position"
                        db.execSQL(
                            """
                            UPDATE activity_session SET sliceId = ?
                            WHERE sliceId IN (
                                SELECT id FROM activity_slice
                                WHERE profileId = ? AND position = ? AND id != ?
                            )
                            """.trimIndent(),
                            arrayOf<Any>(canonicalId, profileId, position, canonicalId)
                        )
                        db.execSQL(
                            "UPDATE activity_slice SET id = ? WHERE profileId = ? AND position = ? AND id != ?",
                            arrayOf<Any>(canonicalId, profileId, position, canonicalId)
                        )
                    }
                }
            }
        }

        /**
         * v4 -> v5 adds the hand-off columns. Transferring a running block now closes and saves
         * the block on the device that had it, and opens a new one that remembers where it came
         * from -- so a phone that dies mid-block can't take the watch's time down with it.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activity_session ADD COLUMN continuedFromSessionId TEXT")
                db.execSQL("ALTER TABLE activity_session ADD COLUMN carriedMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v5 -> v6 adds the "already sent" marker used by incremental sync. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activity_session ADD COLUMN syncedAt INTEGER")
            }
        }

        @Volatile private var instance: TrackerDatabase? = null

        fun build(context: Context): TrackerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "procrastination-tracker-activities.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
