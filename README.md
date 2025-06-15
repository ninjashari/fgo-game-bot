# FGO Bot

An Android application that automates gameplay in Fate/Grand Order (FGO) by making intelligent decisions based on available servants, craft essences, and battle situations.

## Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11 or newer
- Android SDK 31 or newer
- Samsung Galaxy A34 5G or compatible device
- USB debugging enabled on your device
- FGO game installed on your device

## Development Setup

### 📚 **Detailed Setup Guides**
- **[Complete Android Studio Setup Guide](docs/android_studio_setup.md)** - Step-by-step instructions
- **[Quick Start Commands](docs/quick_start_commands.md)** - Essential commands and shortcuts

### 🚀 **Quick Setup**

1. **Install Prerequisites:**
   - Android Studio Arctic Fox (2020.3.1) or newer
   - JDK 11 or newer
   - Enable USB debugging on Samsung Galaxy A34 5G

2. **Clone and Open:**
   ```bash
   git clone https://github.com/yourusername/fgo-bot.git
   cd fgo-bot
   ```
   - Open project in Android Studio
   - Wait for Gradle sync to complete

3. **Configure Device:**
   - Connect Samsung Galaxy A34 5G via USB
   - Enable Developer Options and USB Debugging
   - Allow USB debugging when prompted

4. **Build and Run:**
   - Click "Run" button in Android Studio
   - App will install and launch on your device
   - Enable Accessibility Service in device settings

## Building the App

1. Debug build:
```bash
./gradlew assembleDebug
```

2. Release build:
```bash
./gradlew assembleRelease
```

## Deploying to Device

### Method 1: Android Studio
1. Connect your Samsung Galaxy A34 5G via USB
2. Enable USB debugging on your device
3. Select your device in Android Studio
4. Click "Run" or press Shift+F10

### Method 2: Command Line
1. Connect your device via USB
2. Enable USB debugging
3. Run:
```bash
./gradlew installDebug
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### UI Tests
```bash
./gradlew connectedAndroidTest -PtestClass=com.fgobot.UITestSuite
```

## Performance Testing

1. Enable developer options on your device
2. Enable GPU rendering profile
3. Run the app in debug mode
4. Monitor performance in Android Studio's CPU profiler

## Battery Testing

1. Enable battery optimization settings
2. Run the app for 1 hour
3. Monitor battery usage in device settings
4. Check battery impact in Android Studio's battery profiler

## Debugging

1. Enable USB debugging
2. Connect device
3. Run in debug mode
4. Use Android Studio's debug tools:
   - Logcat for logs
   - CPU profiler for performance
   - Memory profiler for memory usage
   - Network profiler for API calls

## Common Issues

### Screen Capture Issues
- Ensure screen capture permission is granted
- Check if device supports MediaProjection API
- Verify screen resolution settings

### Performance Issues
- Check memory usage in Android Studio
- Monitor CPU usage
- Verify image processing optimization

### Battery Issues
- Check background processes
- Monitor wake locks
- Verify screen capture frequency

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Atlas Academy API for game data
- OpenCV for image processing
- Android team for development tools 