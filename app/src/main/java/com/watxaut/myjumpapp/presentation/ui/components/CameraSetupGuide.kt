package com.watxaut.myjumpapp.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CameraSetupGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera Setup Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, false)
                ) {
                    // Phone Placement Section
                    SetupSection(
                        title = "📱 Phone Placement",
                        icon = Icons.Default.Phone,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        SetupItem("Distance: 120-180 cm away from jumping area")
                        SetupItem("Height: 90-120 cm off the ground (waist to chest level)")
                        SetupItem("Orientation: Hold phone vertically (portrait mode)")
                        SetupItem("Position: Use front camera facing toward you")
                        SetupItem("Stability: Place on tripod, shelf, or stable surface")
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Body Positioning Section
                    SetupSection(
                        title = "🎯 Body Positioning", 
                        icon = Icons.Default.Person,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        SetupItem("Frame: Your entire body from head to feet must be visible")
                        SetupItem("Center: Stand in the center of the camera view")
                        SetupItem("Space: Leave margin around your body in the frame")
                        SetupItem("Face: Look toward the camera for best detection")
                        SetupItem("Lighting: Ensure good lighting on your body")
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Calibration Tips Section
                    SetupSection(
                        title = "⚡ Calibration Tips",
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        SetupItem("Stand completely still for 2-3 seconds")
                        SetupItem("Keep arms naturally at your sides")
                        SetupItem("Don't move until you see \"Ready\" status")
                        SetupItem("Wear fitted clothing for better pose detection")
                        SetupItem("Use plain background if possible")
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Jumping Tips Section
                    SetupSection(
                        title = "🏃‍♂️ Jumping Tips",
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        SetupItem("Jump straight up and down (not forward/backward)")
                        SetupItem("Stay within the camera frame")
                        SetupItem("Make clear crouch → jump → land movements")
                        SetupItem("Try to land in the same spot")
                        SetupItem("Face the camera throughout the jump")
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Visual Guide
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Optimal Setup:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "📱 Phone\n(120-180 cm away)\n↓\n🧍 You\n(centered in frame)\n🏠 Floor",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
private fun SetupSection(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
private fun SetupItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                .padding(top = 6.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}