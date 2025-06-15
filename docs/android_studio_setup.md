# FGO Bot - Android Studio Setup Guide

## 📋 **Prerequisites**

### 1. System Requirements
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **RAM**: Minimum 8GB (16GB recommended)
- **Storage**: 4GB free space for Android Studio + 2GB for project
- **Java**: JDK 11 or newer (Android Studio includes embedded JDK)

### 2. Required Software
- **Android Studio**: Arctic Fox (2020.3.1) or newer
- **Git**: For version control
- **USB Debugging**: Enabled on your Samsung Galaxy A34 5G

## 🚀 **Step-by-Step Setup Instructions**

### Step 1: Install Android Studio

#### **Windows:**
1. Download Android Studio from: https://developer.android.com/studio
2. Run the `.exe` installer
3. Follow the setup wizard
4. Choose "Standard" installation
5. Wait for SDK components to download

#### **macOS:**
1. Download Android Studio from: https://developer.android.com/studio
2. Open the `.dmg` file
3. Drag Android Studio to Applications folder
4. Launch Android Studio
5. Follow the setup wizard

#### **Linux (Ubuntu):**
```bash
# Download and extract Android Studio
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2022.3.1.21/android-studio-2022.3.1.21-linux.tar.gz
tar -xzf android-studio-*-linux.tar.gz
sudo mv android-studio /opt/
sudo ln -sf /opt/android-studio/bin/studio.sh /usr/local/bin/android-studio

# Install required dependencies
sudo apt update
sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386

# Launch Android Studio
android-studio
```

### Step 2: Configure Android Studio

1. **First Launch Setup:**
   - Choose "Do not import settings" (if first time)
   - Select "Standard" setup type
   - Choose your UI theme
   - Verify SDK components installation

2. **SDK Configuration:**
   - Go to `File > Settings` (Windows/Linux) or `Android Studio > Preferences` (macOS)
   - Navigate to `Appearance & Behavior > System Settings > Android SDK`
   - Ensure these are installed:
     - Android SDK Platform 33 (API level 33)
     - Android SDK Build-Tools 33.0.0+
     - Android Emulator
     - Android SDK Platform-Tools
     - Android SDK Tools

3. **JDK Configuration:**
   - Go to `File > Project Structure > SDK Location`
   - Verify JDK location points to JDK 11 or newer
   - If not set, download and configure JDK 11

### Step 3: Clone the FGO Bot Project

#### **Option A: Using Android Studio (Recommended)**
1. Open Android Studio
2. Click "Get from VCS" on the welcome screen
3. Select "Git"
4. Enter repository URL: `https://github.com/yourusername/fgo-game-bot.git`
5. Choose local directory: `C:\AndroidProjects\fgo-game-bot` (Windows) or `~/AndroidProjects/fgo-game-bot` (macOS/Linux)
6. Click "Clone"

#### **Option B: Using Command Line**
```bash
# Navigate to your projects directory
cd ~/AndroidProjects  # macOS/Linux
# cd C:\AndroidProjects  # Windows

# Clone the repository
git clone https://github.com/yourusername/fgo-game-bot.git
cd fgo-game-bot

# Open in Android Studio
# File > Open > Select the fgo-game-bot folder
```

### Step 4: Project Configuration

1. **Open Project:**
   - If not already open, go to `File > Open`
   - Navigate to the `fgo-game-bot` folder
   - Click "OK"

2. **Gradle Sync:**
   - Android Studio will automatically start Gradle sync
   - If prompted, click "Sync Now"
   - Wait for sync to complete (may take 5-10 minutes first time)

3. **SDK Version Check:**
   - Open `app/build.gradle`
   - Verify these settings:
   ```gradle
   android {
       compileSdk 33
       defaultConfig {
           minSdk 26
           targetSdk 33
       }
   }
   ```

4. **Create local.properties (if needed):**
   - Create `local.properties` file in project root
   - Add your SDK path:
   ```properties
   # Windows
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   
   # macOS
   sdk.dir=/Users/YourUsername/Library/Android/sdk
   
   # Linux
   sdk.dir=/home/yourusername/Android/Sdk
   
   # Optional: Atlas Academy API key (for future use)
   ATLAS_ACADEMY_API_KEY=your_api_key_here
   ```

### Step 5: Device Setup (Samsung Galaxy A34 5G)

1. **Enable Developer Options:**
   - Go to `Settings > About phone`
   - Tap "Build number" 7 times
   - Enter your PIN/password when prompted
   - You'll see "Developer options enabled"

2. **Enable USB Debugging:**
   - Go to `Settings > Developer options`
   - Toggle "USB debugging" ON
   - Toggle "Stay awake" ON (optional, keeps screen on while charging)

3. **Connect Device:**
   - Connect your Samsung Galaxy A34 5G via USB cable
   - When prompted on device, select "File Transfer" or "MTP"
   - Allow USB debugging when prompted
   - Check "Always allow from this computer"

4. **Verify Connection:**
   - In Android Studio, check the device dropdown (top toolbar)
   - Your device should appear as "Samsung SM-A346B" or similar
   - If not visible, try:
     - Disconnect and reconnect USB
     - Restart ADB: `Tools > SDK Manager > SDK Tools > Android SDK Platform-Tools`

### Step 6: Build and Run the Project

1. **Initial Build:**
   - Click `Build > Make Project` or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (macOS)
   - Wait for build to complete
   - Check "Build" tab for any errors

2. **Run Configuration:**
   - Ensure "app" is selected in the configuration dropdown
   - Your Samsung Galaxy A34 5G should be selected as target device

3. **Install and Run:**
   - Click the green "Run" button or press `Shift+F10` (Windows/Linux) / `Ctrl+R` (macOS)
   - Android Studio will:
     - Build the APK
     - Install it on your device
     - Launch the app

4. **First Run Verification:**
   - App should launch showing "FGO Bot" text
   - Check Android Studio's "Run" tab for logs
   - Look for successful installation messages

### Step 7: Enable Accessibility Service (Required for Bot)

1. **On Your Device:**
   - Go to `Settings > Accessibility`
   - Find "FGO Bot" in the list
   - Tap on it and toggle "Use service" ON
   - Grant necessary permissions

2. **Verify Service:**
   - The accessibility service should now be active
   - You can verify in Android Studio logs

## 🔧 **Troubleshooting Common Issues**

### Issue 1: Gradle Sync Failed
**Solution:**
```bash
# In Android Studio terminal
./gradlew clean
./gradlew build
```

### Issue 2: Device Not Detected
**Solutions:**
- Install device drivers (Windows)
- Enable "Install via USB" in Developer options
- Try different USB cable
- Restart ADB: `Tools > SDK Manager > Platform Tools`

### Issue 3: Build Errors
**Check:**
- JDK version (should be 11+)
- SDK versions match build.gradle
- Internet connection for dependency downloads
- Gradle wrapper permissions: `chmod +x gradlew` (Linux/macOS)

### Issue 4: App Crashes on Launch
**Debug Steps:**
1. Check Android Studio's "Logcat" tab
2. Filter by "FGOBot" tag
3. Look for error messages
4. Verify device API level (should be 26+)

### Issue 5: Accessibility Service Not Working
**Solutions:**
- Manually enable in device settings
- Check app permissions
- Restart the app
- Check for Android security restrictions

## 📱 **Testing the Installation**

### Basic Functionality Test:
1. **App Launch:** App opens without crashes
2. **UI Display:** Shows "FGO Bot" interface
3. **Logs:** Check Android Studio logs for errors
4. **Accessibility:** Service can be enabled in settings

### Development Workflow:
1. **Code Changes:** Make a small change in MainActivity.kt
2. **Hot Reload:** Use "Apply Changes" button for quick updates
3. **Full Rebuild:** Use "Run" button for complete rebuild
4. **Debugging:** Set breakpoints and use debugger

## 🎯 **Next Steps After Setup**

1. **Explore Code Structure:**
   - Navigate through `app/src/main/java/com/fgobot/`
   - Understand the Clean Architecture implementation
   - Review database entities and DAOs

2. **Run Tests:**
   - Right-click on `app/src/test` folder
   - Select "Run 'Tests in 'test''"
   - Verify all unit tests pass

3. **Database Inspection:**
   - Use Android Studio's Database Inspector
   - `View > Tool Windows > App Inspection`
   - Connect to your device and inspect Room database

4. **API Testing:**
   - Test Atlas Academy API integration
   - Check network requests in Android Studio

## 🔐 **Security Notes**

- Never commit `local.properties` to version control
- Keep API keys secure
- Use debug builds for development
- Enable ProGuard for release builds

## 📞 **Support**

If you encounter issues:
1. Check Android Studio's "Event Log" for detailed errors
2. Review the troubleshooting section above
3. Ensure all prerequisites are met
4. Verify device compatibility

**Your FGO Bot development environment is now ready!** 🚀 