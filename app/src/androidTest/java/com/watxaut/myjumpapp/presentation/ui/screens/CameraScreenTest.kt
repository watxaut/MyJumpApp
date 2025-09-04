package com.watxaut.myjumpapp.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.presentation.ui.theme.MyJumpAppTheme
import com.watxaut.myjumpapp.presentation.viewmodels.JumpSessionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val calibratingState = JumpSessionUiState(
        isSessionActive = false,
        userId = "test-user",
        userName = "Test User",
        isCalibrating = true,
        maxHeight = 0.0,
        surfaceType = SurfaceType.HARD_FLOOR,
        debugInfo = DebugInfo(
            poseDetected = false,
            isStable = false,
            stabilityProgress = 0.0f,
            calibrationProgress = 5,
            calibrationFramesNeeded = 30
        )
    )

    private val activeSessionState = JumpSessionUiState(
        isSessionActive = true,
        userId = "test-user",
        userName = "Test User",
        isCalibrating = false,
        maxHeight = 45.5,
        surfaceType = SurfaceType.HARD_FLOOR,
        debugInfo = DebugInfo(
            poseDetected = true,
            isStable = true,
            calibrationProgress = 30,
            calibrationFramesNeeded = 30,
            currentHipYPixels = 400.0,
            baselineHipYPixels = 450.0,
            hipMovementPixels = 50.0,
            totalBodyHeightPixels = 800.0,
            pixelToCmRatio = 2.5,
            userHeightCm = 170.0,
            landmarksDetected = 33,
            confidenceScore = 0.95f
        )
    )

    private val stableCalibrationState = calibratingState.copy(
        debugInfo = calibratingState.debugInfo.copy(
            poseDetected = true,
            isStable = true,
            stabilityProgress = 0.8f
        )
    )

    @Test
    fun cameraScreen_displaysUserAndSurfaceInfo() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = "test-user",
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onNavigateBack = {}
                )
            }
        }

        // Check title displays jump detection
        composeTestRule
            .onNodeWithText("Jump Detection")
            .assertIsDisplayed()

        // Check surface type icon is displayed
        composeTestRule
            .onNodeWithText("🏀") // Hard floor icon
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysCalibrationStatus() {
        // Mock the UI state for calibrating
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = calibratingState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show calibration status
        composeTestRule
            .onNodeWithText("Position yourself in camera view")
            .assertIsDisplayed()

        // Should show session will start automatically
        composeTestRule
            .onNodeWithText("Session will start automatically")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysStabilityProgress() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = stableCalibrationState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show stability progress
        composeTestRule
            .onNodeWithText("Stay steady... 80%")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysActiveSessionState() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show max height
        composeTestRule
            .onNodeWithText("45.5 cm")
            .assertIsDisplayed()

        // Should show stop session button
        composeTestRule
            .onNodeWithText("Stop Session")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysDebugInformation() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show debug information
        composeTestRule
            .onNodeWithText("Detection Status")
            .assertIsDisplayed()

        // Should show hip positions
        composeTestRule
            .onNodeWithText("400px") // Current hip Y
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("450px") // Baseline hip Y
            .assertIsDisplayed()

        // Should show movement pixels
        composeTestRule
            .onNodeWithText("50px") // Hip movement
            .assertIsDisplayed()

        // Should show pixel ratio
        composeTestRule
            .onNodeWithText("2.50") // Pixel ratio
            .assertIsDisplayed()

        // Should show user height
        composeTestRule
            .onNodeWithText("170cm") // User height
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysMaxHeightInCenter() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState.copy(maxHeight = 55.2),
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show max height in center overlay
        composeTestRule
            .onNodeWithText("55.2 cm")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Max Height (Hard Floor)")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_stopSessionButtonWorks() {
        var stopSessionCalled = false

        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = { stopSessionCalled = true },
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Click stop session button
        composeTestRule
            .onNodeWithText("Stop Session")
            .performClick()

        assert(stopSessionCalled)
    }

    @Test
    fun cameraScreen_backButtonWorks() {
        var backPressed = false

        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = "test-user",
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onNavigateBack = { backPressed = true }
                )
            }
        }

        // Click back button
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assert(backPressed)
    }

    @Test
    fun cameraScreen_displaysSetupGuideButton() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = "test-user",
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onNavigateBack = {}
                )
            }
        }

        // Should have setup guide button
        composeTestRule
            .onNodeWithContentDescription("Setup Guide")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysSurfaceTypeInformation() {
        // Test Hard Floor surface
        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = "test-user",
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Test User • Hard Floor")
            .assertIsDisplayed()

        // Test Sand surface
        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = "test-user",
                    surfaceType = SurfaceType.SAND,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("🏖️") // Sand icon
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Test User • Sand")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_handlesNullUserId() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                CameraScreen(
                    userId = null,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onNavigateBack = {}
                )
            }
        }

        // Should show message to select user first
        composeTestRule
            .onNodeWithText("Please select a user first")
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysConfidenceScore() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show confidence percentage
        composeTestRule
            .onNodeWithText("95%") // Confidence score
            .assertIsDisplayed()
    }

    @Test
    fun cameraScreen_displaysLandmarkCount() {
        composeTestRule.setContent {
            MyJumpAppTheme {
                JumpDetectionContent(
                    uiState = activeSessionState,
                    surfaceType = SurfaceType.HARD_FLOOR,
                    onShowGuide = {},
                    onStopSession = {},
                    onPoseDetected = { _, _ -> },
                    modifier = androidx.compose.ui.Modifier
                )
            }
        }

        // Should show landmark count
        composeTestRule
            .onNodeWithText("33") // Landmarks detected
            .assertIsDisplayed()
    }
}