package com.foxlab.procrastinationtracker.di

import android.content.Context
import com.foxlab.procrastinationtracker.data.AppDatabase
import com.foxlab.procrastinationtracker.data.SessionRepository
import com.foxlab.procrastinationtracker.trackerdata.TrackerDatabase
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Where the app's data layer is built, once, for everyone who needs it.
 *
 * This replaces the lazy properties that used to live on the `Application` subclass and were read
 * by casting `application as ProcrastinationTrackerApp` from ViewModels, services and the tile.
 * That cast was cheap at runtime but it meant every one of those classes could only exist inside a
 * running Android app with that exact Application installed -- which is precisely what made the
 * ViewModels impossible to unit test.
 *
 * Both databases stay singletons: two Room instances over one file would each hold their own
 * write lock and invalidation tracker, so the UI would stop seeing changes made by the services.
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun provideSessionRepository(database: AppDatabase): SessionRepository =
        SessionRepository(database.sessionDao())
}
