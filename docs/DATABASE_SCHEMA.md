# MyJumpApp - Database Schema Documentation

## Database Overview

MyJumpApp uses **Room Database** (SQLite) for local data persistence. The database stores user profiles, jump records, and session data to track vertical jump performance over time.

## Database Configuration

### Room Database Setup
```kotlin
@Database(
    entities = [User::class, Jump::class, JumpSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JumpDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jumpDao(): JumpDao
    abstract fun jumpSessionDao(): JumpSessionDao
}
```

## Entity Definitions

### 1. User Entity
Stores user profile information and settings.

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "date_created")
    val dateCreated: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "height_cm")
    val heightCm: Int? = null,
    
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,
    
    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: Long? = null,
    
    @ColumnInfo(name = "profile_image_path")
    val profileImagePath: String? = null,
    
    @ColumnInfo(name = "total_jumps")
    val totalJumps: Int = 0,
    
    @ColumnInfo(name = "best_jump_height")
    val bestJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
```

**Table Structure:**
- `user_id` (TEXT, PRIMARY KEY) - Unique identifier for each user
- `user_name` (TEXT, NOT NULL) - Display name for the user
- `date_created` (INTEGER, NOT NULL) - Timestamp when user was created
- `height_cm` (INTEGER, NULLABLE) - User's height in centimeters
- `weight_kg` (REAL, NULLABLE) - User's weight in kilograms
- `date_of_birth` (INTEGER, NULLABLE) - User's date of birth timestamp
- `profile_image_path` (TEXT, NULLABLE) - Path to profile image
- `total_jumps` (INTEGER, DEFAULT 0) - Total number of jumps recorded
- `best_jump_height` (REAL, DEFAULT 0.0) - Best jump height in centimeters
- `is_active` (INTEGER, DEFAULT 1) - Whether user is active (boolean as int)

### 2. Jump Entity
Stores individual jump records with measurements and metadata.

```kotlin
@Entity(
    tableName = "jumps",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JumpSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class Jump(
    @PrimaryKey
    val jumpId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    
    @ColumnInfo(name = "height_cm")
    val heightCm: Double,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "video_path")
    val videoPath: String? = null,
    
    @ColumnInfo(name = "takeoff_time")
    val takeoffTime: Long? = null,
    
    @ColumnInfo(name = "landing_time")
    val landingTime: Long? = null,
    
    @ColumnInfo(name = "flight_time_ms")
    val flightTimeMs: Long? = null,
    
    @ColumnInfo(name = "peak_height_frame")
    val peakHeightFrame: Int? = null,
    
    @ColumnInfo(name = "calibration_reference")
    val calibrationReference: Double? = null,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Double = 1.0,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "measurement_method")
    val measurementMethod: String = "camera_vision"
)
```

**Table Structure:**
- `jump_id` (TEXT, PRIMARY KEY) - Unique identifier for each jump
- `user_id` (TEXT, NOT NULL, FOREIGN KEY) - References users.user_id
- `session_id` (TEXT, NULLABLE, FOREIGN KEY) - References jump_sessions.session_id
- `height_cm` (REAL, NOT NULL) - Jump height in centimeters
- `timestamp` (INTEGER, NOT NULL) - When the jump was recorded
- `video_path` (TEXT, NULLABLE) - Path to recorded video file
- `takeoff_time` (INTEGER, NULLABLE) - Timestamp of takeoff moment
- `landing_time` (INTEGER, NULLABLE) - Timestamp of landing moment
- `flight_time_ms` (INTEGER, NULLABLE) - Total flight time in milliseconds
- `peak_height_frame` (INTEGER, NULLABLE) - Video frame at peak height
- `calibration_reference` (REAL, NULLABLE) - Reference measurement for calibration
- `confidence_score` (REAL, DEFAULT 1.0) - AI confidence in measurement
- `notes` (TEXT, NULLABLE) - User notes about the jump
- `measurement_method` (TEXT, DEFAULT 'camera_vision') - How jump was measured

### 3. JumpSession Entity
Groups related jumps together for training sessions.

```kotlin
@Entity(
    tableName = "jump_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["start_time"])
    ]
)
data class JumpSession(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "session_name")
    val sessionName: String? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "total_jumps")
    val totalJumps: Int = 0,
    
    @ColumnInfo(name = "best_jump_height")
    val bestJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "average_jump_height")
    val averageJumpHeight: Double = 0.0,
    
    @ColumnInfo(name = "session_notes")
    val sessionNotes: String? = null,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)
```

**Table Structure:**
- `session_id` (TEXT, PRIMARY KEY) - Unique identifier for each session
- `user_id` (TEXT, NOT NULL, FOREIGN KEY) - References users.user_id
- `session_name` (TEXT, NULLABLE) - Optional name for the session
- `start_time` (INTEGER, NOT NULL) - Session start timestamp
- `end_time` (INTEGER, NULLABLE) - Session end timestamp
- `total_jumps` (INTEGER, DEFAULT 0) - Number of jumps in session
- `best_jump_height` (REAL, DEFAULT 0.0) - Best jump in this session
- `average_jump_height` (REAL, DEFAULT 0.0) - Average jump height
- `session_notes` (TEXT, NULLABLE) - Notes about the training session
- `is_completed` (INTEGER, DEFAULT 0) - Whether session is completed

## Data Access Objects (DAOs)

### UserDao
```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY user_name ASC")
    fun getAllActiveUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateUser(userId: String)
    
    @Query("UPDATE users SET total_jumps = :totalJumps, best_jump_height = :bestHeight WHERE user_id = :userId")
    suspend fun updateUserStats(userId: String, totalJumps: Int, bestHeight: Double)
}
```

### JumpDao
```kotlin
@Dao
interface JumpDao {
    @Query("SELECT * FROM jumps WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getJumpsByUser(userId: String): Flow<List<Jump>>
    
    @Query("SELECT * FROM jumps WHERE user_id = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getJumpsByUserAndDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<Jump>>
    
    @Query("SELECT MAX(height_cm) FROM jumps WHERE user_id = :userId")
    suspend fun getBestJumpHeight(userId: String): Double?
    
    @Query("SELECT AVG(height_cm) FROM jumps WHERE user_id = :userId")
    suspend fun getAverageJumpHeight(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM jumps WHERE user_id = :userId")
    suspend fun getTotalJumpCount(userId: String): Int
    
    @Insert
    suspend fun insertJump(jump: Jump)
    
    @Update
    suspend fun updateJump(jump: Jump)
    
    @Delete
    suspend fun deleteJump(jump: Jump)
    
    @Query("DELETE FROM jumps WHERE user_id = :userId")
    suspend fun deleteAllJumpsForUser(userId: String)
}
```

### JumpSessionDao
```kotlin
@Dao
interface JumpSessionDao {
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId ORDER BY start_time DESC")
    fun getSessionsByUser(userId: String): Flow<List<JumpSession>>
    
    @Query("SELECT * FROM jump_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): JumpSession?
    
    @Query("SELECT * FROM jump_sessions WHERE user_id = :userId AND is_completed = 0 LIMIT 1")
    suspend fun getActiveSession(userId: String): JumpSession?
    
    @Insert
    suspend fun insertSession(session: JumpSession)
    
    @Update
    suspend fun updateSession(session: JumpSession)
    
    @Query("UPDATE jump_sessions SET is_completed = 1, end_time = :endTime WHERE session_id = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long)
    
    @Delete
    suspend fun deleteSession(session: JumpSession)
}
```

## Type Converters

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
```

## Database Relationships

### One-to-Many Relationships
1. **User → Jumps**: One user can have many jumps
2. **User → JumpSessions**: One user can have many training sessions
3. **JumpSession → Jumps**: One session can contain many jumps

### Entity Relationship Diagram
```
User (1) ─────┐
              │
              ├── (Many) Jump
              │
              └── (Many) JumpSession (1) ── (Many) Jump
```

## Indexes and Performance

### Recommended Indexes
```sql
-- Performance indexes for common queries
CREATE INDEX idx_jumps_user_id ON jumps(user_id);
CREATE INDEX idx_jumps_timestamp ON jumps(timestamp);
CREATE INDEX idx_jumps_user_timestamp ON jumps(user_id, timestamp);
CREATE INDEX idx_sessions_user_id ON jump_sessions(user_id);
CREATE INDEX idx_sessions_start_time ON jump_sessions(start_time);
```

## Data Migration Strategy

### Version 1 → Version 2 (Future)
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example: Add new column for jump category
        database.execSQL("ALTER TABLE jumps ADD COLUMN category TEXT DEFAULT 'standard'")
    }
}
```

## Sample Data Queries

### Common Query Examples
```sql
-- Get user's jump history with session info
SELECT j.*, js.session_name 
FROM jumps j 
LEFT JOIN jump_sessions js ON j.session_id = js.session_id 
WHERE j.user_id = ? 
ORDER BY j.timestamp DESC;

-- Get user's monthly progress
SELECT 
    strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch')) as month,
    COUNT(*) as jump_count,
    AVG(height_cm) as avg_height,
    MAX(height_cm) as best_height
FROM jumps 
WHERE user_id = ? 
GROUP BY month 
ORDER BY month DESC;

-- Get session summary
SELECT 
    js.*,
    COUNT(j.jump_id) as actual_jumps,
    AVG(j.height_cm) as actual_avg_height,
    MAX(j.height_cm) as actual_best_height
FROM jump_sessions js
LEFT JOIN jumps j ON js.session_id = j.session_id
WHERE js.session_id = ?
GROUP BY js.session_id;
```

## Storage Considerations

### File Storage
- **Videos**: Stored in `app_data/videos/` directory
- **Profile Images**: Stored in `app_data/profiles/` directory
- **Database**: Located at `databases/jump_database.db`

### Cleanup Strategies
```kotlin
// Clean up old video files when jump is deleted
suspend fun cleanupJumpFiles(jump: Jump) {
    jump.videoPath?.let { path ->
        File(path).delete()
    }
}

// Periodic cleanup of inactive users
suspend fun cleanupInactiveUsers(daysInactive: Int = 365) {
    val cutoffTime = System.currentTimeMillis() - (daysInactive * 24 * 60 * 60 * 1000L)
    // Implementation depends on business requirements
}
```

This database schema provides a robust foundation for tracking vertical jump performance with comprehensive user management, detailed jump records, and session organization capabilities.