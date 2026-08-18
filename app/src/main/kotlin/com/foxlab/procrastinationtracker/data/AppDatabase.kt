package com.foxlab.procrastinationtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "procrastination-tracker.db"
                ).build().also { instance = it }
            }
    }
}
