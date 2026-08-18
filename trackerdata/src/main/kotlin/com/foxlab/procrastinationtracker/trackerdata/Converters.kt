package com.foxlab.procrastinationtracker.trackerdata

import androidx.room.TypeConverter
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType

class Converters {
    @TypeConverter
    fun fromProfileType(type: ProfileType): String = type.name

    @TypeConverter
    fun toProfileType(value: String): ProfileType = ProfileType.valueOf(value)
}
