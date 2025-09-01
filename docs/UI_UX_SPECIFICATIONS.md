# MyJumpApp - UI/UX Specifications

## Design Philosophy

MyJumpApp follows Material Design 3 principles with a focus on:
- **Simplicity**: Clear, intuitive interface for users of all technical levels
- **Performance Focus**: Real-time feedback and smooth animations
- **Accessibility**: Support for users with different abilities
- **Visual Clarity**: High contrast for outdoor use scenarios

## Color Scheme & Branding

### Primary Colors
```kotlin
// Theme colors based on Material Design 3
val md_theme_light_primary = Color(0xFF006C4C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF89F8C7)
val md_theme_light_onPrimaryContainer = Color(0xFF002114)

val md_theme_dark_primary = Color(0xFF6CDB9C)
val md_theme_dark_onPrimary = Color(0xFF003826)
val md_theme_dark_primaryContainer = Color(0xFF005138)
val md_theme_dark_onPrimaryContainer = Color(0xFF89F8C7)
```

### Semantic Colors
- **Success**: Green tones for successful jumps and achievements
- **Warning**: Amber for calibration warnings
- **Error**: Red for measurement errors or camera issues
- **Info**: Blue for informational messages and tips

## Screen Structure & Navigation

### Navigation Architecture
```
MainScreen (Bottom Navigation)
├── HomeScreen (Jump & Quick Start)
├── UsersScreen (User Management)
├── StatsScreen (Analytics & Graphs)
└── SettingsScreen (App Configuration)
```

### Screen Flow Diagram
```
Splash → Home → Camera → Results → Stats
    ↓      ↓       ↑        ↓       ↑
Settings   Users ──┴────────┴───────┘
```

## Screen Specifications

### 1. Splash Screen
**Purpose**: App branding and initialization
**Duration**: 2-3 seconds maximum

```kotlin
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_jump_logo),
                contentDescription = "MyJumpApp Logo",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MyJumpApp",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

### 2. Home Screen
**Purpose**: Main dashboard with quick jump access and recent activity

#### Layout Components:
- **Header**: Welcome message with selected user
- **Quick Jump Button**: Large, prominent CTA
- **Recent Jumps**: Horizontal scroll of last 5 jumps
- **Today's Stats**: Quick overview of today's activity

```kotlin
@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { UserSelectionCard() }
        item { QuickJumpButton() }
        item { TodayStatsCard() }
        item { RecentJumpsSection() }
    }
}
```

#### User Selection Card
- Dropdown/selector for active user
- Add new user button
- User avatar and basic info

#### Quick Jump Button
- Large, centered button (minimum 56dp height)
- Animation on press
- Disabled state if no camera permission

### 3. Camera Screen
**Purpose**: Real-time jump detection and measurement

#### Layout Components:
- **Camera Preview**: Full-screen camera view
- **Overlay UI**: Minimal interference with camera view
- **Calibration Guide**: Visual indicators for setup
- **Control Panel**: Start/stop, settings access

```kotlin
@Composable
fun CameraScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )
        
        CameraOverlay(
            modifier = Modifier.fillMaxSize()
        )
        
        ControlPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
```

#### Camera Overlay Elements:
- **Grid Lines**: Optional 3x3 grid for composition
- **Jump Detection Zone**: Visual boundary for jump area
- **Calibration Markers**: Reference points for measurement
- **Real-time Feedback**: Jump height display during detection

#### Control Panel:
- Record button (large, red when recording)
- Switch camera button
- Flash toggle
- Settings gear icon

### 4. User Management Screen
**Purpose**: Create, edit, and manage user profiles

#### Layout Components:
- **User List**: Scrollable list of all users
- **Add User FAB**: Floating action button
- **User Cards**: Individual user information

```kotlin
@Composable
fun UsersScreen() {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add new user */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(user = user)
            }
        }
    }
}
```

#### User Card Components:
- Profile image (avatar or placeholder)
- Name and basic stats
- Last jump date
- Edit/delete actions
- Jump to user stats button

### 5. Statistics Screen
**Purpose**: Data visualization and performance analytics

#### Layout Components:
- **Time Period Selector**: Tabs for Week/Month/Year/All Time
- **Progress Chart**: Line graph showing improvement over time
- **Statistics Cards**: Key metrics in card format
- **Jump History Table**: Detailed jump records

```kotlin
@Composable
fun StatsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TimePeriodSelector()
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProgressChart() }
            item { StatsCardsRow() }
            item { JumpHistoryTable() }
        }
    }
}
```

#### Progress Chart:
- Line chart using MPAndroidChart
- X-axis: Time (days/weeks/months)
- Y-axis: Jump height (cm)
- Interactive zoom and pan
- Data points with touch feedback

#### Statistics Cards:
- **Best Jump**: Highest recorded jump
- **Average Jump**: Mean performance
- **Total Jumps**: Count of all jumps
- **Improvement**: Percentage change

### 6. Settings Screen
**Purpose**: App configuration and preferences

#### Settings Categories:
```kotlin
// Settings structure
data class SettingsSection(
    val title: String,
    val items: List<SettingItem>
)

val settingsSections = listOf(
    SettingsSection("Camera", listOf(
        SettingItem.Switch("Auto-calibration", true),
        SettingItem.Slider("Detection sensitivity", 0.8f),
        SettingItem.Switch("Save jump videos", false)
    )),
    SettingsSection("Measurements", listOf(
        SettingItem.Dropdown("Units", "Centimeters", listOf("Centimeters", "Inches")),
        SettingItem.Switch("Show confidence score", true)
    )),
    SettingsSection("Privacy", listOf(
        SettingItem.Button("Export data"),
        SettingItem.Button("Delete all data"),
        SettingItem.Toggle("Analytics", false)
    ))
)
```

## Component Library

### Custom Components

#### 1. JumpCard
```kotlin
@Composable
fun JumpCard(
    jump: Jump,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${jump.heightCm} cm",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = formatTimestamp(jump.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (jump.confidenceScore < 0.8) {
                Text(
                    text = "Low confidence measurement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

#### 2. StatCard
```kotlin
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

#### 3. CameraPermissionDialog
```kotlin
@Composable
fun CameraPermissionDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Required") },
        text = { 
            Text("MyJumpApp needs camera access to measure your vertical jumps accurately.")
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

## Responsive Design

### Screen Size Adaptations

#### Phone Layout (< 600dp width)
- Single column layout
- Bottom navigation
- Full-screen camera view

#### Tablet Layout (≥ 600dp width)
- Two-column layout where appropriate
- Side navigation rail
- Picture-in-picture camera view option

#### Landscape Considerations
- Horizontal layout for statistics
- Optimized camera controls placement
- Landscape-specific chart layouts

## Accessibility Features

### Implementation Requirements
```kotlin
// Semantic descriptions for screen readers
modifier = Modifier.semantics {
    contentDescription = "Jump height: 45 centimeters"
    role = Role.Button
}

// High contrast support
colors = if (isSystemInDarkTheme()) DarkColors else LightColors

// Large text support
fontSize = if (isLargeTextEnabled) 
    MaterialTheme.typography.headlineLarge.fontSize
else 
    MaterialTheme.typography.headlineMedium.fontSize
```

### Accessibility Checklist
- [ ] Minimum touch target size: 48dp
- [ ] Color contrast ratio: 4.5:1 (normal text), 3:1 (large text)
- [ ] Content descriptions for all interactive elements
- [ ] Focus management for screen readers
- [ ] Support for TalkBack navigation
- [ ] Keyboard navigation support
- [ ] Scale factor support for text sizes

## Animation Specifications

### Transition Animations
```kotlin
// Screen transitions
val screenTransition = slideInHorizontally() + fadeIn() with
                      slideOutHorizontally() + fadeOut()

// Jump detection animation
val jumpPulse = infiniteRepeatable(
    animation = tween(1000),
    repeatMode = RepeatMode.Reverse
)

// Chart data loading
val chartAnimation = tween<Float>(
    durationMillis = 1500,
    easing = EaseInOutCubic
)
```

### Feedback Animations
- Button press: Scale down to 0.95x for 100ms
- Jump detection: Pulse animation on detection zone
- Data loading: Progress indicators and skeleton screens
- Success feedback: Checkmark animation with bounce

## Performance Considerations

### UI Performance
- Use `LazyColumn` for large lists
- Implement proper `key` values for list items
- Cache expensive computations with `remember`
- Use `derivedStateOf` for derived values

### Memory Management
- Dispose of camera resources properly
- Clean up chart data when not needed
- Use appropriate image loading libraries (Coil)
- Implement proper lifecycle awareness

## Testing Strategy

### UI Testing
```kotlin
@Test
fun jumpCard_displaysCorrectHeight() {
    composeTestRule.setContent {
        JumpCard(
            jump = testJump.copy(heightCm = 45.5)
        )
    }
    
    composeTestRule
        .onNodeWithText("45.5 cm")
        .assertIsDisplayed()
}
```

### Accessibility Testing
- Test with TalkBack enabled
- Verify touch target sizes
- Check color contrast ratios
- Test with various text sizes

This UI/UX specification provides a comprehensive foundation for developing an intuitive, accessible, and performant vertical jump tracking application.