# FGO Bot - Quick Start Commands

## 🚀 **Essential Commands**

### **Build Commands**
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build and install on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTestCoverage
```

### **Android Studio Shortcuts**

#### **Windows/Linux:**
- **Build Project**: `Ctrl + F9`
- **Run App**: `Shift + F10`
- **Debug App**: `Shift + F9`
- **Sync Gradle**: `Ctrl + Shift + O`
- **Clean Project**: `Build > Clean Project`

#### **macOS:**
- **Build Project**: `Cmd + F9`
- **Run App**: `Ctrl + R`
- **Debug App**: `Ctrl + D`
- **Sync Gradle**: `Cmd + Shift + O`

### **Device Commands**
```bash
# List connected devices
adb devices

# Install APK manually
adb install app/build/outputs/apk/debug/app-debug.apk

# View device logs
adb logcat | grep FGOBot

# Clear app data
adb shell pm clear com.fgobot

# Enable/disable accessibility service
adb shell settings put secure enabled_accessibility_services com.fgobot/.core.FGOAccessibilityService
```

## 📱 **Quick Setup Checklist**

### **Before Starting:**
- [ ] Android Studio installed
- [ ] Samsung Galaxy A34 5G connected
- [ ] USB debugging enabled
- [ ] Developer options enabled

### **Project Setup:**
- [ ] Project cloned/downloaded
- [ ] Gradle sync completed
- [ ] No build errors
- [ ] Device detected in Android Studio

### **First Run:**
- [ ] App builds successfully
- [ ] App installs on device
- [ ] App launches without crashes
- [ ] Accessibility service can be enabled

## 🔧 **Common Issues & Quick Fixes**

### **Gradle Sync Issues:**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf .gradle/
./gradlew build
```

### **Device Not Detected:**
```bash
# Restart ADB
adb kill-server
adb start-server
adb devices
```

### **Build Errors:**
1. Check JDK version: `java -version` (should be 11+)
2. Verify SDK path in `local.properties`
3. Clean and rebuild: `Build > Clean Project`

### **App Crashes:**
1. Check Logcat: `View > Tool Windows > Logcat`
2. Filter by "FGOBot" tag
3. Look for stack traces

## 📊 **Project Structure Quick Reference**

```
fgo-game-bot/
├── app/
│   ├── src/main/java/com/fgobot/
│   │   ├── core/           # Core services & utilities
│   │   ├── data/           # Database & API
│   │   ├── presentation/   # UI components
│   │   └── domain/         # Business logic (future)
│   ├── src/test/           # Unit tests
│   └── build.gradle        # App dependencies
├── docs/                   # Documentation
├── build.gradle           # Project configuration
└── local.properties       # Local SDK paths
```

## 🎯 **Development Workflow**

1. **Make Changes**: Edit Kotlin files
2. **Build**: `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (macOS)
3. **Run**: `Shift+F10` (Windows/Linux) or `Ctrl+R` (macOS)
4. **Test**: Right-click test folder > Run Tests
5. **Debug**: Set breakpoints and use debugger

## 📝 **Important Files to Know**

- **MainActivity.kt**: Main app entry point
- **FGODatabase.kt**: Database configuration
- **AtlasAcademyApi.kt**: API interface
- **AndroidManifest.xml**: App permissions and services
- **build.gradle**: Dependencies and build config

## 🔍 **Debugging Tips**

1. **Use Logcat**: Filter by "FGOBot" tag
2. **Set Breakpoints**: Click line numbers in code
3. **Database Inspector**: `View > Tool Windows > App Inspection`
4. **Network Inspector**: Monitor API calls
5. **Layout Inspector**: Debug UI issues

**Happy coding!** 🚀 