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
    val maxHeight: Double = 0.0,
    val debugInfo: DebugInfo = DebugInfo()
)

data class DebugInfo(
    val poseDetected: Boolean = false,
    val isStable: Boolean = false,
    val stabilityProgress: Float = 0.0f,
    val calibrationProgress: Int = 0,
    val calibrationFramesNeeded: Int = 30,
    val currentHeight: Double = 0.0,
    val baselineHeight: Double = 0.0,
    val heightDifference: Double = 0.0,
    val landmarksDetected: Int = 0,
    val confidenceScore: Float = 0f,
    val hipHeightCm: Double = 0.0,
    val currentDepth: Double = 0.0,
    val baselineDepth: Double = 0.0,
    val depthVariation: Double = 0.0,
    // New debug measurements
    val currentHipYPixels: Double = 0.0,
    val baselineHipYPixels: Double = 0.0,
    val hipMovementPixels: Double = 0.0,
    val totalBodyHeightPixels: Double = 0.0,
    val pixelToCmRatio: Double = 1.0,
    val userHeightCm: Double = 0.0,
    val eyeToAnkleHeightCm: Double = 0.0
)


@Singleton
class JumpDetector @Inject constructor() {
    
    private val _jumpData = MutableStateFlow(JumpData())
    val jumpData: StateFlow<JumpData> = _jumpData.asStateFlow()
    
    private var baselineHeight: Double = 0.0
    private var baselineDepth: Double = 0.0
    private var maxHeightReached: Double = 0.0
    private var frameCount = 0
    private var calibrationFrames = 60 // Frames to establish baseline
    private var heightHistory = mutableListOf<Double>()
    private var fullBodyPixelHeights = mutableListOf<Double>() // For pixel-to-cm ratio calculation
    private var pixelToCmRatio: Double = 1.0 // Pixels per cm
    private var isCalibrated = false
    private var userHeightCm: Double = 0.0
    
    // Movement stability detection
    private var isStable = false
    private var stabilityStartTime: Long = 0
    private val stabilityRequiredMs = 2000L // 2 seconds of stability required
    private val movementHistory = mutableListOf<Pair<Double, Double>>() // (hipY, hipZ) pairs
    private val stabilityThreshold = 15.0 // pixels
    private val maxMovementHistorySize = 60 // ~2 seconds at 30fps
    
    // Anti-false-positive measures
    private var depthHistory = mutableListOf<Double>()
    private val depthThreshold = 50.0 // pixels - max allowed depth variation
    private val maxDepthHistorySize = 30
    private var averageDepth: Double = 0.0
    
    private val smoothingWindowSize = 5 // Reduced for better responsiveness
    
    fun processPose(pose: Pose) {
        val poseDetected = pose.allPoseLandmarks.isNotEmpty()
        val landmarksCount = pose.allPoseLandmarks.size
        val confidenceScore = if (poseDetected) {
            pose.allPoseLandmarks.map { it.inFrameLikelihood }.average().toFloat()
        } else 0f
        
        Log.d("JumpDetector", "Processing pose - landmarks: $landmarksCount, confidence: $confidenceScore")
        
        val currentHipY = calculateHipCenterY(pose)
        val currentHipZ = calculateHipCenterZ(pose)
        
        Log.d("JumpDetector", "Hip Y calculation result: $currentHipY, frameCount: $frameCount, isCalibrated: $isCalibrated")
        Log.d("JumpDetector", "Hip Z calculation result: $currentHipZ")
        
        // Check movement stability before calibration
        val (stabilityStatus, stabilityProgress) = checkMovementStability(currentHipY, currentHipZ)
        
        // Update depth tracking
        val currentDepth = currentHipZ ?: 0.0
        val depthVariation = if (isCalibrated && baselineDepth != 0.0) {
            abs(currentDepth - baselineDepth)
        } else 0.0
        
        // Calculate debug values - need to do this before calibration check
        val hipHeightCm = if (currentHipY != null && isCalibrated) {
            val pixelDifference = currentHipY - baselineHeight
            pixelDifference / pixelToCmRatio
        } else 0.0
        
        // Calculate additional debug measurements
        val currentHipYPixels = currentHipY ?: 0.0
        val hipMovementPixels = if (isCalibrated && currentHipY != null) {
            abs(currentHipY - baselineHeight)
        } else 0.0
        
        val totalBodyHeightPixels = if (fullBodyPixelHeights.isNotEmpty()) {
            fullBodyPixelHeights.average()
        } else {
            calculateFullBodyPixelHeight(pose) ?: 0.0
        }
        
        val eyeToAnkleHeightCm = if (userHeightCm > 0) userHeightCm * 0.85 else 0.0
        
        val debugInfo = DebugInfo(
            poseDetected = poseDetected,
            isStable = stabilityStatus,
            stabilityProgress = stabilityProgress,
            calibrationProgress = frameCount,
            calibrationFramesNeeded = calibrationFrames,
            currentHeight = currentHipY ?: 0.0,
            baselineHeight = baselineHeight,
            heightDifference = hipHeightCm,
            landmarksDetected = landmarksCount,
            confidenceScore = confidenceScore,
            hipHeightCm = hipHeightCm,
            currentDepth = currentDepth,
            baselineDepth = baselineDepth,
            depthVariation = depthVariation,
            // New debug measurements
            currentHipYPixels = currentHipYPixels,
            baselineHipYPixels = baselineHeight,
            hipMovementPixels = hipMovementPixels,
            totalBodyHeightPixels = totalBodyHeightPixels,
            pixelToCmRatio = pixelToCmRatio,
            userHeightCm = userHeightCm,
            eyeToAnkleHeightCm = eyeToAnkleHeightCm
        )
        
        // Update debug info immediately
        _jumpData.value = _jumpData.value.copy(debugInfo = debugInfo)
        
        if (currentHipY == null) {
            Log.d("JumpDetector", "Hip Y calculation returned null - missing required landmarks, cannot calibrate")
            // Only reset if person is completely gone (no landmarks detected at all)
            if (frameCount > 0 && pose.allPoseLandmarks.isEmpty()) {
                Log.d("JumpDetector", "No person detected - resetting calibration progress")
                resetCalibration()
            }
            return
        }
        
        // Smooth the hip Y measurement
        heightHistory.add(currentHipY)
        if (heightHistory.size > smoothingWindowSize) {
            heightHistory.removeAt(0)
        }
        
        val smoothedHipY = heightHistory.average()
        
        if (!isCalibrated) {
            // Only start calibration once the person is stable
            if (stabilityStatus) {
                calibrateBaseline(smoothedHipY, currentHipZ ?: 0.0, pose)
            }
            return
        }
        
        // Anti-false-positive filtering for calibrated state
        if (!isValidJumpMovement(smoothedHipY, currentDepth)) {
            Log.d("JumpDetector", "Invalid movement detected - ignoring frame")
            return
        }
        
        // Track maximum height (lowest Y value since Y increases downward)
        if (smoothedHipY < maxHeightReached || maxHeightReached == 0.0) {
            maxHeightReached = smoothedHipY
            val maxHeightCm = abs(maxHeightReached - baselineHeight) / pixelToCmRatio
            Log.d("JumpDetector", "New max height reached: ${String.format("%.1f", maxHeightCm)}cm")
            
            // Update jump data with new maximum height
            _jumpData.value = _jumpData.value.copy(
                maxHeight = maxHeightCm
            )
        }
    }
    
    private fun calibrateBaseline(height: Double, depth: Double, pose: Pose) {
        frameCount++
        Log.i("JumpDetector", "Calibration frame $frameCount/$calibrationFrames - height: $height")
        
        if (frameCount <= calibrationFrames) {
            baselineHeight = ((baselineHeight * (frameCount - 1)) + height) / frameCount
            baselineDepth = ((baselineDepth * (frameCount - 1)) + depth) / frameCount
            Log.i("JumpDetector", "Updated baseline height: $baselineHeight, depth: $baselineDepth")
            
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
            maxHeightReached = baselineHeight // Initialize max height to baseline
            Log.i("JumpDetector", "Calibration completed! Final baseline height: $baselineHeight, pixel-to-cm ratio: $pixelToCmRatio")
        }
    }
    
    
    private fun calculateHipCenterY(pose: Pose): Double? {
        // Check for full body visibility with high confidence before allowing calibration
        if (!isFullBodyVisible(pose)) {
            return null
        }
        
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        
        Log.d("JumpDetector", "Required landmarks - leftHip: ${leftHip != null}, rightHip: ${rightHip != null}")
        
        if (leftHip == null || rightHip == null) {
            Log.w("JumpDetector", "Missing hip landmarks for height calculation")
            return null
        }
        
        // Calculate center point of hips - this is our primary tracking point
        val hipCenterY = (leftHip.position.y + rightHip.position.y) / 2
        
        Log.d("JumpDetector", "Hip center Y: $hipCenterY (leftHip: ${leftHip.position.y}, rightHip: ${rightHip.position.y})")
        
        return hipCenterY.toDouble()
    }
    
    private fun calculateHipCenterZ(pose: Pose): Double? {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        
        if (leftHip == null || rightHip == null) {
            return null
        }
        
        // Calculate center point of hips depth (Z coordinate)
        val hipCenterZ = (leftHip.position3D.z + rightHip.position3D.z) / 2
        
        Log.d("JumpDetector", "Hip center Z: $hipCenterZ (leftHip: ${leftHip.position3D.z}, rightHip: ${rightHip.position3D.z})")
        
        return hipCenterZ.toDouble()
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
        this.userHeightCm = userHeightCm
        if (fullBodyPixelHeights.isNotEmpty()) {
            val averagePixelHeight = fullBodyPixelHeights.average()
            // Eye-to-ankle is approximately 85% of full body height (15cm offset from eye to head top)
            val eyeToAnkleHeightCm = userHeightCm * 0.85
            pixelToCmRatio = averagePixelHeight / eyeToAnkleHeightCm
            Log.i("JumpDetector", "Updated pixel-to-cm ratio with user height: $pixelToCmRatio (userHeight: ${userHeightCm}cm, eyeToAnkle: ${eyeToAnkleHeightCm}cm, avgPixelHeight: $averagePixelHeight)")
        } else {
            Log.w("JumpDetector", "Cannot set pixel-to-cm ratio: no full body measurements available")
        }
    }
    
    fun resetCalibration() {
        frameCount = 0
        baselineHeight = 0.0
        baselineDepth = 0.0
        maxHeightReached = 0.0
        isCalibrated = false
        isStable = false
        stabilityStartTime = 0
        heightHistory.clear()
        fullBodyPixelHeights.clear()
        movementHistory.clear()
        depthHistory.clear()
        pixelToCmRatio = 1.0
        averageDepth = 0.0
        // Note: Don't reset userHeightCm as it should persist across sessions
        _jumpData.value = JumpData()
    }
    
    /**
     * Check if the person is stable enough to start calibration.
     * Requires 2 seconds of minimal movement in both Y and Z axes.
     * Returns (isStable, progressPercent)
     */
    private fun checkMovementStability(hipY: Double?, hipZ: Double?): Pair<Boolean, Float> {
        if (hipY == null || hipZ == null) {
            isStable = false
            stabilityStartTime = 0
            movementHistory.clear()
            return Pair(false, 0.0f)
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Add current position to history
        movementHistory.add(Pair(hipY, hipZ))
        if (movementHistory.size > maxMovementHistorySize) {
            movementHistory.removeAt(0)
        }
        
        // Need at least 30 frames (~1 second) to assess stability
        if (movementHistory.size < 30) {
            return Pair(false, movementHistory.size / 30.0f * 0.5f)
        }
        
        // Calculate movement variation in the recent history
        val recentPositions = movementHistory.takeLast(30) // Last 1 second
        val avgY = recentPositions.map { it.first }.average()
        val avgZ = recentPositions.map { it.second }.average()
        
        val maxYVariation = recentPositions.map { abs(it.first - avgY) }.maxOrNull() ?: Double.MAX_VALUE
        val maxZVariation = recentPositions.map { abs(it.second - avgZ) }.maxOrNull() ?: Double.MAX_VALUE
        
        val isCurrentlyStable = maxYVariation < stabilityThreshold && maxZVariation < stabilityThreshold / 2
        
        if (isCurrentlyStable) {
            if (!isStable) {
                // Just became stable - start the timer
                stabilityStartTime = currentTime
                isStable = true
                Log.d("JumpDetector", "Movement became stable - starting stability timer")
            }
            
            val timeStable = currentTime - stabilityStartTime
            val progress = (timeStable.toFloat() / stabilityRequiredMs.toFloat()).coerceAtMost(1.0f)
            val isStableEnough = timeStable >= stabilityRequiredMs
            
            Log.d("JumpDetector", "Stable for ${timeStable}ms (${String.format("%.1f", progress * 100)}%) - Y var: ${String.format("%.1f", maxYVariation)}, Z var: ${String.format("%.1f", maxZVariation)}")
            
            return Pair(isStableEnough, 0.5f + progress * 0.5f) // 50% for initial detection, 50% for time
        } else {
            // Lost stability - reset timer
            if (isStable) {
                Log.d("JumpDetector", "Movement stability lost - resetting timer. Y var: ${String.format("%.1f", maxYVariation)}, Z var: ${String.format("%.1f", maxZVariation)}")
            }
            isStable = false
            stabilityStartTime = 0
            return Pair(false, movementHistory.size / 60.0f * 0.5f) // Only show detection progress
        }
    }
    
    /**
     * Validate that the detected movement is a legitimate vertical jump and not:
     * 1. Person moving horizontally toward/away from camera
     * 2. Person bending/crouching while approaching
     * 3. Camera/phone movement
     */
    private fun isValidJumpMovement(hipY: Double, hipZ: Double): Boolean {
        // Method 1: Depth filtering - ensure person maintains roughly same distance from camera
        depthHistory.add(hipZ)
        if (depthHistory.size > maxDepthHistorySize) {
            depthHistory.removeAt(0)
        }
        
        if (depthHistory.size >= 10) {
            averageDepth = depthHistory.average()
            val depthVariation = abs(hipZ - averageDepth)
            
            if (depthVariation > depthThreshold) {
                Log.d("JumpDetector", "Depth variation too high: ${String.format("%.1f", depthVariation)} > $depthThreshold - person likely approaching/moving away")
                return false
            }
        }
        
        // Method 2: Baseline depth comparison - person should stay at roughly same depth as during calibration  
        val depthDifference = abs(hipZ - baselineDepth)
        if (depthDifference > depthThreshold) {
            Log.d("JumpDetector", "Depth changed significantly from baseline: ${String.format("%.1f", depthDifference)} > $depthThreshold")
            return false
        }
        
        // Method 3: Average Z position filtering - use running average to filter out approach movements
        if (averageDepth != 0.0) {
            val avgDepthDifference = abs(hipZ - averageDepth)
            if (avgDepthDifference > depthThreshold * 0.7) { // Slightly more lenient than individual frame
                Log.d("JumpDetector", "Average depth difference too high: ${String.format("%.1f", avgDepthDifference)} > ${depthThreshold * 0.7}")
                return false
            }
        }
        
        // All checks passed - this appears to be valid vertical movement
        return true
    }
    
    fun getLastJumpData(): JumpData {
        return _jumpData.value
    }
}