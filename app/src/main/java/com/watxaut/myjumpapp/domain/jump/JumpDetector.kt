package com.watxaut.myjumpapp.domain.jump

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class JumpData(
    val jumpHeight: Double = 0.0,
    val airTime: Long = 0L,
    val isJumping: Boolean = false,
    val phase: JumpPhase = JumpPhase.STANDING,
    val debugInfo: DebugInfo = DebugInfo()
)

data class DebugInfo(
    val poseDetected: Boolean = false,
    val calibrationProgress: Int = 0,
    val calibrationFramesNeeded: Int = 30,
    val currentHeight: Double = 0.0,
    val baselineHeight: Double = 0.0,
    val heightDifference: Double = 0.0,
    val landmarksDetected: Int = 0,
    val confidenceScore: Float = 0f
)

enum class JumpPhase {
    STANDING,
    PREPARING,  // Crouching/preparing to jump
    AIRBORNE,   // In the air
    LANDING     // Coming back down
}

@Singleton
class JumpDetector @Inject constructor() {
    
    private val _jumpData = MutableStateFlow(JumpData())
    val jumpData: StateFlow<JumpData> = _jumpData.asStateFlow()
    
    private var baselineHeight: Double = 0.0
    private var previousHeight: Double = 0.0
    private var jumpStartTime: Long = 0L
    private var maxHeightReached: Double = 0.0
    private var frameCount = 0
    private var calibrationFrames = 30 // Frames to establish baseline
    private var heightHistory = mutableListOf<Double>()
    private var isCalibrated = false
    
    private val jumpThreshold = 0.03 // 3cm threshold for detecting a jump
    private val landingThreshold = 0.02 // 2cm threshold for detecting landing
    private val smoothingWindowSize = 5
    
    fun processPose(pose: Pose) {
        val poseDetected = pose.allPoseLandmarks.isNotEmpty()
        val landmarksCount = pose.allPoseLandmarks.size
        val confidenceScore = if (poseDetected) {
            pose.allPoseLandmarks.map { it.inFrameLikelihood }.average().toFloat()
        } else 0f
        
        val currentHeight = calculatePersonHeight(pose)
        
        val debugInfo = DebugInfo(
            poseDetected = poseDetected,
            calibrationProgress = frameCount,
            calibrationFramesNeeded = calibrationFrames,
            currentHeight = currentHeight ?: 0.0,
            baselineHeight = baselineHeight,
            heightDifference = if (currentHeight != null && isCalibrated) {
                currentHeight - baselineHeight
            } else 0.0,
            landmarksDetected = landmarksCount,
            confidenceScore = confidenceScore
        )
        
        // Update debug info immediately
        _jumpData.value = _jumpData.value.copy(debugInfo = debugInfo)
        
        if (currentHeight == null) {
            return
        }
        
        // Smooth the height measurement
        heightHistory.add(currentHeight)
        if (heightHistory.size > smoothingWindowSize) {
            heightHistory.removeAt(0)
        }
        
        val smoothedHeight = heightHistory.average()
        
        if (!isCalibrated) {
            calibrateBaseline(smoothedHeight)
            return
        }
        
        detectJumpPhase(smoothedHeight)
        previousHeight = smoothedHeight
    }
    
    private fun calibrateBaseline(height: Double) {
        frameCount++
        
        if (frameCount <= calibrationFrames) {
            baselineHeight = ((baselineHeight * (frameCount - 1)) + height) / frameCount
        } else {
            isCalibrated = true
            _jumpData.value = _jumpData.value.copy(phase = JumpPhase.STANDING)
        }
    }
    
    private fun detectJumpPhase(currentHeight: Double) {
        val heightDifference = currentHeight - baselineHeight
        val currentData = _jumpData.value
        
        when (currentData.phase) {
            JumpPhase.STANDING -> {
                // Detect crouch (preparing to jump)
                if (heightDifference < -jumpThreshold) {
                    _jumpData.value = currentData.copy(phase = JumpPhase.PREPARING)
                }
            }
            
            JumpPhase.PREPARING -> {
                // Detect lift-off
                if (heightDifference > jumpThreshold) {
                    jumpStartTime = System.currentTimeMillis()
                    maxHeightReached = currentHeight
                    _jumpData.value = currentData.copy(
                        phase = JumpPhase.AIRBORNE,
                        isJumping = true
                    )
                }
                // Return to standing if no jump occurs
                else if (abs(heightDifference) < landingThreshold) {
                    _jumpData.value = currentData.copy(phase = JumpPhase.STANDING)
                }
            }
            
            JumpPhase.AIRBORNE -> {
                // Track maximum height
                if (currentHeight > maxHeightReached) {
                    maxHeightReached = currentHeight
                }
                
                // Detect landing (return to near baseline)
                if (abs(currentHeight - baselineHeight) < landingThreshold) {
                    val airTime = System.currentTimeMillis() - jumpStartTime
                    val jumpHeight = (maxHeightReached - baselineHeight) * 100 // Convert to cm
                    
                    _jumpData.value = currentData.copy(
                        phase = JumpPhase.LANDING,
                        jumpHeight = jumpHeight,
                        airTime = airTime,
                        isJumping = false
                    )
                }
            }
            
            JumpPhase.LANDING -> {
                // Return to standing after brief landing phase
                if (abs(currentHeight - baselineHeight) < landingThreshold) {
                    _jumpData.value = currentData.copy(
                        phase = JumpPhase.STANDING,
                        jumpHeight = 0.0,
                        airTime = 0L
                    )
                }
            }
        }
    }
    
    private fun calculatePersonHeight(pose: Pose): Double? {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        
        if (leftHip == null || rightHip == null || leftAnkle == null || rightAnkle == null) {
            return null
        }
        
        // Calculate center point of hips and ankles
        val hipCenterY = (leftHip.position.y + rightHip.position.y) / 2
        val ankleCenterY = (leftAnkle.position.y + rightAnkle.position.y) / 2
        
        // Return the distance between hip center and ankle center
        // Note: In camera coordinates, Y increases downward, so we use ankle - hip
        return (ankleCenterY - hipCenterY).toDouble()
    }
    
    fun resetCalibration() {
        frameCount = 0
        baselineHeight = 0.0
        isCalibrated = false
        heightHistory.clear()
        _jumpData.value = JumpData()
    }
    
    fun getLastJumpData(): JumpData {
        return _jumpData.value
    }
}