package com.watxaut.myjumpapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.dao.UserDao
import com.watxaut.myjumpapp.data.database.entities.Jump
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.utils.database.Converters

@Database(
    entities = [User::class, Jump::class, JumpSession::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JumpDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun jumpDao(): JumpDao
    abstract fun jumpSessionDao(): JumpSessionDao
    
    companion object {
        const val DATABASE_NAME = "jump_database"
    }
}