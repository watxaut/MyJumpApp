package com.watxaut.myjumpapp.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    pose: Pose?,
    previewWidth: Float = 864f,  // Default camera preview width
    previewHeight: Float = 480f, // Default camera preview height
    modifier: Modifier = Modifier
) {
    val headColor = MaterialTheme.colorScheme.primary
    val hipColor = MaterialTheme.colorScheme.secondary
    val footColor = MaterialTheme.colorScheme.tertiary
    
    Canvas(modifier = modifier.fillMaxSize()) {
        pose?.let { p ->
            // Camera is rotated 270 degrees, so we need to transform coordinates
            // The camera preview is 864x480 but rotated, so effective dimensions are swapped
            val effectivePreviewWidth = previewHeight // 480
            val effectivePreviewHeight = previewWidth // 864
            
            val scaleX = size.width / effectivePreviewWidth
            val scaleY = size.height / effectivePreviewHeight
            
            // Draw head landmarks
            drawHeadLandmarks(p, headColor, scaleX, scaleY, effectivePreviewWidth, effectivePreviewHeight)
            // Draw hip landmarks
            drawHipLandmarks(p, hipColor, scaleX, scaleY, effectivePreviewWidth, effectivePreviewHeight)
            // Draw foot landmarks
            drawFootLandmarks(p, footColor, scaleX, scaleY, effectivePreviewWidth, effectivePreviewHeight)
        }
    }
}

private fun DrawScope.drawHeadLandmarks(pose: Pose, color: Color, scaleX: Float, scaleY: Float, previewWidth: Float, previewHeight: Float) {
    val headLandmarks = listOf(
        PoseLandmark.NOSE,
        PoseLandmark.LEFT_EAR,
        PoseLandmark.RIGHT_EAR
    )
    
    headLandmarks.forEach { landmarkType ->
        pose.getPoseLandmark(landmarkType)?.let { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                // Transform coordinates and mirror for front-facing camera
                val transformedX = (size.width - landmark.position.x * scaleX)
                val transformedY = landmark.position.y * scaleY
                
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = Offset(transformedX, transformedY)
                )
            }
        }
    }
}

private fun DrawScope.drawHipLandmarks(pose: Pose, color: Color, scaleX: Float, scaleY: Float, previewWidth: Float, previewHeight: Float) {
    val hipLandmarks = listOf(
        PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_HIP
    )
    
    hipLandmarks.forEach { landmarkType ->
        pose.getPoseLandmark(landmarkType)?.let { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                // Transform coordinates and mirror for front-facing camera
                val transformedX = (size.width - landmark.position.x * scaleX)
                val transformedY = landmark.position.y * scaleY
                
                drawCircle(
                    color = color,
                    radius = 8.dp.toPx(),
                    center = Offset(transformedX, transformedY)
                )
            }
        }
    }
}

private fun DrawScope.drawFootLandmarks(pose: Pose, color: Color, scaleX: Float, scaleY: Float, previewWidth: Float, previewHeight: Float) {
    val footLandmarks = listOf(
        PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.LEFT_HEEL,
        PoseLandmark.RIGHT_HEEL,
        PoseLandmark.LEFT_FOOT_INDEX,
        PoseLandmark.RIGHT_FOOT_INDEX
    )
    
    footLandmarks.forEach { landmarkType ->
        pose.getPoseLandmark(landmarkType)?.let { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                // Transform coordinates and mirror for front-facing camera
                val transformedX = (size.width - landmark.position.x * scaleX)
                val transformedY = landmark.position.y * scaleY
                
                drawCircle(
                    color = color,
                    radius = 5.dp.toPx(),
                    center = Offset(transformedX, transformedY)
                )
            }
        }
    }
}