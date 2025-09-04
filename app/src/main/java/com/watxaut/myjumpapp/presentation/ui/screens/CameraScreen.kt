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
            // Top Status Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (uiState.isCalibrating) {
                        CalibrationStatus(uiState.debugInfo)
                    } else {
                        MaxHeightDisplay(uiState.maxHeight)
                    }
                    
                    // Position warning
                    uiState.debugInfo.positionWarning?.let { warning ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PositionWarning(warning)
                    }
                    
                    if (uiState.isSessionActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SessionStats(uiState)
                    }
                }
            }
            
            // Bottom Controls - Only show stop button when session is active
            if (uiState.isSessionActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onStopSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stop Session",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            } else if (uiState.isCalibrating) {
                // Show calibration status when not active and still calibrating
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.debugInfo.poseDetected) 
                                "Stay still while calibrating..." 
                            else 
                                "Position yourself in camera view",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Session will start automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Max Height Display (center overlay) - Always show when not calibrating
        if (!uiState.isCalibrating && uiState.maxHeight > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${String.format("%.1f", uiState.maxHeight)} cm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Max Height (${surfaceType.displayName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MaxHeightDisplay(maxHeight: Double) {
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
            text = "Max Height: ${String.format("%.1f", maxHeight)} cm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
            value = "${String.format("%.1f", uiState.maxHeight)} cm"
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