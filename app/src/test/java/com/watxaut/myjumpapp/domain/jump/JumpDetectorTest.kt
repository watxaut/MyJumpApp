package com.watxaut.myjumpapp.domain.jump

import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import app.cash.turbine.test
import kotlin.math.abs

@ExperimentalCoroutinesApi
class JumpDetectorTest {

    private lateinit var jumpDetector: JumpDetector
    private lateinit var mockPose: Pose

    @Before
    fun setUp() {
        jumpDetector = JumpDetector()
        mockPose = mock()
    }

    // Test data class constructors and default values
    @Test
    fun `JumpData has correct default values`() {
        val jumpData = JumpData()
        
        assertThat(jumpData.maxHeight).isEqualTo(0.0)
        assertThat(jumpData.debugInfo.poseDetected).isFalse()
        assertThat(jumpData.debugInfo.isStable).isFalse()
        assertThat(jumpData.debugInfo.calibrationProgress).isEqualTo(0)
    }

    @Test
    fun `DebugInfo has correct default values`() {
        val debugInfo = DebugInfo()
        
        assertThat(debugInfo.poseDetected).isFalse()
        assertThat(debugInfo.isStable).isFalse()
        assertThat(debugInfo.stabilityProgress).isEqualTo(0.0f)
        assertThat(debugInfo.calibrationProgress).isEqualTo(0)
        assertThat(debugInfo.calibrationFramesNeeded).isEqualTo(30)
        assertThat(debugInfo.pixelToCmRatio).isEqualTo(1.0)
        assertThat(debugInfo.userHeightCm).isEqualTo(0.0)
    }

    // Test pose processing with no landmarks
    @Test
    fun `processPose with no landmarks should not detect pose`() = runTest {
        whenever(mockPose.allPoseLandmarks).thenReturn(emptyList())
        
        jumpDetector.jumpData.test {
            jumpDetector.processPose(mockPose)
            
            val jumpData = awaitItem()
            assertThat(jumpData.debugInfo.poseDetected).isFalse()
            assertThat(jumpData.debugInfo.landmarksDetected).isEqualTo(0)
            assertThat(jumpData.debugInfo.confidenceScore).isEqualTo(0f)
        }
    }

    // Test pose processing with landmarks but missing required hip landmarks
    @Test
    fun `processPose with landmarks but missing hips should not calibrate`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = false)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            jumpDetector.processPose(mockPose)
            
            val jumpData = awaitItem()
            assertThat(jumpData.debugInfo.poseDetected).isTrue()
            assertThat(jumpData.debugInfo.landmarksDetected).isGreaterThan(0)
            assertThat(jumpData.debugInfo.currentHipYPixels).isEqualTo(0.0) // No hip calculation possible
        }
    }

    // Test successful pose detection with full body visible
    @Test
    fun `processPose with full body should detect pose and start stability check`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            jumpDetector.processPose(mockPose)
            
            val jumpData = awaitItem()
            assertThat(jumpData.debugInfo.poseDetected).isTrue()
            assertThat(jumpData.debugInfo.landmarksDetected).isGreaterThan(30)
            assertThat(jumpData.debugInfo.confidenceScore).isGreaterThan(0.95f)
            assertThat(jumpData.debugInfo.currentHipYPixels).isGreaterThan(0.0)
        }
    }

    // Test movement stability detection
    @Test
    fun `movement stability should progress over time with steady pose`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            // Process multiple frames with same position
            repeat(35) { // Need >30 frames to start stability calculation
                jumpDetector.processPose(mockPose)
            }
            
            val jumpData = expectMostRecentItem()
            assertThat(jumpData.debugInfo.stabilityProgress).isGreaterThan(0.5f)
        }
    }

    // Test user height setting and pixel ratio calculation
    @Test
    fun `setUserHeight should calculate correct pixel-to-cm ratio`() = runTest {
        val userHeight = 170.0 // 170cm tall user
        val mockBodyHeight = 500.0 // pixels from eye to ankle
        
        // First need to calibrate to collect body measurements
        val mockLandmarks = createMockLandmarks(withHips = true, withFullBody = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            // Process enough frames to complete calibration
            repeat(65) { 
                jumpDetector.processPose(mockPose)
            }
            
            // Set user height after calibration
            jumpDetector.setUserHeight(userHeight)
            
            val jumpData = expectMostRecentItem()
            val eyeToAnkleHeight = userHeight * 0.85 // 85% of total height
            val expectedRatio = mockBodyHeight / eyeToAnkleHeight
            
            assertThat(jumpData.debugInfo.userHeightCm).isEqualTo(userHeight)
            assertThat(jumpData.debugInfo.eyeToAnkleHeightCm).isEqualTo(eyeToAnkleHeight)
            assertThat(jumpData.debugInfo.pixelToCmRatio).isWithin(0.1).of(expectedRatio)
        }
    }

    // Test calibration process
    @Test
    fun `calibration should complete after required frames`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            // Process frames to complete stability (2 seconds worth)
            repeat(35) { jumpDetector.processPose(mockPose) }
            
            var jumpData = expectMostRecentItem()
            val initialCalibrationFrames = jumpData.debugInfo.calibrationFramesNeeded
            
            // Process calibration frames 
            repeat(initialCalibrationFrames + 5) { 
                jumpDetector.processPose(mockPose)
            }
            
            jumpData = expectMostRecentItem()
            assertThat(jumpData.debugInfo.calibrationProgress).isAtLeast(initialCalibrationFrames)
        }
    }

    // Test jump height calculation
    @Test
    fun `jump height should be calculated correctly from hip movement`() = runTest {
        val baselineHipY = 400.0
        val jumpHipY = 350.0 // 50 pixels higher (lower Y value)
        val pixelToCmRatio = 3.0 // 3 pixels per cm
        val expectedJumpHeight = (baselineHipY - jumpHipY) / pixelToCmRatio
        
        // First complete calibration with baseline position
        val baselineLandmarks = createMockLandmarksWithHipY(baselineHipY, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(baselineLandmarks)
        
        jumpDetector.jumpData.test {
            // Complete calibration
            repeat(70) { jumpDetector.processPose(mockPose) }
            jumpDetector.setUserHeight(170.0)
            
            // Now simulate jump (higher position = lower Y)
            val jumpLandmarks = createMockLandmarksWithHipY(jumpHipY, highConfidence = true)
            whenever(mockPose.allPoseLandmarks).thenReturn(jumpLandmarks)
            
            jumpDetector.processPose(mockPose)
            
            val jumpData = expectMostRecentItem()
            assertThat(jumpData.maxHeight).isGreaterThan(0.0)
            assertThat(jumpData.debugInfo.hipMovementPixels).isEqualTo(abs(jumpHipY - baselineHipY))
        }
    }

    // Test reset functionality
    @Test
    fun `resetCalibration should reset all state`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.jumpData.test {
            // Process some frames first
            repeat(10) { jumpDetector.processPose(mockPose) }
            
            jumpDetector.resetCalibration()
            
            val jumpData = expectMostRecentItem()
            assertThat(jumpData.maxHeight).isEqualTo(0.0)
            assertThat(jumpData.debugInfo.calibrationProgress).isEqualTo(0)
            assertThat(jumpData.debugInfo.isStable).isFalse()
            assertThat(jumpData.debugInfo.baselineHipYPixels).isEqualTo(0.0)
        }
    }

    // Test anti-false-positive depth filtering
    @Test
    fun `depth filtering should prevent false positives from camera approach`() = runTest {
        val baselineDepth = 100.0
        val approachingDepth = 50.0 // Much closer to camera
        
        // Set up calibrated state first
        val baselineLandmarks = createMockLandmarksWithDepth(400.0, baselineDepth, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(baselineLandmarks)
        
        jumpDetector.jumpData.test {
            // Complete calibration
            repeat(70) { jumpDetector.processPose(mockPose) }
            jumpDetector.setUserHeight(170.0)
            
            // Now simulate approaching camera (closer depth, higher hip position)
            val approachingLandmarks = createMockLandmarksWithDepth(300.0, approachingDepth, highConfidence = true)
            whenever(mockPose.allPoseLandmarks).thenReturn(approachingLandmarks)
            
            jumpDetector.processPose(mockPose)
            
            val jumpData = expectMostRecentItem()
            // Should not register significant height increase due to depth filtering
            assertThat(jumpData.maxHeight).isLessThan(10.0) // Should be minimal
            assertThat(jumpData.debugInfo.depthVariation).isGreaterThan(40.0) // Large depth change detected
        }
    }

    // Helper functions to create mock data
    private fun createMockLandmarks(
        withHips: Boolean = true,
        withFullBody: Boolean = false,
        highConfidence: Boolean = true
    ): List<PoseLandmark> {
        val landmarks = mutableListOf<PoseLandmark>()
        val confidence = if (highConfidence) 0.98f else 0.5f
        
        if (withHips) {
            landmarks.add(createMockLandmark(23, 200f, 400f, 100f, confidence)) // LEFT_HIP
            landmarks.add(createMockLandmark(24, 250f, 400f, 100f, confidence)) // RIGHT_HIP
        }
        
        if (withFullBody) {
            // Add all required landmarks for full body detection
            (0..32).forEach { landmarkType ->
                if (!landmarks.any { it.landmarkType == landmarkType }) {
                    landmarks.add(createMockLandmark(landmarkType, 200f + landmarkType * 10f, 200f + landmarkType * 10f, 100f, confidence))
                }
            }
        } else {
            // Add minimum set of landmarks
            repeat(35) { i ->
                landmarks.add(createMockLandmark(i, 200f + i * 5f, 300f + i * 5f, 100f, confidence))
            }
        }
        
        return landmarks
    }
    
    private fun createMockLandmarksWithHipY(
        hipY: Double, 
        hipZ: Double = 100.0,
        highConfidence: Boolean = true
    ): List<PoseLandmark> {
        val confidence = if (highConfidence) 0.98f else 0.5f
        return listOf(
            createMockLandmark(23, 200f, hipY.toFloat(), hipZ.toFloat(), confidence), // LEFT_HIP
            createMockLandmark(24, 250f, hipY.toFloat(), hipZ.toFloat(), confidence) // RIGHT_HIP
        ) + createMockLandmarks(withHips = false, highConfidence = highConfidence)
    }
    
    private fun createMockLandmarksWithDepth(
        hipY: Double,
        hipZ: Double, 
        highConfidence: Boolean = true
    ): List<PoseLandmark> {
        return createMockLandmarksWithHipY(hipY, hipZ, highConfidence)
    }
    
    private fun createMockLandmark(
        type: Int,
        x: Float,
        y: Float, 
        z: Float,
        confidence: Float
    ): PoseLandmark {
        val landmark = mock<PoseLandmark>()
        whenever(landmark.landmarkType).thenReturn(type)
        whenever(landmark.position).thenReturn(mock {
            whenever(it.x).thenReturn(x)
            whenever(it.y).thenReturn(y)
        })
        whenever(landmark.position3D).thenReturn(mock {
            whenever(it.x).thenReturn(x)
            whenever(it.y).thenReturn(y)
            whenever(it.z).thenReturn(z)
        })
        whenever(landmark.inFrameLikelihood).thenReturn(confidence)
        return landmark
    }
    
}