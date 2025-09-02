package com.watxaut.myjumpapp.domain.jump

import android.util.Log
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
    private var calibrationFrames = 60 // Frames to establish baseline
    private var heightHistory = mutableListOf<Double>()
    private var fullBodyPixelHeights = mutableListOf<Double>() // For pixel-to-cm ratio calculation
    private var pixelToCmRatio: Double = 1.0 // Pixels per cm
    private var isCalibrated = false
    
    private val jumpThreshold = 0.3 // 30cm threshold for detecting a jump  
    private val crouchThreshold = 0.15 // 15cm threshold for detecting crouch
    private val landingThreshold = 0.05 // 5cm threshold for detecting landing
    private val smoothingWindowSize = 8
    
    fun processPose(pose: Pose) {
        val poseDetected = pose.allPoseLandmarks.isNotEmpty()
        val landmarksCount = pose.allPoseLandmarks.size
        val confidenceScore = if (poseDetected) {
            pose.allPoseLandmarks.map { it.inFrameLikelihood }.average().toFloat()
        } else 0f
        
        Log.d("JumpDetector", "Processing pose - landmarks: $landmarksCount, confidence: $confidenceScore")
        
        val currentHeight = calculatePersonHeight(pose)
        
        Log.d("JumpDetector", "Height calculation result: $currentHeight, frameCount: $frameCount, isCalibrated: $isCalibrated")
        
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
            Log.d("JumpDetector", "Height calculation returned null - missing required landmarks, cannot calibrate")
            // Only reset if person is completely gone (no landmarks detected at all)
            if (frameCount > 0 && pose.allPoseLandmarks.isEmpty()) {
                Log.d("JumpDetector", "No person detected - resetting calibration progress")
                resetCalibration()
            }
            return
        }
        
        // Smooth the height measurement
        heightHistory.add(currentHeight)
        if (heightHistory.size > smoothingWindowSize) {
            heightHistory.removeAt(0)
        }
        
        val smoothedHeight = heightHistory.average()
        
        if (!isCalibrated) {
            calibrateBaseline(smoothedHeight, pose)
            return
        }
        
        detectJumpPhase(smoothedHeight)
        previousHeight = smoothedHeight
    }
    
    private fun calibrateBaseline(height: Double, pose: Pose) {
        frameCount++
        Log.i("JumpDetector", "Calibration frame $frameCount/$calibrationFrames - height: $height")
        
        if (frameCount <= calibrationFrames) {
            baselineHeight = ((baselineHeight * (frameCount - 1)) + height) / frameCount
            Log.i("JumpDetector", "Updated baseline height: $baselineHeight")
            
            // Collect full body pixel heights in the last half of calibration (frames 31-60)
            if (frameCount > calibrationFrames / 2) {
                val fullBodyPixelHeight = calculateFullBodyPixelHeight(pose)
                if (fullBodyPixelHeight != null) {
                    fullBodyPixelHeights.add(fullBodyPixelHeight)
                    Log.d("JumpDetector", "Collected full body pixel height: $fullBodyPixelHeight (frame $frameCount)")
                }
            }
        } else {
            // Calculate pixel-to-cm ratio using collected measurements
            if (fullBodyPixelHeights.isNotEmpty()) {
                val averagePixelHeight = fullBodyPixelHeights.average()
                // This will be updated when user height is provided
                pixelToCmRatio = 1.0
                Log.i("JumpDetector", "Pixel-to-cm ratio calculated: $pixelToCmRatio (avgPixelHeight: $averagePixelHeight)")
            } else {
                Log.w("JumpDetector", "No full body measurements collected, using default ratio")
                pixelToCmRatio = 1.0
            }
            
            isCalibrated = true
            Log.i("JumpDetector", "Calibration completed! Final baseline height: $baselineHeight, pixel-to-cm ratio: $pixelToCmRatio")
            _jumpData.value = _jumpData.value.copy(phase = JumpPhase.STANDING)
        }
    }
    
    private fun detectJumpPhase(currentHeight: Double) {
        val heightDifference = currentHeight - baselineHeight
        val currentData = _jumpData.value
        
        Log.i("JumpDetector", "Detecting jump phase - current: ${currentData.phase}, height: $currentHeight, baseline: $baselineHeight, difference: $heightDifference")
        
        when (currentData.phase) {
            JumpPhase.STANDING -> {
                // Detect crouch (preparing to jump) - use separate crouch threshold
                if (heightDifference < -crouchThreshold) {
                    Log.i("JumpDetector", "STANDING -> PREPARING: Person crouched (difference: $heightDifference < -$crouchThreshold)")
                    _jumpData.value = currentData.copy(phase = JumpPhase.PREPARING)
                }
            }
            
            JumpPhase.PREPARING -> {
                // Detect lift-off
                if (heightDifference > jumpThreshold) {
                    jumpStartTime = System.currentTimeMillis()
                    maxHeightReached = currentHeight
                    Log.i("JumpDetector", "PREPARING -> AIRBORNE: Jump detected! (difference: $heightDifference > $jumpThreshold)")
                    _jumpData.value = currentData.copy(
                        phase = JumpPhase.AIRBORNE,
                        isJumping = true
                    )
                }
                // Return to standing if no jump occurs
                else if (abs(heightDifference) < landingThreshold) {
                    Log.i("JumpDetector", "PREPARING -> STANDING: Returned to standing without jumping (difference: ${abs(heightDifference)} < $landingThreshold)")
                    _jumpData.value = currentData.copy(phase = JumpPhase.STANDING)
                }
            }
            
            JumpPhase.AIRBORNE -> {
                // Track maximum height
                if (currentHeight > maxHeightReached) {
                    Log.i("JumpDetector", "AIRBORNE: New max height reached: $currentHeight (was $maxHeightReached)")
                    maxHeightReached = currentHeight
                }
                
                // Detect landing (return to near baseline)
                if (abs(currentHeight - baselineHeight) < landingThreshold) {
                    val airTime = System.currentTimeMillis() - jumpStartTime
                    val jumpHeightPixels = maxHeightReached - baselineHeight
                    val jumpHeight = jumpHeightPixels / pixelToCmRatio // Convert pixels to cm using calibrated ratio
                    
                    Log.i("JumpDetector", "AIRBORNE -> LANDING: Jump completed! Height: ${String.format("%.1f", jumpHeight)}cm, Air time: ${airTime}ms")
                    _jumpData.value = currentData.copy(
                        phase = JumpPhase.LANDING,
                        jumpHeight = jumpHeight,
                        airTime = airTime,
                        isJumping = false
                    )
                }
            }
            
            JumpPhase.LANDING -> {
                // Return to standing after brief landing phase and update baseline
                if (abs(currentHeight - baselineHeight) < landingThreshold) {
                    // Update baseline to current position (person might have moved)
                    baselineHeight = currentHeight
                    Log.i("JumpDetector", "LANDING -> STANDING: Ready for next jump, baseline updated to $baselineHeight")
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
        // Check for full body visibility with high confidence before allowing calibration
        if (!isFullBodyVisible(pose)) {
            return null
        }
        
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        
        Log.i("JumpDetector", "Required landmarks - leftHip: ${leftHip != null}, rightHip: ${rightHip != null}, leftAnkle: ${leftAnkle != null}, rightAnkle: ${rightAnkle != null}")
        
        if (leftHip == null || rightHip == null || leftAnkle == null || rightAnkle == null) {
            Log.w("JumpDetector", "Missing required landmarks for height calculation")
            return null
        }
        
        // Calculate center point of hips and ankles
        val hipCenterY = (leftHip.position.y + rightHip.position.y) / 2
        val ankleCenterY = (leftAnkle.position.y + rightAnkle.position.y) / 2
        
        val height = (ankleCenterY - hipCenterY).toDouble()
        Log.i("JumpDetector", "Calculated height: $height (hipY: $hipCenterY, ankleY: $ankleCenterY)")
        
        // Return the distance between hip center and ankle center
        // Note: In camera coordinates, Y increases downward, so we use ankle - hip
        return height
    }
    
    private fun calculateFullBodyPixelHeight(pose: Pose): Double? {
        // Get eye landmarks
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        
        if (leftEye == null || rightEye == null || leftAnkle == null || rightAnkle == null) {
            Log.w("JumpDetector", "Missing landmarks for full body height calculation")
            return null
        }
        
        // Calculate center point of eyes and ankles
        val eyeCenterY = (leftEye.position.y + rightEye.position.y) / 2
        val ankleCenterY = (leftAnkle.position.y + rightAnkle.position.y) / 2
        
        // Calculate eye-to-ankle pixel distance
        val fullBodyPixelHeight = (ankleCenterY - eyeCenterY).toDouble()
        
        Log.d("JumpDetector", "Full body pixel height: $fullBodyPixelHeight (eyeY: $eyeCenterY, ankleY: $ankleCenterY)")
        
        return fullBodyPixelHeight
    }
    
    private fun isFullBodyVisible(pose: Pose): Boolean {
        val confidenceThreshold = 0.96f

        // Print all landmarks and their confidence
        val landmarkNames = mapOf(
            PoseLandmark.NOSE to "NOSE",
            PoseLandmark.LEFT_EYE_INNER to "LEFT_EYE_INNER",
            PoseLandmark.LEFT_EYE to "LEFT_EYE",
            PoseLandmark.LEFT_EYE_OUTER to "LEFT_EYE_OUTER",
            PoseLandmark.RIGHT_EYE_INNER to "RIGHT_EYE_INNER",
            PoseLandmark.RIGHT_EYE to "RIGHT_EYE",
            PoseLandmark.RIGHT_EYE_OUTER to "RIGHT_EYE_OUTER",
            PoseLandmark.LEFT_EAR to "LEFT_EAR",
            PoseLandmark.RIGHT_EAR to "RIGHT_EAR",
            PoseLandmark.LEFT_MOUTH to "LEFT_MOUTH",
            PoseLandmark.RIGHT_MOUTH to "RIGHT_MOUTH",
            PoseLandmark.LEFT_SHOULDER to "LEFT_SHOULDER",
            PoseLandmark.RIGHT_SHOULDER to "RIGHT_SHOULDER",
            PoseLandmark.LEFT_ELBOW to "LEFT_ELBOW",
            PoseLandmark.RIGHT_ELBOW to "RIGHT_ELBOW",
            PoseLandmark.LEFT_WRIST to "LEFT_WRIST",
            PoseLandmark.RIGHT_WRIST to "RIGHT_WRIST",
            PoseLandmark.LEFT_PINKY to "LEFT_PINKY",
            PoseLandmark.RIGHT_PINKY to "RIGHT_PINKY",
            PoseLandmark.LEFT_INDEX to "LEFT_INDEX",
            PoseLandmark.RIGHT_INDEX to "RIGHT_INDEX",
            PoseLandmark.LEFT_THUMB to "LEFT_THUMB",
            PoseLandmark.RIGHT_THUMB to "RIGHT_THUMB",
            PoseLandmark.LEFT_HIP to "LEFT_HIP",
            PoseLandmark.RIGHT_HIP to "RIGHT_HIP",
            PoseLandmark.LEFT_KNEE to "LEFT_KNEE",
            PoseLandmark.RIGHT_KNEE to "RIGHT_KNEE",
            PoseLandmark.LEFT_ANKLE to "LEFT_ANKLE",
            PoseLandmark.RIGHT_ANKLE to "RIGHT_ANKLE",
            PoseLandmark.LEFT_HEEL to "LEFT_HEEL",
            PoseLandmark.RIGHT_HEEL to "RIGHT_HEEL",
            PoseLandmark.LEFT_FOOT_INDEX to "LEFT_FOOT_INDEX",
            PoseLandmark.RIGHT_FOOT_INDEX to "RIGHT_FOOT_INDEX"
        )
        
        landmarkNames.forEach { (landmarkType, name) ->
            val landmark = pose.getPoseLandmark(landmarkType)
            val confidence = landmark?.inFrameLikelihood ?: 0f
            Log.d("JumpDetector", "$name: ${String.format("%.3f", confidence)} ${if (confidence > confidenceThreshold) "✓" else "✗"}")
        }
        
        // Check if ALL landmarks are visible with confidence > 0.95
        val allLandmarksVisible = landmarkNames.all { (landmarkType, _) ->
            val landmark = pose.getPoseLandmark(landmarkType)
            val confidence = landmark?.inFrameLikelihood ?: 0f
            confidence > confidenceThreshold
        }
        
        val visibleLandmarksCount = landmarkNames.count { (landmarkType, _) ->
            val landmark = pose.getPoseLandmark(landmarkType)
            val confidence = landmark?.inFrameLikelihood ?: 0f
            confidence > confidenceThreshold
        }
        
        Log.d("JumpDetector", "=== FULL BODY CHECK (confidence > 0.95) ===")
        Log.d("JumpDetector", "Visible landmarks: $visibleLandmarksCount/${landmarkNames.size}")
        Log.d("JumpDetector", "All landmarks visible: $allLandmarksVisible")
        
        if (!allLandmarksVisible) {
            Log.d("JumpDetector", "Not all landmarks visible with sufficient confidence (>0.95) - cannot start calibration")
        }
        else {
            Log.i("JumpDetector", "All landmarks visible with sufficient confidence (>0.95) - ready for calibration!")
        }
        
        return allLandmarksVisible
    }
    
    fun setUserHeight(userHeightCm: Double) {
        if (fullBodyPixelHeights.isNotEmpty()) {
            val averagePixelHeight = fullBodyPixelHeights.average()
            // Eye-to-ankle is approximately 85% of full body height (15cm offset from eye to head top)
            val eyeToAnkleHeightCm = userHeightCm * 0.85
            pixelToCmRatio = eyeToAnkleHeightCm / averagePixelHeight
            Log.i("JumpDetector", "Updated pixel-to-cm ratio with user height: $pixelToCmRatio (userHeight: ${userHeightCm}cm, eyeToAnkle: ${eyeToAnkleHeightCm}cm, avgPixelHeight: $averagePixelHeight)")
        } else {
            Log.w("JumpDetector", "Cannot set pixel-to-cm ratio: no full body measurements available")
        }
    }
    
    fun resetCalibration() {
        frameCount = 0
        baselineHeight = 0.0
        isCalibrated = false
        heightHistory.clear()
        fullBodyPixelHeights.clear()
        pixelToCmRatio = 1.0
        _jumpData.value = JumpData()
    }
    
    fun getLastJumpData(): JumpData {
        return _jumpData.value
    }
}