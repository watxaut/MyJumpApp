# MyJumpApp 🏀

An Android application for measuring and tracking vertical jump height using computer vision and pose detection technology.

## Features

### 🎯 **Accurate Jump Measurement**
- **Real-time pose detection** using Google ML Kit Vision
- **Automatic jump detection** with hip position tracking
- **Precise height calculation** using pixel-to-cm conversion based on user body measurements
- **Movement stability detection** requiring 2 seconds of stillness before calibration
- **Advanced false-positive filtering** to prevent camera approach from registering as jumps

### 🏃‍♂️ **Multi-User Support**
- **Create and manage multiple user profiles**
- **Individual statistics tracking** for each user
- **User height configuration** for accurate measurements (eye-to-ankle calibration)
- **User-specific personal records and achievements**

### 🏐 **Surface Type Tracking**
- **Hard Floor** tracking (🏀) - for indoor courts, gymnasium floors, concrete
- **Sand** tracking (🏖️) - for beach volleyball courts and sand surfaces
- **Separate statistics** for each surface type (jumps are typically lower on sand)
- **Surface comparison analytics** showing performance differences

### 📊 **Comprehensive Statistics**
- **Personal bests** and average heights per surface
- **Recent performance** (last 7 days, last 30 days)
- **Progress tracking** with trend analysis
- **Achievement system** with milestone tracking
- **Session history** with detailed analytics
- **Streak tracking** (daily and weekly)

### 🎚️ **Advanced Calibration System**
- **Automatic person detection** with full body visibility requirements
- **Movement stability detection** - user must remain still for 2 seconds
- **Real-time calibration progress** with visual feedback
- **Pixel-to-cm ratio calculation** based on user's actual height
- **High-confidence pose landmark detection** (>95% confidence threshold)

### 🛡️ **Anti-False-Positive Technology**
Three-layer filtering system to ensure accurate measurements:
1. **Depth filtering** - maintains consistent distance from camera
2. **Baseline depth comparison** - validates user stays at calibration depth
3. **Average Z-position filtering** - uses running averages to detect approach movements

### 📱 **Modern UI/UX**
- **Material Design 3** with dynamic color theming
- **Real-time debug information** for troubleshooting measurements
- **Comprehensive statistics dashboard** with multiple views
- **Surface-specific performance comparison**
- **Visual progress indicators** and achievement tracking

## How It Works

### 1. **User Setup**
- Create a user profile with name and accurate height measurement
- Select surface type (Hard Floor or Sand) for your jumping session
- The app uses your height for precise pixel-to-cm calibration

### 2. **Camera Calibration**
- Position yourself in camera view with full body visible
- The app detects your pose using ML Kit Vision API
- **Stay steady** for 2 seconds while the app calibrates baseline positions
- Automatic calibration completes when sufficient data is collected

### 3. **Jump Detection**
- Each session represents one jump attempt
- The app tracks your hip center position in real-time
- Maximum height reached during the session is recorded
- Advanced filtering prevents false readings from camera movement

### 4. **Statistics Tracking**
- Performance data is automatically saved per surface type
- View progress over time with detailed analytics
- Compare performance between different surfaces
- Track personal records and achievements

## Technical Architecture

### 🏗️ **Built With**
- **Kotlin** - Primary development language
- **Jetpack Compose** - Modern Android UI toolkit
- **ML Kit Vision** - Google's pose detection API
- **Room Database** - Local data persistence
- **Hilt** - Dependency injection
- **Coroutines & Flow** - Asynchronous programming
- **CameraX** - Camera integration

### 📐 **Measurement Technology**
- **Computer Vision**: Uses 33 body landmarks for pose detection
- **Geometric Calibration**: Eye-to-ankle measurement represents 85% of body height
- **Real-time Processing**: 30fps pose detection and analysis
- **Sub-pixel Accuracy**: Smoothing algorithms for precise measurements

### 🔧 **Key Components**
- **JumpDetector**: Core measurement and calibration logic
- **StatisticsRepository**: Performance data calculation and aggregation  
- **Surface-aware Database**: Separate tracking for different jump surfaces
- **WakeLock Manager**: Keeps screen active during sessions

## Debug Information

The app provides comprehensive debug information for troubleshooting measurements:

- **Hip Y (Current/Baseline)**: Current and calibrated hip positions in pixels
- **Hip Movement**: Pixel difference from baseline position  
- **Body Height**: Total body height measurement in pixels
- **Pixel Ratio**: Conversion factor from pixels to centimeters
- **Confidence Score**: ML Kit pose detection confidence
- **Landmark Count**: Number of detected body landmarks
- **Depth Information**: Z-coordinate tracking for false-positive filtering

## Usage Tips

### 📏 **For Best Results**
1. **Accurate height measurement** is crucial - measure from head to floor
2. **Good lighting** improves pose detection accuracy
3. **Stable phone position** - mount or place phone securely
4. **Clear background** helps with person detection
5. **Stay within frame** - ensure full body is visible during calibration

### 🏃‍♂️ **Jump Technique**
- Each session counts as one jump attempt
- Focus on maximum vertical height rather than multiple jumps
- Allow proper calibration before attempting your jump
- Surface type affects results - sand jumps are typically 15-20% lower

### 📊 **Interpreting Statistics**
- **Personal Best**: Your highest recorded jump on each surface
- **Average Height**: Mean performance over selected time periods
- **Consistency Score**: How consistent your performances are (lower deviation = higher score)
- **Surface Comparison**: Performance difference between hard floor and sand

## Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on Android device with camera

## Requirements

- **Android 13.0 (API 33)** or higher
- **Camera permission** for pose detection
- **Storage permission** for data persistence
- **Rear-facing camera** recommended for best results

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

This project is licensed under the MIT License.

---

**MyJumpApp** - Measure your potential, track your progress! 🚀