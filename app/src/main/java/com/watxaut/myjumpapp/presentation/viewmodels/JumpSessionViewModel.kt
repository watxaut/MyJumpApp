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
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.domain.jump.SurfaceType
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
    val sessionDuration: Long = 0L,
    val isCalibrating: Boolean = true,
    val maxHeight: Double = 0.0,
    val maxHeightLowerBound: Double = 0.0,
    val maxHeightUpperBound: Double = 0.0,
    val maxSpikeReach: Double = 0.0,
    val maxSpikeReachLowerBound: Double = 0.0,
    val maxSpikeReachUpperBound: Double = 0.0,
    val hasEyeToHeadMeasurement: Boolean = false,
    val surfaceType: SurfaceType = SurfaceType.HARD_FLOOR,
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
    
    private var sessionStartTime: Long = 0L
    private var currentSession: JumpSession? = null
    
    init {
        // Observe jump detection
        viewModelScope.launch {
            jumpDetector.jumpData.collect { jumpData ->
                val wasCalibrating = _uiState.value.isCalibrating
                val isNowCalibrating = jumpData.debugInfo.calibrationProgress < jumpData.debugInfo.calibrationFramesNeeded
                
                Log.d("JumpSessionViewModel", "Jump data update: maxHeight=${jumpData.maxHeight}, wasCalibrating=$wasCalibrating, isNowCalibrating=$isNowCalibrating, calibrationProgress=${jumpData.debugInfo.calibrationProgress}/${jumpData.debugInfo.calibrationFramesNeeded}")
                
                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        maxHeight = jumpData.maxHeight,
                        maxHeightLowerBound = jumpData.maxHeightLowerBound,
                        maxHeightUpperBound = jumpData.maxHeightUpperBound,
                        maxSpikeReach = jumpData.maxSpikeReach,
                        maxSpikeReachLowerBound = jumpData.maxSpikeReachLowerBound,
                        maxSpikeReachUpperBound = jumpData.maxSpikeReachUpperBound,
                        hasEyeToHeadMeasurement = jumpData.hasEyeToHeadMeasurement,
                        isCalibrating = isNowCalibrating,
                        debugInfo = jumpData.debugInfo
                    )
                    Log.i("JumpSessionViewModel", "State updated: isCalibrating=${newState.isCalibrating}, maxHeight=${newState.maxHeight}, hasEyeToHead=${newState.hasEyeToHeadMeasurement}, isSessionActive=${newState.isSessionActive}")
                    newState
                }
                
                // Auto-start session when calibration completes
                if (wasCalibrating && !isNowCalibrating && _uiState.value.userId != null && !_uiState.value.isSessionActive) {
                    Log.i("JumpSessionViewModel", "Calibration completed, auto-starting session for user: ${_uiState.value.userId}")
                    
                    // Set user height for pixel-to-cm calibration
                    viewModelScope.launch {
                        val user = userRepository.getUserById(_uiState.value.userId!!)
                        if (user != null && user.heightCm != null && user.heightCm > 0) {
                            jumpDetector.setUserHeight(user.heightCm.toDouble(), user.eyeToHeadVertexCm, user.heelToHandReachCm ?: 0.0)
                            Log.i("JumpSessionViewModel", "Set user height for calibration: ${user.heightCm}cm, eyeToHeadVertex: ${user.eyeToHeadVertexCm ?: "not provided"}, heelToHandReach: ${user.heelToHandReachCm ?: 0.0}cm")
                        } else {
                            Log.w("JumpSessionViewModel", "User height not available for calibration")
                        }
                    }
                    
                    startSession(_uiState.value.userId!!, _uiState.value.surfaceType)
                }
                
            }
        }
    }
    
    fun startSession(userId: String, surfaceType: SurfaceType = SurfaceType.HARD_FLOOR) {
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
                    sessionName = "${surfaceType.displayName} Session ${System.currentTimeMillis()}",
                    startTime = System.currentTimeMillis(),
                    surfaceType = surfaceType,
                    isCompleted = false
                )
                Log.i("JumpSessionViewModel", "Created session: ${session.sessionId}")
                
                jumpSessionRepository.insertSession(session)
                currentSession = session
                sessionStartTime = System.currentTimeMillis()
                
                // Acquire wake lock to keep screen on during session
                wakeLockManager.acquireWakeLock(context)
                Log.i("JumpSessionViewModel", "Wake lock acquired")
                
                Log.i("JumpSessionViewModel", "Session started with completed calibration")
                
                _uiState.update {
                    val newState = it.copy(
                        isSessionActive = true,
                        userId = userId,
                        userName = user.userName,
                        currentSessionId = session.sessionId,
                        surfaceType = surfaceType,
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
                    totalJumps = 1, // Each session counts as 1 jump
                    bestJumpHeight = currentState.maxHeight,
                    averageJumpHeight = currentState.maxHeight,
                    isCompleted = true
                )
                
                jumpSessionRepository.updateSession(updatedSession)
                
                // Update user statistics
                val user = userRepository.getUserById(session.userId)
                if (user != null) {
                    val newBestHeight = maxOf(user.bestJumpHeight, currentState.maxHeight)
                    
                    // Update overall stats - increment jump count
                    userRepository.updateUserStats(
                        userId = session.userId,
                        totalJumps = user.totalJumps + 1,
                        bestHeight = newBestHeight
                    )
                    
                    // Update surface-specific stats
                    val currentSurfaceType = session.surfaceType
                    val newBestHeightHardFloor = if (currentSurfaceType == SurfaceType.HARD_FLOOR) {
                        maxOf(user.bestJumpHeightHardFloor, currentState.maxHeight)
                    } else user.bestJumpHeightHardFloor
                    
                    val newBestHeightSand = if (currentSurfaceType == SurfaceType.SAND) {
                        maxOf(user.bestJumpHeightSand, currentState.maxHeight)
                    } else user.bestJumpHeightSand
                    
                    val newSessionsHardFloor = if (currentSurfaceType == SurfaceType.HARD_FLOOR) {
                        user.totalSessionsHardFloor + 1
                    } else user.totalSessionsHardFloor
                    
                    val newSessionsSand = if (currentSurfaceType == SurfaceType.SAND) {
                        user.totalSessionsSand + 1
                    } else user.totalSessionsSand
                    
                    val newJumpsHardFloor = if (currentSurfaceType == SurfaceType.HARD_FLOOR) {
                        user.totalJumpsHardFloor + 1
                    } else user.totalJumpsHardFloor
                    
                    val newJumpsSand = if (currentSurfaceType == SurfaceType.SAND) {
                        user.totalJumpsSand + 1
                    } else user.totalJumpsSand
                    
                    userRepository.updateSurfaceSpecificStats(
                        userId = session.userId,
                        bestHeightHardFloor = newBestHeightHardFloor,
                        bestHeightSand = newBestHeightSand,
                        totalSessionsHardFloor = newSessionsHardFloor,
                        totalSessionsSand = newSessionsSand,
                        totalJumpsHardFloor = newJumpsHardFloor,
                        totalJumpsSand = newJumpsSand
                    )
                }
                
                // Release wake lock
                wakeLockManager.releaseWakeLock(context)
                
                // Reset state and trigger new calibration
                currentSession = null
                jumpDetector.resetCalibration()
                
                _uiState.update {
                    JumpSessionUiState(
                        userId = currentState.userId,
                        userName = currentState.userName,
                        isCalibrating = true
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
    
    fun setSurfaceType(surfaceType: SurfaceType) {
        _uiState.update {
            it.copy(surfaceType = surfaceType)
        }
    }
    
    fun resetToInitialState() {
        // Stop any active session
        if (_uiState.value.isSessionActive) {
            viewModelScope.launch { stopSession() }
        }
        
        // Reset everything to initial state
        currentSession = null
        jumpDetector.resetCalibration()
        
        // Release wake lock
        wakeLockManager.releaseWakeLock(context)
        
        // Reset UI state
        _uiState.value = JumpSessionUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        resetToInitialState()
    }
}