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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add eye_to_head_vertex_cm column to users table
        database.execSQL("ALTER TABLE users ADD COLUMN eye_to_head_vertex_cm REAL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add heel_to_hand_reach_cm column to users table for spike reach measurement
        database.execSQL("ALTER TABLE users ADD COLUMN heel_to_hand_reach_cm REAL")
        
        // Add spike_reach_cm column to jumps table for spike reach tracking
        database.execSQL("ALTER TABLE jumps ADD COLUMN spike_reach_cm REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add jump type columns to jump_sessions table for dynamic jump tracking
        database.execSQL("ALTER TABLE jump_sessions ADD COLUMN jump_type TEXT NOT NULL DEFAULT 'STATIC'")
        database.execSQL("ALTER TABLE jump_sessions ADD COLUMN hand_reach_height REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE jump_sessions ADD COLUMN theoretical_spike_reach REAL NOT NULL DEFAULT 0.0")
        
        // Create index on jump_type for efficient querying
        database.execSQL("CREATE INDEX index_jump_sessions_jump_type ON jump_sessions(jump_type)")
    }
}