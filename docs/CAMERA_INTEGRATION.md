# MyJumpApp - Camera Integration & Jump Detection

## Overview

This document details the camera integration architecture and jump detection algorithms for MyJumpApp. The system uses Android's Camera2 API combined with ML Kit for computer vision to accurately measure vertical jump height in real-time.

## Camera System Architecture

### Camera2 API Integration

#### Camera Configuration
```kotlin
class CameraManager(private val context: Context) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    
    companion object {
        const val PREVIEW_WIDTH = 1920
        const val PREVIEW_HEIGHT = 1080
        const val TARGET_FPS = 60 // High frame rate for motion detection
    }
    
    fun openCamera(surfaceView: SurfaceView, onReady: () -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraId = getCameraId() // Back-facing camera
        
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCaptureSession(surfaceView, onReady)
            }
            
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }
            
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, backgroundHandler)
    }
}
```

#### Capture Session Setup
```kotlin
private fun createCaptureSession(surfaceView: SurfaceView, onReady: () -> Unit) {
    val surface = surfaceView.holder.surface
    
    // Setup ImageReader for frame analysis
    imageReader = ImageReader.newInstance(
        PREVIEW_WIDTH, PREVIEW_HEIGHT, 
        ImageFormat.YUV_420_888, 2
    ).apply {
        setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            processFrame(image)
            image?.close()
        }, backgroundHandler)
    }
    
    val surfaces = listOf(surface, imageReader!!.surface)
    
    cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview(surface)
            onReady()
        }
        
        override fun onConfigureFailed(session: CameraCaptureSession) {
            // Handle error
        }
    }, backgroundHandler)
}
```

## Jump Detection Algorithm

### Computer Vision Pipeline

The jump detection system follows a multi-stage approach:

1. **Frame Preprocessing**
2. **Motion Detection**
3. **Person Detection & Tracking**
4. **Jump Event Detection**
5. **Height Calculation**

### Stage 1: Frame Preprocessing

```kotlin
class FrameProcessor {
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    
    fun preprocessFrame(image: Image): Bitmap {
        // Convert YUV_420_888 to RGB bitmap
        val bitmap = Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        )
        yuvToRgbConverter.yuvToRgb(image, bitmap)
        
        // Apply preprocessing filters
        return bitmap
            .let { applyGaussianBlur(it, 1.0f) }
            .let { enhanceContrast(it, 1.2f) }
            .let { cropToDetectionZone(it) }
    }
    
    private fun cropToDetectionZone(bitmap: Bitmap): Bitmap {
        // Crop to central 70% of frame to focus on jump area
        val cropWidth = (bitmap.width * 0.7).toInt()
        val cropHeight = (bitmap.height * 0.8).toInt()
        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 4 // Slightly above center
        
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }
}
```

### Stage 2: Motion Detection

```kotlin
class MotionDetector {
    private var previousFrame: Bitmap? = null
    private val motionThreshold = 30 // Pixel intensity difference threshold
    
    fun detectMotion(currentFrame: Bitmap): MotionResult {
        val previousFrame = this.previousFrame
        this.previousFrame = currentFrame.copy(currentFrame.config, false)
        
        if (previousFrame == null) {
            return MotionResult(hasMotion = false, intensity = 0.0)
        }
        
        val motionIntensity = calculateFrameDifference(previousFrame, currentFrame)
        val hasSignificantMotion = motionIntensity > motionThreshold
        
        return MotionResult(
            hasMotion = hasSignificantMotion,
            intensity = motionIntensity,
            motionRegions = if (hasSignificantMotion) findMotionRegions(previousFrame, currentFrame) else emptyList()
        )
    }
    
    private fun calculateFrameDifference(frame1: Bitmap, frame2: Bitmap): Double {
        val width = frame1.width
        val height = frame1.height
        var totalDifference = 0.0
        var pixelCount = 0
        
        for (x in 0 until width step 4) { // Sample every 4th pixel for performance
            for (y in 0 until height step 4) {
                val pixel1 = frame1.getPixel(x, y)
                val pixel2 = frame2.getPixel(x, y)
                
                val r1 = Color.red(pixel1)
                val g1 = Color.green(pixel1)
                val b1 = Color.blue(pixel1)
                
                val r2 = Color.red(pixel2)
                val g2 = Color.green(pixel2)
                val b2 = Color.blue(pixel2)
                
                val difference = sqrt(
                    ((r2 - r1).toDouble().pow(2) +
                     (g2 - g1).toDouble().pow(2) +
                     (b2 - b1).toDouble().pow(2)) / 3.0
                )
                
                totalDifference += difference
                pixelCount++
            }
        }
        
        return totalDifference / pixelCount
    }
}
```

### Stage 3: Person Detection & Tracking using ML Kit

```kotlin
class PersonDetector {
    private val objectDetector: ObjectDetector
    private val poseDetector: PoseDetector
    
    init {
        // Configure ML Kit Object Detection
        val objectDetectorOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        objectDetector = ObjectDetection.getClient(objectDetectorOptions)
        
        // Configure ML Kit Pose Detection
        val poseDetectorOptions = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(poseDetectorOptions)
    }
    
    fun detectPerson(bitmap: Bitmap, callback: (PersonDetectionResult) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        // Detect objects first
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val person = detectedObjects.find { obj ->
                    obj.labels.any { it.text == "Person" && it.confidence > 0.7f }
                }
                
                if (person != null) {
                    // If person detected, get pose information
                    detectPose(inputImage, person, callback)
                } else {
                    callback(PersonDetectionResult(isPersonDetected = false))
                }
            }
            .addOnFailureListener { exception ->
                callback(PersonDetectionResult(isPersonDetected = false, error = exception))
            }
    }
    
    private fun detectPose(
        inputImage: InputImage, 
        person: DetectedObject,
        callback: (PersonDetectionResult) -> Unit
    ) {
        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                val landmarks = extractRelevantLandmarks(pose)
                callback(
                    PersonDetectionResult(
                        isPersonDetected = true,
                        boundingBox = person.boundingBox,
                        pose = landmarks,
                        confidence = pose.allPoseLandmarks.minOfOrNull { it.inFrameLikelihood } ?: 0f
                    )
                )
            }
            .addOnFailureListener {
                callback(
                    PersonDetectionResult(
                        isPersonDetected = true,
                        boundingBox = person.boundingBox,
                        pose = null
                    )
                )
            }
    }
    
    private fun extractRelevantLandmarks(pose: Pose): JumpLandmarks? {
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        
        return if (leftAnkle != null && rightAnkle != null) {
            JumpLandmarks(
                leftAnkle = leftAnkle,
                rightAnkle = rightAnkle,
                leftKnee = leftKnee,
                rightKnee = rightKnee,
                leftHip = leftHip,
                rightHip = rightHip,
                centerOfFeet = PointF(
                    (leftAnkle.position.x + rightAnkle.position.x) / 2,
                    (leftAnkle.position.y + rightAnkle.position.y) / 2
                )
            )
        } else null
    }
}
```

### Stage 4: Jump Event Detection

```kotlin
class JumpEventDetector {
    private val jumpStateManager = JumpStateManager()
    private val heightTracker = HeightTracker()
    
    fun processPersonDetection(
        result: PersonDetectionResult,
        timestamp: Long
    ): JumpEvent? {
        if (!result.isPersonDetected || result.pose == null) {
            jumpStateManager.onPersonLost()
            return null
        }
        
        val footPosition = result.pose.centerOfFeet
        val currentState = jumpStateManager.getCurrentState()
        
        return when (currentState) {
            JumpState.IDLE -> {
                if (detectPrepStance(result.pose)) {
                    jumpStateManager.transitionToPrep()
                    null
                } else null
            }
            
            JumpState.PREP -> {
                when {
                    detectTakeoff(result.pose, footPosition) -> {
                        jumpStateManager.transitionToFlight(timestamp)
                        heightTracker.startTracking(footPosition)
                        JumpEvent.TakeoffDetected(timestamp, footPosition)
                    }
                    detectStanceAbandon(result.pose) -> {
                        jumpStateManager.transitionToIdle()
                        null
                    }
                    else -> null
                }
            }
            
            JumpState.FLIGHT -> {
                heightTracker.updateHeight(footPosition, timestamp)
                
                if (detectLanding(result.pose, footPosition)) {
                    val jumpData = heightTracker.finishTracking()
                    jumpStateManager.transitionToLanded(timestamp)
                    JumpEvent.LandingDetected(timestamp, footPosition, jumpData)
                } else {
                    JumpEvent.FlightProgress(
                        timestamp, 
                        footPosition, 
                        heightTracker.getCurrentHeight()
                    )
                }
            }
            
            JumpState.LANDED -> {
                if (detectStabilization(result.pose)) {
                    jumpStateManager.transitionToIdle()
                    JumpEvent.JumpComplete
                } else null
            }
        }
    }
    
    private fun detectTakeoff(pose: JumpLandmarks, footPosition: PointF): Boolean {
        // Detect takeoff based on:
        // 1. Sudden upward acceleration of center of feet
        // 2. Knee angle extension
        // 3. Hip angle extension
        
        val kneeAngle = calculateKneeAngle(pose)
        val hipAngle = calculateHipAngle(pose)
        val verticalVelocity = heightTracker.getVerticalVelocity()
        
        return verticalVelocity > TAKEOFF_VELOCITY_THRESHOLD && 
               kneeAngle > KNEE_EXTENSION_THRESHOLD &&
               hipAngle > HIP_EXTENSION_THRESHOLD
    }
    
    private fun detectLanding(pose: JumpLandmarks, footPosition: PointF): Boolean {
        // Detect landing based on:
        // 1. Downward movement stopping
        // 2. Foot position stabilizing
        // 3. Knee flexion increase
        
        val verticalVelocity = heightTracker.getVerticalVelocity()
        val kneeAngle = calculateKneeAngle(pose)
        val positionStability = heightTracker.getPositionStability()
        
        return verticalVelocity < LANDING_VELOCITY_THRESHOLD &&
               kneeAngle < KNEE_FLEXION_THRESHOLD &&
               positionStability > STABILITY_THRESHOLD
    }
    
    companion object {
        const val TAKEOFF_VELOCITY_THRESHOLD = 50.0 // pixels per frame
        const val LANDING_VELOCITY_THRESHOLD = -10.0 // negative = downward
        const val KNEE_EXTENSION_THRESHOLD = 160.0 // degrees
        const val HIP_EXTENSION_THRESHOLD = 170.0 // degrees
        const val KNEE_FLEXION_THRESHOLD = 120.0 // degrees
        const val STABILITY_THRESHOLD = 0.9 // stability score 0-1
    }
}
```

### Stage 5: Height Calculation

```kotlin
class JumpHeightCalculator {
    private var calibrationData: CalibrationData? = null
    
    fun calibrate(referenceObjectHeight: Float, referencePixelHeight: Float) {
        calibrationData = CalibrationData(
            pixelsPerCm = referencePixelHeight / referenceObjectHeight,
            baselineY = 0f // Will be set during jump detection
        )
    }
    
    fun calculateJumpHeight(jumpData: JumpTrackingData): JumpMeasurement {
        val calibration = calibrationData ?: throw IllegalStateException("Camera not calibrated")
        
        // Calculate height using multiple methods for accuracy
        val peakMethod = calculateHeightFromPeak(jumpData, calibration)
        val flightTimeMethod = calculateHeightFromFlightTime(jumpData)
        val trajectoryMethod = calculateHeightFromTrajectory(jumpData, calibration)
        
        // Weighted average of methods for final measurement
        val finalHeight = weightedAverage(
            listOf(
                Pair(peakMethod, 0.5), // Peak method gets highest weight
                Pair(flightTimeMethod, 0.3), // Flight time is good backup
                Pair(trajectoryMethod, 0.2)  // Trajectory for verification
            )
        )
        
        val confidence = calculateConfidence(peakMethod, flightTimeMethod, trajectoryMethod)
        
        return JumpMeasurement(
            heightCm = finalHeight,
            confidence = confidence,
            measurementMethods = listOf("peak", "flight_time", "trajectory"),
            rawData = jumpData,
            calibrationUsed = calibration
        )
    }
    
    private fun calculateHeightFromPeak(
        jumpData: JumpTrackingData, 
        calibration: CalibrationData
    ): Double {
        val takeoffY = jumpData.takeoffPosition.y
        val peakY = jumpData.peakPosition.y
        val pixelDifference = takeoffY - peakY // Negative Y is upward
        
        return pixelDifference / calibration.pixelsPerCm
    }
    
    private fun calculateHeightFromFlightTime(jumpData: JumpTrackingData): Double {
        // Using physics: h = (g * t²) / 8
        // Where t is total flight time
        val flightTimeSeconds = jumpData.flightTimeMs / 1000.0
        val gravity = 9.81 // m/s²
        
        val heightMeters = (gravity * flightTimeSeconds * flightTimeSeconds) / 8.0
        return heightMeters * 100.0 // Convert to centimeters
    }
    
    private fun calculateHeightFromTrajectory(
        jumpData: JumpTrackingData, 
        calibration: CalibrationData
    ): Double {
        // Fit parabolic trajectory to position data
        val positions = jumpData.trajectoryPoints
        
        if (positions.size < 5) return 0.0
        
        // Use quadratic regression to fit y = ax² + bx + c
        val (a, b, c) = fitQuadraticRegression(positions)
        
        // Find vertex of parabola (peak)
        val peakX = -b / (2 * a)
        val peakY = a * peakX * peakX + b * peakX + c
        
        // Calculate height difference
        val takeoffY = positions.first().y
        val pixelHeight = takeoffY - peakY
        
        return pixelHeight / calibration.pixelsPerCm
    }
    
    private fun calculateConfidence(vararg measurements: Double): Double {
        if (measurements.isEmpty()) return 0.0
        
        val mean = measurements.average()
        val variance = measurements.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Lower standard deviation = higher confidence
        val coefficientOfVariation = standardDeviation / mean
        
        return maxOf(0.0, 1.0 - coefficientOfVariation).coerceAtMost(1.0)
    }
}
```

## Calibration System

### Automatic Calibration
```kotlin
class AutoCalibrationManager {
    fun performAutoCalibration(bitmap: Bitmap): CalibrationResult {
        // Detect common objects for scale reference
        val referenceObjects = detectReferenceObjects(bitmap)
        
        val bestReference = referenceObjects
            .filter { it.confidence > 0.8 }
            .maxByOrNull { it.confidence }
        
        return if (bestReference != null) {
            val pixelsPerCm = bestReference.pixelHeight / bestReference.realWorldHeight
            CalibrationResult.Success(pixelsPerCm, bestReference.name)
        } else {
            CalibrationResult.RequiresManualCalibration
        }
    }
    
    private fun detectReferenceObjects(bitmap: Bitmap): List<ReferenceObject> {
        // Common objects with known dimensions
        val knownObjects = mapOf(
            "Person" to 170.0, // Average height in cm
            "Door" to 200.0,   // Standard door height
            "Chair" to 80.0,   // Chair back height
            "Table" to 75.0    // Standard table height
        )
        
        // Use ML Kit to detect these objects and estimate their size
        return detectObjectsWithDimensions(bitmap, knownObjects)
    }
}
```

### Manual Calibration UI
```kotlin
class ManualCalibrationView {
    fun showCalibrationOverlay(bitmap: Bitmap): CalibrationOverlay {
        return CalibrationOverlay(
            instructions = "Position the red line at the bottom of a known object",
            referenceLineY = bitmap.height * 0.8f,
            inputField = "Enter object height in cm",
            onCalibrationComplete = { height ->
                performManualCalibration(height, referenceLineY)
            }
        )
    }
}
```

## Performance Optimization

### Frame Processing Optimization
```kotlin
class OptimizedFrameProcessor {
    private val processingExecutor = Executors.newFixedThreadPool(2)
    private val frameSkipCount = 2 // Process every 3rd frame
    private var frameCounter = 0
    
    fun processFrame(image: Image) {
        frameCounter++
        
        // Skip frames for performance
        if (frameCounter % (frameSkipCount + 1) != 0) {
            image.close()
            return
        }
        
        // Process on background thread
        processingExecutor.execute {
            try {
                val bitmap = preprocessFrame(image)
                val motionResult = detectMotion(bitmap)
                
                if (motionResult.hasMotion) {
                    personDetector.detectPerson(bitmap) { result ->
                        jumpEventDetector.processPersonDetection(result, System.currentTimeMillis())
                    }
                }
            } finally {
                image.close()
            }
        }
    }
}
```

### Memory Management
```kotlin
class MemoryManager {
    private val bitmapPool = BitmapPool(maxSize = 10)
    
    fun recycleBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        bitmapPool.put(bitmap)
    }
    
    fun getBitmap(width: Int, height: Int): Bitmap {
        return bitmapPool.get(width, height) ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}
```

## Error Handling & Edge Cases

### Common Issues and Solutions

1. **Poor Lighting Conditions**
   - Increase exposure compensation
   - Enable flash when needed
   - Adjust detection thresholds dynamically

2. **Camera Shake/Movement**
   - Implement stabilization algorithms
   - Warn user about camera stability
   - Use gyroscope data for correction

3. **Multiple People in Frame**
   - Track largest detected person
   - Warn user to clear the jump area
   - Use person ID tracking across frames

4. **Partial Person Detection**
   - Require minimum pose landmarks
   - Fallback to bounding box tracking
   - Reduce confidence in measurements

```kotlin
class ErrorHandler {
    fun handleDetectionError(error: DetectionError): ErrorResponse {
        return when (error) {
            is DetectionError.LowLight -> ErrorResponse.SuggestBetterLighting
            is DetectionError.CameraShake -> ErrorResponse.SuggestTripod
            is DetectionError.MultiplePeople -> ErrorResponse.ClearJumpArea
            is DetectionError.PartialDetection -> ErrorResponse.ImproveFraming
            is DetectionError.CalibrationFailed -> ErrorResponse.RequireManualCalibration
        }
    }
}
```

## Testing Strategy

### Unit Tests for Algorithms
```kotlin
class JumpDetectionTest {
    @Test
    fun `calculateJumpHeight_withValidData_returnsCorrectHeight`() {
        val jumpData = createTestJumpData(
            takeoffY = 800f,
            peakY = 600f,
            flightTime = 800L
        )
        val calibration = CalibrationData(pixelsPerCm = 10f, baselineY = 800f)
        
        val calculator = JumpHeightCalculator()
        calculator.calibrate(50f, 500f) // 50cm reference, 500 pixels
        
        val result = calculator.calculateJumpHeight(jumpData)
        
        assertEquals(20.0, result.heightCm, 0.5) // 200 pixels / 10 px/cm = 20cm
        assertTrue(result.confidence > 0.7)
    }
}
```

### Integration Tests with Mock Camera
```kotlin
class CameraIntegrationTest {
    @Test
    fun `camera_processingPipeline_detectsJumpCorrectly`() {
        val mockCameraFrames = loadTestFrameSequence("jump_sequence.mp4")
        val jumpDetector = JumpEventDetector()
        
        val detectedEvents = mutableListOf<JumpEvent>()
        
        mockCameraFrames.forEach { frame ->
            jumpDetector.processFrame(frame) { event ->
                detectedEvents.add(event)
            }
        }
        
        assertTrue(detectedEvents.any { it is JumpEvent.TakeoffDetected })
        assertTrue(detectedEvents.any { it is JumpEvent.LandingDetected })
    }
}
```

This comprehensive camera integration and jump detection system provides accurate, real-time vertical jump measurement using computer vision and machine learning techniques optimized for mobile devices.