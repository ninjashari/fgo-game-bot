# FGO Bot - Android Project Setup

## 🚀 **Project Overview**
FGO Bot is an Android application built with modern Android development practices, targeting Android 14 (API 34) with backward compatibility to Android 8.0 (API 26).

## 📱 **Technical Specifications**

### **Target Platform**
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 8.0 (API 26)
- **Compile SDK**: Android 14 (API 34)

### **Development Stack**
- **Language**: Kotlin 1.9.20
- **Build System**: Gradle 8.4 with Kotlin DSL
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture with MVVM pattern
- **Database**: Room Database (SQLite)
- **Networking**: Retrofit + OkHttp
- **Async Operations**: Kotlin Coroutines
- **Testing**: JUnit 4 + Espresso

## 🛠️ **Project Structure**

```
fgo-game-bot/
├── app/
│   ├── build.gradle.kts              # App-level build configuration
│   ├── proguard-rules.pro            # ProGuard rules for release builds
│   └── src/
│       ├── main/
│       │   ├── java/com/fgobot/
│       │   │   └── presentation/
│       │   │       ├── MainActivity.kt
│       │   │       └── theme/
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   ├── res/
│       │   │   ├── values/
│       │   │   │   ├── strings.xml
│       │   │   │   ├── colors.xml
│       │   │   │   └── themes.xml
│       │   │   ├── values-night/
│       │   │   │   └── themes.xml
│       │   │   └── xml/
│       │   │       ├── accessibility_service_config.xml
│       │   │       ├── backup_rules.xml
│       │   │       └── data_extraction_rules.xml
│       │   └── AndroidManifest.xml
│       ├── test/
│       │   └── java/com/fgobot/
│       │       └── ExampleUnitTest.kt
│       └── androidTest/
│           └── java/com/fgobot/
│               └── ExampleInstrumentedTest.kt
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                  # Root-level build configuration
├── settings.gradle.kts               # Project settings
├── gradle.properties                 # Gradle properties
└── local.properties                  # Local development properties
```

## 🔧 **Setup Instructions**

### **Prerequisites**
1. **Android Studio**: Latest stable version (Hedgehog or newer)
2. **JDK**: Java 11 or higher
3. **Android SDK**: API 34 (Android 14)
4. **Git**: For version control

### **Installation Steps**

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd fgo-game-bot
   ```

2. **Configure Local Properties**
   - Update `local.properties` with your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory
   - Wait for Gradle sync to complete

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

5. **Run Tests**
   ```bash
   # Unit tests
   ./gradlew test
   
   # Instrumented tests (requires connected device/emulator)
   ./gradlew connectedAndroidTest
   ```

## 🎨 **Features Implemented**

### ✅ **Completed**
- [x] **Project Structure**: Clean Architecture setup
- [x] **Build System**: Gradle 8.4 with Kotlin DSL
- [x] **UI Framework**: Jetpack Compose with Material Design 3
- [x] **Theme System**: Light/Dark theme support with FGO colors
- [x] **Dependencies**: All core libraries configured
- [x] **Testing Framework**: Unit and instrumented tests
- [x] **Android Manifest**: Permissions and components configured
- [x] **Resource Files**: Strings, colors, themes, and XML configs

### 🔄 **Ready for Implementation**
- [ ] Database layer (Room entities and DAOs)
- [ ] API layer (Retrofit services and models)
- [ ] Business logic (Use cases and repositories)
- [ ] UI components (Screens and composables)
- [ ] Accessibility service implementation

## 🧪 **Testing**

### **Unit Tests**
```bash
./gradlew test
```
- Tests basic functionality and logic
- No Android dependencies required
- Fast execution

### **Instrumented Tests**
```bash
./gradlew connectedAndroidTest
```
- Tests Android-specific functionality
- Requires connected device or emulator
- Tests UI components and Android APIs

## 🚀 **Build Variants**

### **Debug Build**
- Debuggable enabled
- Application ID suffix: `.debug`
- Version name suffix: `-debug`

### **Release Build**
- Code obfuscation enabled (ProGuard)
- Optimized for production
- Signed APK required for distribution

## 📦 **Dependencies**

### **Core Android**
- AndroidX Core KTX
- Lifecycle Runtime KTX
- Activity Compose

### **UI Framework**
- Jetpack Compose BOM
- Material Design 3
- Navigation Compose

### **Data Layer**
- Room Database
- Retrofit + OkHttp
- Gson for JSON parsing

### **Async Operations**
- Kotlin Coroutines

### **Testing**
- JUnit 4
- Espresso
- Mockito

## 🔐 **Security & Privacy**

- **Local Data Storage**: Room database with encryption ready
- **API Communication**: HTTPS only with certificate pinning ready
- **Permissions**: Minimal required permissions declared
- **Backup Rules**: Sensitive data excluded from backups

## 📱 **Compatibility**

- **Android Versions**: 8.0 (API 26) to 14 (API 34)
- **Screen Sizes**: Phone and tablet support
- **Orientations**: Portrait optimized
- **Languages**: English (extensible for localization)

## 🎯 **Next Steps**

1. **Implement Database Layer**: Create Room entities and DAOs
2. **Add API Integration**: Implement Atlas Academy API services
3. **Create UI Components**: Build Compose screens and components
4. **Add Business Logic**: Implement use cases and repositories
5. **Integrate Accessibility Service**: Add automation capabilities

## 📞 **Support**

For setup issues or questions:
1. Check Android Studio's Build Output
2. Verify SDK and dependencies are properly installed
3. Ensure Gradle sync completed successfully
4. Check `local.properties` configuration

---

**Status**: ✅ **Basic Android Project Setup Complete**  
**Next Phase**: Database Layer Implementation 