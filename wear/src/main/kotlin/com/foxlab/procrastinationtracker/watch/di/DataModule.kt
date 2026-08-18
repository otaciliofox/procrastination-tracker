package com.foxlab.procrastinationtracker.watch.di

import android.content.Context
import com.foxlab.procrastinationtracker.trackerdata.TrackerDatabase
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The watch's half of the data layer, built once for the whole process.
 *
 * Mirrors the phone's module deliberately: both apps share `:trackerdata`, so the watch gets the
 * same schema, the same repository and the same merge rules -- only the surrounding app differs.
 * There is no Timer-mode database here; the watch keeps that state in its foreground service and
 * pushes finished sessions to the phone.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideTrackerDatabase(@ApplicationContext context: Context): TrackerDatabase =
        TrackerDatabase.build(context)

    @Provides
    @Singleton
    fun provideTrackerRepository(database: TrackerDatabase): TrackerRepository =
        TrackerRepository(database)
}
