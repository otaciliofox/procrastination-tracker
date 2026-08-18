package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.foxlab.procrastinationtracker.core.ActivityIcons

/**
 * Key -> drawable for the watch. *Which* key an activity gets is decided by [ActivityIcons] in
 * `:core`, shared with the phone; this file is only the wrist's half of that contract, which is
 * why the two apps can never drift into showing different icons for the same activity.
 */
object WatchActivityIcons {

    private val byKey: Map<String, ImageVector> = mapOf(
        ActivityIcons.WORK to Icons.Filled.Work,
        ActivityIcons.SCHOOL to Icons.Filled.School,
        ActivityIcons.COFFEE to Icons.Filled.Coffee,
        ActivityIcons.FOCUS to Icons.Filled.GpsFixed,
        ActivityIcons.FITNESS to Icons.Filled.FitnessCenter,
        ActivityIcons.BOOK to Icons.Filled.MenuBook,
        ActivityIcons.HOBBY to Icons.Filled.Palette,
        ActivityIcons.REST to Icons.Filled.SelfImprovement,
        ActivityIcons.HOME to Icons.Filled.Home,
        ActivityIcons.SLEEP to Icons.Filled.Bedtime,
        ActivityIcons.FOOD to Icons.Filled.Restaurant,
        ActivityIcons.STAR to Icons.Filled.Star
    )

    fun iconFor(title: String, iconKey: String?): ImageVector =
        byKey[ActivityIcons.keyFor(title, iconKey)] ?: Icons.Filled.Star
}
