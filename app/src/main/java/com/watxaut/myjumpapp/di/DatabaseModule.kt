package com.watxaut.myjumpapp.di

import android.content.Context
import androidx.room.Room
import com.watxaut.myjumpapp.data.database.JumpDatabase
import com.watxaut.myjumpapp.data.database.migrations.MIGRATION_1_2
import com.watxaut.myjumpapp.data.database.migrations.MIGRATION_2_3
import com.watxaut.myjumpapp.data.database.migrations.MIGRATION_3_4
import com.watxaut.myjumpapp.data.database.migrations.MIGRATION_4_5
import com.watxaut.myjumpapp.data.database.dao.JumpDao
import com.watxaut.myjumpapp.data.database.dao.JumpSessionDao
import com.watxaut.myjumpapp.data.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJumpDatabase(@ApplicationContext context: Context): JumpDatabase {
        return Room.databaseBuilder(
            context,
            JumpDatabase::class.java,
            JumpDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    @Provides
    fun provideUserDao(database: JumpDatabase): UserDao = database.userDao()

    @Provides
    fun provideJumpDao(database: JumpDatabase): JumpDao = database.jumpDao()

    @Provides
    fun provideJumpSessionDao(database: JumpDatabase): JumpSessionDao = database.jumpSessionDao()
}