package com.watxaut.myjumpapp.presentation.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPoseDetected: (androidx.camera.core.ImageProxy, com.google.mlkit.vision.pose.Pose) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val poseDetector = remember { createPoseDetector() }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    ) { view ->
        try {
            val cameraProvider = cameraProviderFuture.get()
            Log.d("CameraPreview", "Camera provider obtained successfully")
        
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
        
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    PoseAnalyzer(poseDetector, onPoseDetected)
                )
            }
        
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d("CameraPreview", "Camera bound successfully: ${camera.cameraInfo}")
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Camera binding failed", exc)
            }
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Failed to get camera provider", exc)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            poseDetector.close()
        }
    }
}

private fun createPoseDetector(): PoseDetector {
    val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
        .build()
    Log.d("CameraPreview", "Pose detector created with options: STREAM_MODE, CPU_GPU")
    return PoseDetection.getClient(options)
}

private class PoseAnalyzer(
    private val poseDetector: PoseDetector,
    private val onPoseDetected: (ImageProxy, com.google.mlkit.vision.pose.Pose) -> Unit
) : ImageAnalysis.Analyzer {
    
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                Log.d("PoseAnalyzer", "Processing image: ${mediaImage.width}x${mediaImage.height}, rotation: ${imageProxy.imageInfo.rotationDegrees}")
                
                poseDetector.process(image)
                    .addOnSuccessListener { pose ->
                        // Always call onPoseDetected to update debug info, even if no pose found
                        onPoseDetected(imageProxy, pose)
                        Log.d("PoseAnalyzer", "Pose detection success: ${pose.allPoseLandmarks.size} landmarks")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("PoseAnalyzer", "Pose detection failed", exception)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (exc: Exception) {
                Log.e("PoseAnalyzer", "Error creating InputImage", exc)
                imageProxy.close()
            }
        } else {
            Log.w("PoseAnalyzer", "MediaImage is null")
            imageProxy.close()
        }
    }
}