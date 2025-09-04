package com.watxaut.myjumpapp.presentation.viewmodels

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.pose.Pose
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.repository.JumpRepository
import com.watxaut.myjumpapp.data.repository.JumpSessionRepository
import com.watxaut.myjumpapp.data.repository.UserRepository
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.domain.jump.JumpData
import com.watxaut.myjumpapp.domain.jump.JumpDetector
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.utils.WakeLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class JumpSessionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: JumpSessionViewModel
    private lateinit var mockJumpDetector: JumpDetector
    private lateinit var mockJumpRepository: JumpRepository
    private lateinit var mockJumpSessionRepository: JumpSessionRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockWakeLockManager: WakeLockManager
    private lateinit var mockContext: Context
    private lateinit var mockPose: Pose

    private val testUserId = "test-user-123"
    private val testUser = User(
        userId = testUserId,
        userName = "Test User",
        heightCm = 170,
        bestJumpHeight = 50.0,
        totalJumps = 5,
        bestJumpHeightHardFloor = 55.0,
        bestJumpHeightSand = 45.0,
        totalSessionsHardFloor = 3,
        totalSessionsSand = 2,
        totalJumpsHardFloor = 3,
        totalJumpsSand = 2
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        mockJumpDetector = mock()
        mockJumpRepository = mock()
        mockJumpSessionRepository = mock()
        mockUserRepository = mock()
        mockWakeLockManager = mock()
        mockContext = mock()
        mockPose = mock()

        // Mock JumpDetector flow
        val jumpDataFlow = MutableStateFlow(JumpData())
        whenever(mockJumpDetector.jumpData).thenReturn(jumpDataFlow)

        viewModel = JumpSessionViewModel(
            jumpDetector = mockJumpDetector,
            jumpRepository = mockJumpRepository,
            jumpSessionRepository = mockJumpSessionRepository,
            userRepository = mockUserRepository,
            wakeLockManager = mockWakeLockManager,
            context = mockContext
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Test initial state
    @Test
    fun `viewModel should have correct initial state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            
            assertThat(initialState.isSessionActive).isFalse()
            assertThat(initialState.userId).isNull()
            assertThat(initialState.userName).isEmpty()
            assertThat(initialState.isCalibrating).isTrue()
            assertThat(initialState.maxHeight).isEqualTo(0.0)
            assertThat(initialState.surfaceType).isEqualTo(SurfaceType.HARD_FLOOR)
            assertThat(initialState.error).isNull()
        }
    }

    // Test user ID setting
    @Test
    fun `setUserId should update UI state with user information`() = runTest {
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)

        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.setUserId(testUserId)
            
            val updatedState = awaitItem()
            assertThat(updatedState.userId).isEqualTo(testUserId)
            assertThat(updatedState.userName).isEqualTo("Test User")
        }
    }

    @Test
    fun `setUserId with non-existent user should not update state`() = runTest {
        whenever(mockUserRepository.getUserById("invalid-id")).thenReturn(null)

        viewModel.uiState.test {
            val initialState = awaitItem()
            
            viewModel.setUserId("invalid-id")
            
            // Should not emit new state since user not found
            expectNoEvents()
        }
    }

    // Test surface type setting
    @Test
    fun `setSurfaceType should update surface type in UI state`() = runTest {
        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.setSurfaceType(SurfaceType.SAND)
            
            val updatedState = awaitItem()
            assertThat(updatedState.surfaceType).isEqualTo(SurfaceType.SAND)
        }
    }

    // Test session start
    @Test
    fun `startSession should create session and update state`() = runTest {
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        
        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.startSession(testUserId, SurfaceType.HARD_FLOOR)
            
            val updatedState = awaitItem()
            assertThat(updatedState.isSessionActive).isTrue()
            assertThat(updatedState.userId).isEqualTo(testUserId)
            assertThat(updatedState.userName).isEqualTo("Test User")
            assertThat(updatedState.surfaceType).isEqualTo(SurfaceType.HARD_FLOOR)
            
            verify(mockJumpSessionRepository).insertSession(any())
            verify(mockWakeLockManager).acquireWakeLock(mockContext)
        }
    }

    @Test
    fun `startSession with invalid user should set error state`() = runTest {
        whenever(mockUserRepository.getUserById("invalid-id")).thenReturn(null)
        
        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.startSession("invalid-id", SurfaceType.HARD_FLOOR)
            
            val errorState = awaitItem()
            assertThat(errorState.error).isEqualTo("User not found")
            assertThat(errorState.isSessionActive).isFalse()
        }
    }

    // Test session stop
    @Test
    fun `stopSession should complete session and update user statistics`() = runTest {
        // First start a session
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        
        viewModel.setUserId(testUserId)
        viewModel.startSession(testUserId, SurfaceType.HARD_FLOOR)
        
        // Mock the current session for stopSession
        val testSession = JumpSession(
            sessionId = "test-session",
            userId = testUserId,
            sessionName = "Test Session",
            startTime = System.currentTimeMillis(),
            surfaceType = SurfaceType.HARD_FLOOR,
            isCompleted = false
        )

        viewModel.uiState.test {
            // Skip previous states
            skipItems(2)
            
            viewModel.stopSession()
            
            val finalState = awaitItem()
            assertThat(finalState.isSessionActive).isFalse()
            assertThat(finalState.isCalibrating).isTrue()
            
            verify(mockJumpSessionRepository).updateSession(any())
            verify(mockUserRepository).updateUserStats(eq(testUserId), any(), any())
            verify(mockUserRepository).updateSurfaceSpecificStats(any(), any(), any(), any(), any(), any(), any())
            verify(mockWakeLockManager).releaseWakeLock(mockContext)
            verify(mockJumpDetector).resetCalibration()
        }
    }

    // Test pose processing
    @Test
    fun `processPoseDetection should process pose when session active`() = runTest {
        val mockImageProxy = mock<androidx.camera.core.ImageProxy>()
        
        // Start a session first
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        viewModel.startSession(testUserId, SurfaceType.HARD_FLOOR)
        
        viewModel.processPoseDetection(mockImageProxy, mockPose)
        
        verify(mockJumpDetector).processPose(mockPose)
    }

    @Test
    fun `processPoseDetection should process pose when calibrating`() = runTest {
        val mockImageProxy = mock<androidx.camera.core.ImageProxy>()
        
        // ViewModel starts in calibrating state
        viewModel.processPoseDetection(mockImageProxy, mockPose)
        
        verify(mockJumpDetector).processPose(mockPose)
    }

    @Test
    fun `processPoseDetection should not process pose when inactive and not calibrating`() = runTest {
        val mockImageProxy = mock<androidx.camera.core.ImageProxy>()
        
        // Simulate completed calibration but no active session
        val jumpDataFlow = MutableStateFlow(
            JumpData(debugInfo = DebugInfo(calibrationProgress = 30, calibrationFramesNeeded = 30))
        )
        whenever(mockJumpDetector.jumpData).thenReturn(jumpDataFlow)
        
        viewModel.processPoseDetection(mockImageProxy, mockPose)
        
        verify(mockJumpDetector, never()).processPose(mockPose)
    }

    // Test jump data flow integration
    @Test
    fun `jump data changes should update UI state`() = runTest {
        val jumpDataFlow = MutableStateFlow(JumpData())
        whenever(mockJumpDetector.jumpData).thenReturn(jumpDataFlow)
        
        val newViewModel = JumpSessionViewModel(
            jumpDetector = mockJumpDetector,
            jumpRepository = mockJumpRepository,
            jumpSessionRepository = mockJumpSessionRepository,
            userRepository = mockUserRepository,
            wakeLockManager = mockWakeLockManager,
            context = mockContext
        )
        
        newViewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.maxHeight).isEqualTo(0.0)
            
            // Simulate jump data update
            jumpDataFlow.value = JumpData(
                maxHeight = 55.0,
                debugInfo = DebugInfo(poseDetected = true, isStable = true)
            )
            
            val updatedState = awaitItem()
            assertThat(updatedState.maxHeight).isEqualTo(55.0)
            assertThat(updatedState.debugInfo.poseDetected).isTrue()
            assertThat(updatedState.debugInfo.isStable).isTrue()
        }
    }

    // Test auto-start session after calibration
    @Test
    fun `should auto-start session when calibration completes`() = runTest {
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        
        val jumpDataFlow = MutableStateFlow(JumpData())
        whenever(mockJumpDetector.jumpData).thenReturn(jumpDataFlow)
        
        val newViewModel = JumpSessionViewModel(
            jumpDetector = mockJumpDetector,
            jumpRepository = mockJumpRepository,
            jumpSessionRepository = mockJumpSessionRepository,
            userRepository = mockUserRepository,
            wakeLockManager = mockWakeLockManager,
            context = mockContext
        )
        
        // Set user first
        newViewModel.setUserId(testUserId)
        
        newViewModel.uiState.test {
            // Skip initial states
            skipItems(2)
            
            // Simulate calibration completion (was calibrating, now not calibrating)
            jumpDataFlow.value = JumpData(
                debugInfo = DebugInfo(
                    calibrationProgress = 30,
                    calibrationFramesNeeded = 30,
                    poseDetected = true,
                    isStable = true
                )
            )
            
            val autoStartedState = awaitItem()
            assertThat(autoStartedState.isSessionActive).isTrue()
            
            verify(mockJumpDetector).setUserHeight(170.0) // User's height
            verify(mockJumpSessionRepository).insertSession(any())
        }
    }

    // Test reset to initial state
    @Test
    fun `resetToInitialState should reset all state and stop session`() = runTest {
        // Start a session first
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        viewModel.startSession(testUserId, SurfaceType.HARD_FLOOR)
        
        viewModel.uiState.test {
            // Skip initial states
            skipItems(2)
            
            viewModel.resetToInitialState()
            
            val resetState = awaitItem()
            assertThat(resetState.isSessionActive).isFalse()
            assertThat(resetState.userId).isNull()
            assertThat(resetState.userName).isEmpty()
            assertThat(resetState.isCalibrating).isTrue()
            assertThat(resetState.maxHeight).isEqualTo(0.0)
            
            verify(mockWakeLockManager).releaseWakeLock(mockContext)
            verify(mockJumpDetector).resetCalibration()
        }
    }

    // Test error clearing
    @Test
    fun `clearError should remove error from state`() = runTest {
        whenever(mockUserRepository.getUserById("invalid-id")).thenReturn(null)
        
        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            // Trigger an error
            viewModel.startSession("invalid-id", SurfaceType.HARD_FLOOR)
            val errorState = awaitItem()
            assertThat(errorState.error).isNotNull()
            
            // Clear the error
            viewModel.clearError()
            val clearedState = awaitItem()
            assertThat(clearedState.error).isNull()
        }
    }

    // Test session duration updates
    @Test
    fun `session duration should update when session is active`() = runTest {
        val mockImageProxy = mock<androidx.camera.core.ImageProxy>()
        
        whenever(mockUserRepository.getUserById(testUserId)).thenReturn(testUser)
        
        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.startSession(testUserId, SurfaceType.HARD_FLOOR)
            val sessionStartedState = awaitItem()
            
            // Process pose detection which should update duration
            viewModel.processPoseDetection(mockImageProxy, mockPose)
            
            val durationUpdatedState = awaitItem()
            assertThat(durationUpdatedState.sessionDuration).isGreaterThan(0L)
        }
    }
}