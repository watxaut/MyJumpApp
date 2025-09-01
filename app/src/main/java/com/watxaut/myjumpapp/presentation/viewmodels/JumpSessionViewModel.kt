package com.watxaut.myjumpapp.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.pose.Pose
import com.watxaut.myjumpapp.data.database.entities.Jump
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.repository.JumpRepository
import com.watxaut.myjumpapp.data.repository.JumpSessionRepository
import com.watxaut.myjumpapp.data.repository.UserRepository
import com.watxaut.myjumpapp.domain.jump.JumpDetector
import com.watxaut.myjumpapp.domain.jump.JumpPhase
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.utils.WakeLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JumpSessionUiState(
    val isSessionActive: Boolean = false,
    val userId: String? = null,
    val userName: String = "",
    val currentSessionId: String? = null,
    val jumpCount: Int = 0,
    val currentJumpHeight: Double = 0.0,
    val bestJumpHeight: Double = 0.0,
    val averageJumpHeight: Double = 0.0,
    val sessionDuration: Long = 0L,
    val isCalibrating: Boolean = true,
    val jumpPhase: JumpPhase = JumpPhase.STANDING,
    val isJumping: Boolean = false,
    val debugInfo: DebugInfo = DebugInfo(),
    val error: String? = null
)

@HiltViewModel
class JumpSessionViewModel @Inject constructor(
    private val jumpDetector: JumpDetector,
    private val jumpRepository: JumpRepository,
    private val jumpSessionRepository: JumpSessionRepository,
    private val userRepository: UserRepository,
    private val wakeLockManager: WakeLockManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JumpSessionUiState())
    val uiState: StateFlow<JumpSessionUiState> = _uiState.asStateFlow()
    
    private val jumpHeights = mutableListOf<Double>()
    private var sessionStartTime: Long = 0L
    private var currentSession: JumpSession? = null
    
    init {
        // Observe jump detection
        viewModelScope.launch {
            jumpDetector.jumpData.collect { jumpData ->
                val wasCalibrating = _uiState.value.isCalibrating
                val isNowCalibrating = jumpData.debugInfo.calibrationProgress < jumpData.debugInfo.calibrationFramesNeeded
                
                Log.d("JumpSessionViewModel", "Jump data update: phase=${jumpData.phase}, isJumping=${jumpData.isJumping}, height=${jumpData.jumpHeight}, wasCalibrating=$wasCalibrating, isNowCalibrating=$isNowCalibrating, calibrationProgress=${jumpData.debugInfo.calibrationProgress}/${jumpData.debugInfo.calibrationFramesNeeded}")
                
                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        jumpPhase = jumpData.phase,
                        isJumping = jumpData.isJumping,
                        currentJumpHeight = jumpData.jumpHeight,
                        isCalibrating = isNowCalibrating,
                        debugInfo = jumpData.debugInfo
                    )
                    Log.i("JumpSessionViewModel", "State updated: isCalibrating=${newState.isCalibrating}, jumpPhase=${newState.jumpPhase}, isSessionActive=${newState.isSessionActive}")
                    newState
                }
                
                // Auto-start session when calibration completes
                if (wasCalibrating && !isNowCalibrating && _uiState.value.userId != null && !_uiState.value.isSessionActive) {
                    Log.i("JumpSessionViewModel", "Calibration completed, auto-starting session for user: ${_uiState.value.userId}")
                    startSession(_uiState.value.userId!!)
                }
                
                // Record completed jump
                if (jumpData.phase == JumpPhase.LANDING && jumpData.jumpHeight > 0) {
                    Log.i("JumpSessionViewModel", "Recording jump: height=${jumpData.jumpHeight}cm, airTime=${jumpData.airTime}ms")
                    recordJump(jumpData.jumpHeight, jumpData.airTime)
                }
            }
        }
    }
    
    fun startSession(userId: String) {
        Log.i("JumpSessionViewModel", "Starting session for user: $userId")
        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(userId)
                if (user == null) {
                    Log.e("JumpSessionViewModel", "User not found: $userId")
                    _uiState.update { it.copy(error = "User not found") }
                    return@launch
                }
                Log.i("JumpSessionViewModel", "User found: ${user.userName}")
                
                // Create new session
                val session = JumpSession(
                    userId = userId,
                    sessionName = "Jump Session ${System.currentTimeMillis()}",
                    startTime = System.currentTimeMillis(),
                    isCompleted = false
                )
                Log.i("JumpSessionViewModel", "Created session: ${session.sessionId}")
                
                jumpSessionRepository.insertSession(session)
                currentSession = session
                sessionStartTime = System.currentTimeMillis()
                
                // Acquire wake lock to keep screen on during session
                wakeLockManager.acquireWakeLock(context)
                Log.i("JumpSessionViewModel", "Wake lock acquired")
                
                // Reset detection and UI state
                jumpDetector.resetCalibration()
                jumpHeights.clear()
                Log.i("JumpSessionViewModel", "Jump detector reset and calibration started")
                
                _uiState.update {
                    val newState = it.copy(
                        isSessionActive = true,
                        userId = userId,
                        userName = user.userName,
                        currentSessionId = session.sessionId,
                        jumpCount = 0,
                        currentJumpHeight = 0.0,
                        bestJumpHeight = 0.0,
                        averageJumpHeight = 0.0,
                        sessionDuration = 0L,
                        isCalibrating = true,
                        error = null
                    )
                    Log.i("JumpSessionViewModel", "Session started - isSessionActive=${newState.isSessionActive}, isCalibrating=${newState.isCalibrating}")
                    newState
                }
                
            } catch (exception: Exception) {
                Log.e("JumpSessionViewModel", "Failed to start session", exception)
                _uiState.update {
                    it.copy(error = exception.message ?: "Failed to start session")
                }
            }
        }
    }
    
    fun stopSession() {
        viewModelScope.launch {
            try {
                val session = currentSession ?: return@launch
                val currentState = _uiState.value
                
                // Update session with final statistics
                val updatedSession = session.copy(
                    endTime = System.currentTimeMillis(),
                    totalJumps = currentState.jumpCount,
                    bestJumpHeight = currentState.bestJumpHeight,
                    averageJumpHeight = currentState.averageJumpHeight,
                    isCompleted = true
                )
                
                jumpSessionRepository.updateSession(updatedSession)
                
                // Update user statistics
                val user = userRepository.getUserById(session.userId)
                if (user != null) {
                    val newTotalJumps = user.totalJumps + currentState.jumpCount
                    val newBestHeight = maxOf(user.bestJumpHeight, currentState.bestJumpHeight)
                    
                    userRepository.updateUserStats(
                        userId = session.userId,
                        totalJumps = newTotalJumps,
                        bestHeight = newBestHeight
                    )
                }
                
                // Release wake lock
                wakeLockManager.releaseWakeLock(context)
                
                // Reset state
                currentSession = null
                jumpHeights.clear()
                
                _uiState.update {
                    JumpSessionUiState(
                        userId = currentState.userId,
                        userName = currentState.userName
                    )
                }
                
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(error = exception.message ?: "Failed to stop session")
                }
            }
        }
    }
    
    fun processPoseDetection(imageProxy: ImageProxy, pose: Pose) {
        val currentState = _uiState.value
        Log.d("JumpSessionViewModel", "Processing pose - sessionActive=${currentState.isSessionActive}, isCalibrating=${currentState.isCalibrating}, landmarks=${pose.allPoseLandmarks.size}")
        
        // Always process poses when calibrating OR when session is active
        if (currentState.isCalibrating || currentState.isSessionActive) {
            jumpDetector.processPose(pose)
            
            // Update session duration only when session is active
            if (currentState.isSessionActive) {
                val currentTime = System.currentTimeMillis()
                val duration = currentTime - sessionStartTime
                _uiState.update { it.copy(sessionDuration = duration) }
            }
        } else {
            Log.d("JumpSessionViewModel", "Skipping pose processing - session not active and not calibrating")
        }
    }
    
    private fun recordJump(height: Double, airTime: Long) {
        val session = currentSession ?: return
        val currentState = _uiState.value
        
        viewModelScope.launch {
            try {
                // Create jump record
                val jump = Jump(
                    sessionId = session.sessionId,
                    userId = session.userId,
                    heightCm = height,
                    flightTimeMs = airTime,
                    timestamp = System.currentTimeMillis()
                )
                
                jumpRepository.insertJump(jump)
                
                // Update statistics
                jumpHeights.add(height)
                val newJumpCount = jumpHeights.size
                val newBestHeight = jumpHeights.maxOrNull() ?: 0.0
                val newAverageHeight = if (jumpHeights.isNotEmpty()) {
                    jumpHeights.average()
                } else 0.0
                
                _uiState.update { currentState ->
                    currentState.copy(
                        jumpCount = newJumpCount,
                        bestJumpHeight = newBestHeight,
                        averageJumpHeight = newAverageHeight
                    )
                }
                
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(error = exception.message ?: "Failed to record jump")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun setUserId(userId: String) {
        viewModelScope.launch {
            val user = userRepository.getUserById(userId)
            if (user != null) {
                _uiState.update {
                    it.copy(
                        userId = userId,
                        userName = user.userName
                    )
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Always release wake lock when ViewModel is destroyed
        wakeLockManager.releaseWakeLock(context)
    }
}