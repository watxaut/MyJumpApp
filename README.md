# MyJumpApp 🏀

An Android application for measuring and tracking vertical jump height using computer vision and pose detection.


https://github.com/user-attachments/assets/4ab743d7-71be-467b-8a69-d02ab6d68815


## Features

- **Real-time pose detection** using Google ML Kit Vision with automatic hip tracking
- **Multi-user profiles** with individual statistics and personal records
- **Surface type tracking** - Hard Floor (🏀) and Sand (🏖️) with separate analytics
- **Advanced calibration** - 2-second stability detection with pixel-to-cm conversion
- **False-positive filtering** - depth tracking prevents camera movement from registering as jumps
- **Comprehensive statistics** - personal bests, averages, trends, achievements, and streak tracking
- **Material Design 3** UI with dynamic theming and real-time debug information

## How It Works

1. **Setup**: Create user profile with accurate height and select surface type
2. **Calibration**: Stand still for 2 seconds with full body visible for automatic calibration
3. **Jump**: Hip center position tracked in real-time; maximum height recorded per session
4. **Track**: View statistics, compare surfaces, and monitor progress over time

## Technical Stack

- **Kotlin** & **Jetpack Compose** - Modern Android development
- **ML Kit Vision** - 33-landmark pose detection at 30fps
- **Room Database** - Local persistence with surface-aware tracking
- **Hilt** - Dependency injection
- **CameraX** - Camera integration
- **Coroutines & Flow** - Asynchronous operations

### Measurement Technology
- Eye-to-ankle calibration (85% of body height)
- Sub-pixel accuracy with smoothing algorithms
- >95% confidence threshold for pose landmarks
- Three-layer depth filtering system

## Usage Tips

**For Best Results:**
- Measure height accurately (head to floor)
- Use good lighting and stable phone position
- Keep full body in frame during calibration
- Allow proper calibration before jumping

**Note:** Sand jumps are typically 15-20% lower than hard floor jumps.

## Installation

```bash
git clone <repository-url>
# Open in Android Studio, sync Gradle, and run
```

## Requirements

- Android 13.0 (API 33) or higher
- Camera and storage permissions
- Rear-facing camera recommended

## Contributing

Contributions welcome! Submit pull requests or open issues for bugs and feature requests.

## License

MIT License

---

**MyJumpApp** - Measure your potential, track your progress! 🚀
