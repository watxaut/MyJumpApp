package com.watxaut.myjumpapp.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.domain.jump.SurfaceType
import com.watxaut.myjumpapp.presentation.ui.components.CameraPermissionHandler
import com.watxaut.myjumpapp.presentation.ui.components.CameraPreview
import com.watxaut.myjumpapp.presentation.ui.components.CameraSetupGuideDialog
import com.watxaut.myjumpapp.presentation.ui.components.PoseOverlay
import com.watxaut.myjumpapp.presentation.viewmodels.JumpSessionViewModel
import com.watxaut.myjumpapp.presentation.viewmodels.JumpSessionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    userId: String?,
    surfaceType: SurfaceType,
    onNavigateBack: () -> Unit,
    viewModel: JumpSessionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSetupGuide by remember { mutableStateOf(false) }
    
    LaunchedEffect(userId, surfaceType) {
        if (userId != null) {
            viewModel.setUserId(userId)
            viewModel.setSurfaceType(surfaceType)
        }
    }
    
    // Show error as snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Handle error display
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jump Detection")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = surfaceType.icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (uiState.userName.isNotEmpty()) {
                            Text(
                                text = "${uiState.userName} • ${surfaceType.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.resetToInitialState()
                        onNavigateBack() 
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSetupGuide = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Setup Guide"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (userId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please select a user first",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            CameraPermissionHandler {
                JumpDetectionContent(
                    uiState = uiState,
                    surfaceType = surfaceType,
                    onShowGuide = { showSetupGuide = true },
                    onStopSession = { viewModel.stopSession() },
                    onPoseDetected = { imageProxy, pose ->
                        viewModel.processPoseDetection(imageProxy, pose)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
    
    // Show setup guide dialog
    if (showSetupGuide) {
        CameraSetupGuideDialog(
            onDismiss = { showSetupGuide = false }
        )
    }
}

@Composable
private fun JumpDetectionContent(
    uiState: JumpSessionUiState,
    surfaceType: SurfaceType,
    onShowGuide: () -> Unit,
    onStopSession: () -> Unit,
    onPoseDetected: (androidx.camera.core.ImageProxy, com.google.mlkit.vision.pose.Pose) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPose by remember { mutableStateOf<com.google.mlkit.vision.pose.Pose?>(null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            onPoseDetected = { imageProxy, pose ->
                currentPose = pose
                onPoseDetected(imageProxy, pose)
            }
        )
        
        // Pose Overlay - Draw head, hips, and toes
        PoseOverlay(
            pose = currentPose
        )
        
        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top spacer to push content to bottom
            Spacer(modifier = Modifier.height(1.dp))
            
            // Bottom Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isSessionActive) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    else 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isSessionActive) {
                        // Stop session button (reduced height)
                        Button(
                            onClick = onStopSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stop Session",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    } else {
                        // Positioning instructions
                        Text(
                            text = "Place yourself in position",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.isCalibrating) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stand 2-3 meters away from the camera",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isCalibrating) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Make sure your entire body is visible",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isCalibrating) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Center Screen Messages
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            when {
                // Show calibration messages when all landmarks are detected and calibrating
                uiState.isCalibrating && uiState.debugInfo.poseDetected -> {
                    Card(
                        modifier = Modifier
                            .padding(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = if (uiState.debugInfo.isStable) {
                                    uiState.debugInfo.calibrationProgress.toFloat() / uiState.debugInfo.calibrationFramesNeeded.toFloat()
                                } else {
                                    uiState.debugInfo.stabilityProgress
                                },
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val statusText = when {
                                !uiState.debugInfo.isStable -> "Stay Still"
                                uiState.debugInfo.calibrationProgress > 0 -> "Calibrating..."
                                else -> "Ready to Calibrate"
                            }
                            
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val subtitleText = if (uiState.debugInfo.isStable) {
                                "${uiState.debugInfo.calibrationProgress}/${uiState.debugInfo.calibrationFramesNeeded} frames"
                            } else {
                                "Minimize movement for accurate calibration"
                            }
                            
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Show max height when session is active and not calibrating
                !uiState.isCalibrating && uiState.maxHeight > 0 -> {
                    Card(
                        modifier = Modifier
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${String.format("%.1f", uiState.maxHeight)} cm",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!uiState.hasEyeToHeadMeasurement && uiState.maxHeight > 0.0) {
                                Text(
                                    text = "(${String.format("%.1f", uiState.maxHeightLowerBound)} - ${String.format("%.1f", uiState.maxHeightUpperBound)} cm)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = "Max Height (${surfaceType.displayName})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        // Position warning overlay (top of screen)
        uiState.debugInfo.positionWarning?.let { warning ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Position Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MaxHeightDisplay(uiState: JumpSessionUiState) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Max Height: ${String.format("%.1f", uiState.maxHeight)} cm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (!uiState.hasEyeToHeadMeasurement && uiState.maxHeight > 0.0) {
            Text(
                text = "Confidence interval: ${String.format("%.1f", uiState.maxHeightLowerBound)} - ${String.format("%.1f", uiState.maxHeightUpperBound)} cm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun SessionStats(uiState: JumpSessionUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(
            label = "Max Height",
            value = if (uiState.hasEyeToHeadMeasurement || uiState.maxHeight == 0.0) {
                "${String.format("%.1f", uiState.maxHeight)} cm"
            } else {
                "${String.format("%.1f", uiState.maxHeight)} cm (${String.format("%.1f", uiState.maxHeightLowerBound)}-${String.format("%.1f", uiState.maxHeightUpperBound)})"
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalibrationStatus(debugInfo: DebugInfo) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = if (debugInfo.isStable) {
                    debugInfo.calibrationProgress.toFloat() / debugInfo.calibrationFramesNeeded.toFloat()
                } else {
                    debugInfo.stabilityProgress
                },
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = if (debugInfo.isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                val statusText = when {
                    !debugInfo.poseDetected -> "Searching for person..."
                    !debugInfo.isStable -> "Stay steady... ${String.format("%.0f", debugInfo.stabilityProgress * 100)}%"
                    debugInfo.calibrationProgress > 0 -> "Calibrating... Stand still"
                    else -> "Ready to calibrate"
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !debugInfo.poseDetected -> MaterialTheme.colorScheme.error
                        !debugInfo.isStable -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                val subtitleText = if (debugInfo.isStable) {
                    "${debugInfo.calibrationProgress}/${debugInfo.calibrationFramesNeeded} frames"
                } else {
                    "Minimize movement for 2 seconds"
                }
                
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
    }
}



@Composable
private fun PositionWarning(warning: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.error
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Position Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}