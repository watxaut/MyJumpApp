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
import com.watxaut.myjumpapp.domain.jump.JumpPhase
import com.watxaut.myjumpapp.domain.jump.DebugInfo
import com.watxaut.myjumpapp.presentation.ui.components.CameraPermissionHandler
import com.watxaut.myjumpapp.presentation.ui.components.CameraPreview
import com.watxaut.myjumpapp.presentation.ui.components.CameraSetupGuideDialog
import com.watxaut.myjumpapp.presentation.viewmodels.JumpSessionViewModel
import com.watxaut.myjumpapp.presentation.viewmodels.JumpSessionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    userId: String?,
    onNavigateBack: () -> Unit,
    viewModel: JumpSessionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSetupGuide by remember { mutableStateOf(false) }
    var showInitialTip by remember { mutableStateOf(true) }
    
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.setUserId(userId)
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
                        Text("Jump Detection")
                        if (uiState.userName.isNotEmpty()) {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    showInitialTip = showInitialTip,
                    onDismissTip = { showInitialTip = false },
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
    showInitialTip: Boolean,
    onDismissTip: () -> Unit,
    onShowGuide: () -> Unit,
    onStopSession: () -> Unit,
    onPoseDetected: (androidx.camera.core.ImageProxy, com.google.mlkit.vision.pose.Pose) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            onPoseDetected = onPoseDetected
        )
        
        // Initial Setup Tip
        if (showInitialTip && !uiState.isSessionActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Setup Tip",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Position phone 120-180 cm away at waist height. Tap ? for full guide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Row {
                        TextButton(onClick = onShowGuide) {
                            Text("Guide", color = MaterialTheme.colorScheme.primary)
                        }
                        
                        TextButton(onClick = onDismissTip) {
                            Text("Got it", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        
        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar (with space for tip)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = if (showInitialTip && !uiState.isSessionActive) 80.dp else 0.dp),
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
                        JumpPhaseIndicator(uiState.jumpPhase)
                    }
                    
                    // Debug info display
                    if (!uiState.isCalibrating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DebugInfoDisplay(uiState.debugInfo)
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
        
        // Jump Height Display (center overlay)
        if (uiState.isJumping && uiState.currentJumpHeight > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "${String.format("%.1f", uiState.currentJumpHeight)} cm",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun JumpPhaseIndicator(phase: JumpPhase) {
    val (text, color) = when (phase) {
        JumpPhase.STANDING -> "Ready" to MaterialTheme.colorScheme.primary
        JumpPhase.PREPARING -> "Preparing..." to MaterialTheme.colorScheme.tertiary
        JumpPhase.AIRBORNE -> "JUMPING!" to MaterialTheme.colorScheme.secondary
        JumpPhase.LANDING -> "Landing" to MaterialTheme.colorScheme.primary
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
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
            label = "Jumps",
            value = "${uiState.jumpCount}"
        )
        
        StatItem(
            label = "Best",
            value = "${String.format("%.1f", uiState.bestJumpHeight)} cm"
        )
        
        if (uiState.jumpCount > 0) {
            StatItem(
                label = "Avg",
                value = "${String.format("%.1f", uiState.averageJumpHeight)} cm"
            )
        }
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
                progress = debugInfo.calibrationProgress.toFloat() / debugInfo.calibrationFramesNeeded.toFloat(),
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (debugInfo.poseDetected) "Calibrating... Stand still" else "Searching for person...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${debugInfo.calibrationProgress}/${debugInfo.calibrationFramesNeeded} frames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Live debug info during calibration
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DebugItem(
                label = "Body",
                value = if (debugInfo.poseDetected) "✓ Detected" else "✗ Not found",
                color = if (debugInfo.poseDetected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            DebugItem(
                label = "Landmarks",
                value = "${debugInfo.landmarksDetected}",
                color = if (debugInfo.landmarksDetected >= 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
            
            DebugItem(
                label = "Confidence",
                value = "${String.format("%.0f", debugInfo.confidenceScore * 100)}%",
                color = if (debugInfo.confidenceScore > 0.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun DebugInfoDisplay(debugInfo: DebugInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Detection Status",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DebugItem(
                    label = "Height",
                    value = "${String.format("%.1f", debugInfo.currentHeight)}px",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                DebugItem(
                    label = "Baseline",
                    value = "${String.format("%.1f", debugInfo.baselineHeight)}px",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                DebugItem(
                    label = "Diff",
                    value = "${String.format("%+.2f", debugInfo.heightDifference)}px",
                    color = if (Math.abs(debugInfo.heightDifference) > 0.02) 
                        MaterialTheme.colorScheme.secondary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DebugItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}