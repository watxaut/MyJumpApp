package com.watxaut.myjumpapp.utils.database

import androidx.room.TypeConverter
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import java.util.Date

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromSurfaceType(surfaceType: SurfaceType): String {
        return surfaceType.name
    }
    
    @TypeConverter
    fun toSurfaceType(surfaceTypeName: String): SurfaceType {
        return SurfaceType.fromString(surfaceTypeName)
    }
}