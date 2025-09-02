package com.watxaut.myjumpapp.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add surface_type column to jump_sessions table
        database.execSQL("ALTER TABLE jump_sessions ADD COLUMN surface_type TEXT NOT NULL DEFAULT 'HARD_FLOOR'")
        
        // Add surface-specific statistics columns to users table
        database.execSQL("ALTER TABLE users ADD COLUMN best_jump_height_hard_floor REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE users ADD COLUMN best_jump_height_sand REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE users ADD COLUMN total_sessions_hard_floor INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN total_sessions_sand INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN total_jumps_hard_floor INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN total_jumps_sand INTEGER NOT NULL DEFAULT 0")
        
        // Create index on surface_type for efficient querying
        database.execSQL("CREATE INDEX index_jump_sessions_surface_type ON jump_sessions(surface_type)")
        
        // Migrate existing data: set all existing sessions to HARD_FLOOR and update user stats
        database.execSQL("""
            UPDATE users 
            SET best_jump_height_hard_floor = best_jump_height,
                total_sessions_hard_floor = (
                    SELECT COUNT(*) 
                    FROM jump_sessions 
                    WHERE jump_sessions.user_id = users.user_id 
                    AND jump_sessions.is_completed = 1
                ),
                total_jumps_hard_floor = total_jumps
        """)
    }
}