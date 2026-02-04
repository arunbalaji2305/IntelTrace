# IntelTrace - Quick Reference Guide

## 🚀 Quick Start Commands

### Build and Run
```bash
# Clean build
./gradlew clean build

# Install debug APK
./gradlew installDebug

# Run app
./gradlew installDebug && adb shell am start -n com.example.inteltrace_v3/.MainActivity
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Check code coverage
./gradlew jacocoTestReport
```

---

## 📱 ADB Commands

### Device Management
```bash
# List connected devices
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Uninstall app
adb uninstall com.example.inteltrace_v3

# Clear app data
adb shell pm clear com.example.inteltrace_v3
```

### Debugging
```bash
# View logs (all)
adb logcat

# Filter IntelTrace logs
adb logcat | grep -i "IntelTrace"

# Filter errors only
adb logcat *:E

# Save logs to file
adb logcat > logs.txt

# View database
adb shell
run-as com.example.inteltrace_v3
cd databases
sqlite3 inteltrace_database
```

### VPN Testing
```bash
# Check VPN status
adb shell dumpsys connectivity | grep -i vpn

# Force stop app
adb shell am force-stop com.example.inteltrace_v3

# Simulate network disconnect
adb shell svc wifi disable
adb shell svc wifi enable
```

---

## 🔧 Common Modifications

### Change App Name
**File**: `app/src/main/res/values/strings.xml`
```xml
<string name="app_name">IntelTrace</string>
```

### Change Package Name
1. Right-click package → Refactor → Rename
2. Update `namespace` in `app/build.gradle.kts`
3. Update `applicationId` in `app/build.gradle.kts`
4. Update AndroidManifest.xml

### Add New Dependency
**File**: `gradle/libs.versions.toml`
```toml
[versions]
new-library = "1.0.0"

[libraries]
new-library = { group = "com.example", name = "library", version.ref = "new-library" }
```

**File**: `app/build.gradle.kts`
```kotlin
dependencies {
    implementation(libs.new.library)
}
```

### Change API Keys
**File**: `app/build.gradle.kts`
```kotlin
buildConfigField("String", "ABUSEIPDB_API_KEY", "\"your-key-here\"")
buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"your-key-here\"")
```

---

## 🎨 UI Customization

### Change Primary Color
**File**: `app/src/main/java/com/example/inteltrace_v3/ui/theme/Color.kt`
```kotlin
val md_theme_light_primary = Color(0xFF6750A4) // Change this
val md_theme_dark_primary = Color(0xFFD0BCFF)  // And this
```

### Change App Icon
1. Place new icon in: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
2. Generate adaptive icon:
   - Right-click `res` → New → Image Asset
   - Configure icon
   - Generate

### Modify Dashboard Stats
**File**: `presentation/dashboard/DashboardScreen.kt`
```kotlin
// Add new stat card
StatCard(
    title = "New Stat",
    value = "123",
    icon = Icons.Default.YourIcon,
    color = Color.Blue
)
```

---

## 🛡️ Security Configuration

### Adjust Threat Threshold
**File**: `core/detection/ThreatDetectionEngine.kt`
```kotlin
private fun getThreatLevel(score: Int): ThreatLevel {
    return when {
        score >= 90 -> ThreatLevel.CRITICAL  // Raised from 80
        score >= 70 -> ThreatLevel.HIGH      // Raised from 60
        // ...
    }
}
```

### Add Custom Malicious IPs
**File**: `core/detection/IOCMatcher.kt`
```kotlin
private fun loadDefaultIOCs() {
    maliciousIPs.addAll(
        listOf(
            "192.168.1.1",  // Add your IPs here
            "10.0.0.1"
        )
    )
}
```

### Modify Port Detection
**File**: `core/detection/ThreatDetectionEngine.kt`
```kotlin
private fun checkSuspiciousPort(port: Int, protocol: Int): PortThreat? {
    return when {
        port == 8080 -> PortThreat(30, "Custom reason")
        // Add more rules
    }
}
```

---

## 💾 Database Operations

### View Database
```bash
# Connect to device
adb shell

# Navigate to app data
run-as com.example.inteltrace_v3
cd databases

# Open database
sqlite3 inteltrace_database

# Useful queries
.tables                          # List all tables
SELECT * FROM connections LIMIT 10;
SELECT * FROM threats WHERE threatScore > 50;
SELECT COUNT(*) FROM connections;
```

### Clear Database
```kotlin
// In your code
viewModelScope.launch {
    database.clearAllTables()
}
```

### Export Database
```bash
# Pull database from device
adb exec-out run-as com.example.inteltrace_v3 cat databases/inteltrace_database > local_backup.db

# Push database to device
adb push local_backup.db /sdcard/
adb shell run-as com.example.inteltrace_v3 cp /sdcard/local_backup.db databases/inteltrace_database
```

---

## 🐛 Debugging Tips

### Enable Verbose Logging
**File**: `core/vpn/IntelTraceVpnService.kt`
```kotlin
companion object {
    private const val TAG = "IntelTraceVpnService"
    private const val DEBUG = true  // Enable debug logs
}

if (DEBUG) {
    Log.d(TAG, "Detailed message here")
}
```

### Monitor Network Traffic
```bash
# Watch live connections
adb shell dumpsys connectivity

# Monitor data usage
adb shell dumpsys netstats

# Check DNS
adb shell dumpsys connectivity | grep -i dns
```

### Profile Performance
1. Android Studio → View → Tool Windows → Profiler
2. Select app process
3. Monitor:
   - CPU usage
   - Memory allocation
   - Network requests

---

## 📦 Building & Distribution

### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Generate AAB for Play Store
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Sign APK Manually
```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias inteltrace

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore release.jks app-release-unsigned.apk inteltrace

# Verify signature
jarsigner -verify -verbose -certs app-release.apk

# Align APK
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

---

## 🧪 Testing Scenarios

### Test VPN Functionality
```kotlin
// Manual test checklist
1. Enable VPN
2. Open Chrome
3. Visit google.com
4. Check connections appear
5. Verify threat scores
6. Disable VPN
7. Verify connections stop
```

### Test OSINT Integration
```kotlin
// Test with known IPs
val testIPs = listOf(
    "8.8.8.8",        // Google DNS (should be safe)
    "1.1.1.1",        // Cloudflare (should be safe)
    "185.220.101.1"   // Tor exit (may be flagged)
)

testIPs.forEach { ip ->
    val result = threatRepository.checkIPReputation(ip)
    println("$ip: ${result.threatScore}")
}
```

### Test Database Performance
```kotlin
// Insert test data
viewModelScope.launch {
    val startTime = System.currentTimeMillis()
    repeat(1000) { i ->
        connectionRepository.insertConnection(
            ConnectionEntity(/* ... */)
        )
    }
    val endTime = System.currentTimeMillis()
    Log.d(TAG, "1000 inserts took ${endTime - startTime}ms")
}
```

---

## 🔐 Security Best Practices

### Don't Commit API Keys
**.gitignore** (already included):
```
# API Keys
local.properties
*.jks
*.keystore
```

### Store Keys Securely
```kotlin
// Use BuildConfig for compile-time
val apiKey = BuildConfig.ABUSEIPDB_API_KEY

// Or use Android Keystore for runtime
val keyStore = KeyStore.getInstance("AndroidKeyStore")
```

### Validate User Input
```kotlin
fun isValidIP(ip: String): Boolean {
    return ip.matches(
        Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    )
}
```

---

## 📊 Performance Optimization

### Reduce Memory Usage
```kotlin
// In ConnectionDao.kt - limit results
@Query("SELECT * FROM connections ORDER BY timestamp DESC LIMIT 100")

// In ThreatCache.kt - reduce cache size
private val MAX_CACHE_SIZE = 500  // Instead of 1000
```

### Improve Battery Life
```kotlin
// In IntelTraceVpnService.kt
private data class ConnectionInfo(...) {
    fun shouldAnalyze(): Boolean {
        return packetCount % 100 == 0  // Analyze less frequently
    }
}
```

### Optimize Database Queries
```kotlin
// Add index to frequently queried columns
@Entity(
    tableName = "connections",
    indices = [Index(value = ["destIp", "timestamp"])]
)
```

---

## 🆘 Emergency Fixes

### App Won't Build
```bash
# Nuclear option
rm -rf .gradle
rm -rf build
rm -rf app/build
./gradlew clean
./gradlew build
```

### Hilt Errors
```kotlin
// Verify Application class
@HiltAndroidApp
class IntelTraceApplication : Application()

// Verify in AndroidManifest
android:name=".IntelTraceApplication"

// Verify activity
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

### VPN Won't Start
```kotlin
// Check permissions in AndroidManifest
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

// Check service declaration
<service
    android:name=".core.vpn.IntelTraceVpnService"
    android:permission="android.permission.BIND_VPN_SERVICE">
```

---

## 📚 Useful Links

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [VpnService API](https://developer.android.com/reference/android/net/VpnService)
- [Hilt DI](https://developer.android.com/training/dependency-injection/hilt-android)

---

## 🎓 Pro Tips

1. **Use Logcat Filters**: Create custom filters for your package
2. **Enable USB Debugging Early**: Saves time during development
3. **Keep Device Plugged In**: VPN testing drains battery
4. **Use Emulator for Speed**: Faster iteration than physical device
5. **Commit Often**: Small, focused commits are better
6. **Comment Complex Logic**: Future you will thank you
7. **Test on Multiple Devices**: Different Android versions behave differently
8. **Read Crash Reports**: Android Studio Logcat shows full stack traces

---

**Last Updated**: February 2026
**Version**: 1.0.0

For detailed guides, see:
- [README.md](README.md) - Main project documentation
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Detailed setup instructions
- [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md) - Presentation tips
