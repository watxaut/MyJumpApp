package com.watxaut.myjumpapp.domain.jump

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class JumpDetectorPositionWarningTest {

    @Mock
    private lateinit var mockPose: Pose

    private lateinit var jumpDetector: JumpDetector

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        jumpDetector = JumpDetector()
    }

    @Test
    fun `position warning should be null during calibration`() = runTest {
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)

        jumpDetector.jumpData.test {
            jumpDetector.processPose(mockPose)
            
            val jumpData = expectMostRecentItem()
            assertThat(jumpData.debugInfo.positionWarning).isNull()
        }
    }

    @Test
    fun `position warning should appear when user moves forward from baseline`() = runTest {
        // First calibrate with stable position
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.setUserHeight(170.0) // Set user height first
        
        jumpDetector.jumpData.test {
            // Calibrate by processing many frames with same position  
            repeat(70) { // More than calibrationFrames (60)
                jumpDetector.processPose(mockPose)
            }
            
            // Wait for calibration to complete
            val calibratedState = expectMostRecentItem()
            assertThat(calibratedState.debugInfo.calibrationProgress).isEqualTo(60)
            
            // Now move significantly forward (increase Z depth)
            val forwardLandmarks = createMockLandmarksWithDepth(400.0, 200.0) // Much closer Z
            whenever(mockPose.allPoseLandmarks).thenReturn(forwardLandmarks)
            
            jumpDetector.processPose(mockPose)
            
            val warningState = expectMostRecentItem()
            assertThat(warningState.debugInfo.positionWarning).isNotNull()
            assertThat(warningState.debugInfo.positionWarning).contains("position")
        }
    }

    @Test
    fun `position warning should be null when depth is within acceptable range`() = runTest {
        // Similar test but with acceptable depth change
        val mockLandmarks = createMockLandmarks(withHips = true, highConfidence = true)
        whenever(mockPose.allPoseLandmarks).thenReturn(mockLandmarks)
        
        jumpDetector.setUserHeight(170.0)
        
        jumpDetector.jumpData.test {
            // Calibrate
            repeat(70) {
                jumpDetector.processPose(mockPose)
            }
            
            val calibratedState = expectMostRecentItem()
            assertThat(calibratedState.debugInfo.calibrationProgress).isEqualTo(60)
            
            // Small acceptable depth change
            val nearbyLandmarks = createMockLandmarksWithDepth(400.0, 110.0) // Small Z change
            whenever(mockPose.allPoseLandmarks).thenReturn(nearbyLandmarks)
            
            jumpDetector.processPose(mockPose)
            
            val noWarningState = expectMostRecentItem()
            assertThat(noWarningState.debugInfo.positionWarning).isNull()
        }
    }

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

    private fun createMockLandmarksWithDepth(
        hipY: Double,
        hipZ: Double,
        highConfidence: Boolean = true
    ): List<PoseLandmark> {
        val confidence = if (highConfidence) 0.98f else 0.5f
        return listOf(
            createMockLandmark(23, 200f, hipY.toFloat(), hipZ.toFloat(), confidence), // LEFT_HIP
            createMockLandmark(24, 250f, hipY.toFloat(), hipZ.toFloat(), confidence)  // RIGHT_HIP
        ) + createMockLandmarks(withHips = false, highConfidence = highConfidence)
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