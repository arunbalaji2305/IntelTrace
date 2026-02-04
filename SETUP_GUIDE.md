# IntelTrace - Complete Setup & Development Guide

## 📚 Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Setup Instructions](#setup-instructions)
4. [API Configuration](#api-configuration)
5. [Testing the Application](#testing-the-application)
6. [Understanding the Code](#understanding-the-code)
7. [Customization Guide](#customization-guide)
8. [Deployment](#deployment)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 Project Overview

IntelTrace is an Android security application that:
- Monitors network traffic using VPN technology
- Analyzes connections against OSINT threat databases
- Provides real-time threat alerts
- Works without root access
- Maintains user privacy with local-first architecture

### Key Components
- **VPN Service**: Captures network packets
- **Packet Parser**: Extracts IP, port, protocol information
- **Detection Engine**: Analyzes threats using multiple sources
- **Room Database**: Stores connection logs and threat data
- **Jetpack Compose UI**: Modern, reactive user interface

---

## 🛠️ Technology Stack

### Frontend
- **Jetpack Compose**: Declarative UI framework
- **Material Design 3**: Modern design system
- **Navigation Compose**: Type-safe navigation

### Backend/Core
- **Kotlin**: 100% Kotlin codebase
- **Coroutines & Flow**: Asynchronous programming
- **Hilt**: Dependency injection
- **Room**: Local database (SQLite wrapper)

### Networking
- **VpnService API**: Android's VPN framework
- **Retrofit**: REST API client
- **OkHttp**: HTTP client with logging

### OSINT APIs
- **AbuseIPDB**: IP reputation database
- **VirusTotal**: Multi-engine threat scanner
- **URLhaus**: Malicious URL database

### Architecture
- **MVVM**: Model-View-ViewModel pattern
- **Clean Architecture**: Separation of concerns
- **Repository Pattern**: Data abstraction layer

---

## 🚀 Setup Instructions

### Step 1: Install Prerequisites

#### Required Software
1. **Android Studio** (Hedgehog 2023.1.1 or newer)
   - Download from: https://developer.android.com/studio
   - Install with default settings

2. **JDK 11** (bundled with Android Studio)
   - Verify: Open Terminal and run `java -version`

3. **Git** (optional, for cloning)
   - Download from: https://git-scm.com/

#### System Requirements
- **OS**: Windows 10/11, macOS 10.14+, or Linux
- **RAM**: 8 GB minimum (16 GB recommended)
- **Storage**: 10 GB free space
- **Android Device/Emulator**: Android 9.0 (API 28) or higher

### Step 2: Open the Project

1. Launch Android Studio
2. Click "Open" from the welcome screen
3. Navigate to the IntelTrace_v3 folder
4. Click "OK" to open

### Step 3: Sync Project

1. Wait for Android Studio to index files
2. Click "Sync Project with Gradle Files" (or it will auto-sync)
3. Wait for dependencies to download (first time may take 5-10 minutes)
4. Resolve any sync errors (usually auto-resolved)

### Step 4: Configure SDK

1. Go to File → Project Structure → SDK Location
2. Ensure Android SDK location is set (usually auto-detected)
3. Verify SDK version 34 or higher is installed

---

## 🔑 API Configuration

### Option 1: Configure in Build File (Recommended for Development)

1. Open `app/build.gradle.kts`
2. Find the `buildConfigField` lines:
```kotlin
buildConfigField("String", "ABUSEIPDB_API_KEY", "\"YOUR_ABUSEIPDB_KEY\"")
buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"YOUR_VIRUSTOTAL_KEY\"")
```
3. Replace with your actual keys
4. Sync Gradle

### Option 2: Configure in App (Recommended for Production)

1. Build and install the app
2. Open Settings screen
3. Enter API keys in the appropriate fields
4. Keys are stored securely in SharedPreferences

### Getting API Keys

#### AbuseIPDB (Free)
1. Visit: https://www.abuseipdb.com/
2. Click "Sign Up" → Create account
3. Verify email
4. Go to: https://www.abuseipdb.com/account/api
5. Copy your API key
6. **Free Tier**: 1,000 checks per day

#### VirusTotal (Free)
1. Visit: https://www.virustotal.com/
2. Sign up with Google/email
3. Click your profile picture → API Key
4. Copy your API key
5. **Free Tier**: 4 requests/minute, 500/day

### Testing Without API Keys

The app will work without API keys but with limited functionality:
- IOC matching still works (offline threat database)
- Port analysis still works
- OSINT reputation checks will be skipped
- Threat scores will be based only on local analysis

---

## 📱 Testing the Application

### Using Android Emulator

1. **Create AVD (Android Virtual Device)**:
   - Tools → Device Manager
   - Click "Create Device"
   - Select "Pixel 6" or any phone
   - Select System Image: API 34 (Android 14)
   - Click "Finish"

2. **Run the App**:
   - Click Run button (▶️) or Shift+F10
   - Select your emulator
   - Wait for app to install and launch

3. **Test VPN Functionality**:
   - Toggle VPN switch on Dashboard
   - Grant VPN permission when prompted
   - Open Chrome or any browser
   - Visit websites (google.com, etc.)
   - Return to IntelTrace to see captured connections

### Using Physical Device

1. **Enable Developer Options**:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options now enabled

2. **Enable USB Debugging**:
   - Settings → Developer Options
   - Toggle "USB Debugging" ON

3. **Connect Device**:
   - Connect phone to computer via USB
   - Accept "Allow USB Debugging" on phone
   - In Android Studio, select your device
   - Click Run

4. **Test on Device**:
   - Same steps as emulator
   - Better performance
   - Real network traffic capture

### Testing Scenarios

#### Scenario 1: Normal Browsing
```
1. Enable VPN in IntelTrace
2. Open Chrome
3. Visit safe websites (google.com, youtube.com)
4. Check Connections screen
5. Should see low threat scores (0-20)
```

#### Scenario 2: Suspicious Connections (Testing)
```
1. Enable VPN
2. Visit known test sites or use nmap to scan ports
3. Check for medium threat scores (40-60)
4. Verify alerts are generated
```

#### Scenario 3: OSINT Integration
```
1. Configure API keys
2. Enable VPN
3. Browse various sites
4. Check threat scores updated with OSINT data
5. View country/ISP information in connection details
```

---

## 📖 Understanding the Code

### Core Components Explained

#### 1. VPN Service (`IntelTraceVpnService.kt`)
```kotlin
// Creates a local VPN interface
val builder = Builder()
    .addAddress("10.0.0.2", 24)  // VPN IP address
    .addRoute("0.0.0.0", 0)      // Route all traffic
    .setSession("IntelTrace")
    
vpnInterface = builder.establish()  // Start VPN
```

**How it works**:
- Creates a virtual network interface
- All device traffic flows through this interface
- App reads packets, analyzes, then forwards them
- No actual VPN server needed (local-only)

#### 2. Packet Parser (`PacketParser.kt`)
```kotlin
// Extracts IP header information
val protocol = buffer.get(9).toInt() and 0xFF  // TCP=6, UDP=17
val srcAddress = extractIPv4Address(buffer, 12)
val dstAddress = extractIPv4Address(buffer, 16)
```

**What it does**:
- Reads raw packet bytes
- Parses IP headers (IPv4/IPv6)
- Extracts source/destination IPs
- Extracts port numbers for TCP/UDP
- No deep packet inspection (respects privacy)

#### 3. Threat Detection (`ThreatDetectionEngine.kt`)
```kotlin
// Multi-layered analysis
1. Check IOC database (known bad IPs)
2. Analyze port usage (suspicious ports)
3. Query OSINT APIs (reputation)
4. Calculate final threat score
5. Generate alerts if needed
```

**Detection Logic**:
```
Final Score = OSINT Score + Port Score + Pattern Matching
- OSINT: 0-100 from AbuseIPDB/VirusTotal
- Port: +20 for RAT ports, +15 for Tor, etc.
- Pattern: +10 for known malware signatures
```

#### 4. Database (`Room`)
```kotlin
// Four main tables
- ConnectionEntity: All network connections
- ThreatEntity: IP reputation cache
- AppInfoEntity: Per-app statistics
- AlertEntity: Security alerts
```

#### 5. UI Layer (`Compose`)
```kotlin
// Declarative UI with state management
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI updates automatically when state changes
}
```

---

## 🎨 Customization Guide

### Changing Threat Thresholds

Edit `ThreatDetectionEngine.kt`:
```kotlin
private fun getThreatLevel(score: Int): ThreatLevel {
    return when {
        score >= 80 -> ThreatLevel.CRITICAL  // Change to 90 for stricter
        score >= 60 -> ThreatLevel.HIGH
        score >= 40 -> ThreatLevel.MEDIUM
        score >= 20 -> ThreatLevel.LOW
        else -> ThreatLevel.SAFE
    }
}
```

### Adding Custom IOCs

Edit `IOCMatcher.kt`:
```kotlin
private fun loadDefaultIOCs() {
    maliciousIPs.addAll(
        listOf(
            "185.220.101.1",
            "YOUR.MALICIOUS.IP.HERE",  // Add here
        )
    )
}
```

### Changing UI Colors

Edit `Color.kt` in ui/theme:
```kotlin
val md_theme_light_primary = Color(0xFF6750A4)  // Change primary color
val md_theme_light_error = Color(0xFFB3261E)    // Change error color
```

### Adding New Screens

1. Create ViewModel:
```kotlin
@HiltViewModel
class YourViewModel @Inject constructor() : ViewModel()
```

2. Create Screen:
```kotlin
@Composable
fun YourScreen(viewModel: YourViewModel = hiltViewModel())
```

3. Add to Navigation:
```kotlin
composable("your_route") {
    YourScreen()
}
```

---

## 🚢 Deployment

### Building Debug APK

1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Find APK in: `app/build/outputs/apk/debug/`
3. Transfer to device and install

### Building Release APK

1. **Generate Signing Key**:
```bash
keytool -genkey -v -keystore inteltrace-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias inteltrace
```

2. **Configure Signing** in `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/inteltrace-release-key.jks")
        storePassword = "your-password"
        keyAlias = "inteltrace"
        keyPassword = "your-password"
    }
}
```

3. **Build Release**:
   - Build → Generate Signed Bundle / APK
   - Select APK
   - Choose your keystore
   - Select "release" variant
   - Build

### Play Store Preparation

1. Update `versionCode` and `versionName` in build.gradle
2. Build AAB (Android App Bundle) instead of APK
3. Test thoroughly on multiple devices
4. Prepare store listing (description, screenshots)
5. Submit for review

---

## 🐛 Troubleshooting

### Build Issues

**Problem**: Gradle sync failed
```
Solution:
1. File → Invalidate Caches → Invalidate and Restart
2. Delete .gradle folder in project
3. Sync again
```

**Problem**: Dependency not found
```
Solution:
1. Check internet connection
2. File → Settings → Build Tools → Gradle
3. Enable "Offline mode" → Disable it
4. Sync
```

### Runtime Issues

**Problem**: App crashes on startup
```
Solution:
1. Check Logcat for error
2. Common: Hilt setup issue → Ensure @HiltAndroidApp on Application class
3. Clean build: Build → Clean Project → Rebuild Project
```

**Problem**: VPN not starting
```
Solution:
1. Check if another VPN is active
2. Uninstall other VPN apps for testing
3. Check Logcat for VpnService errors
4. Verify VPN permission granted
```

**Problem**: No connections showing
```
Solution:
1. Verify VPN is actually active (check Android VPN icon)
2. Open browser and visit websites
3. Wait 5-10 seconds
4. Pull to refresh in Connections screen
5. Check Logcat for parsing errors
```

### API Issues

**Problem**: OSINT queries not working
```
Solution:
1. Verify API keys are correct
2. Check API rate limits (AbuseIPDB: 1000/day)
3. Enable logging to see API responses
4. Test API keys in browser/Postman first
```

---

## 📊 Performance Optimization

### Reducing Battery Drain

1. **Adjust Analysis Frequency**:
```kotlin
// In IntelTraceVpnService.kt
fun shouldAnalyze(): Boolean {
    return packetCount % 100 == 0  // Analyze every 100th packet instead of 50
}
```

2. **Increase Cache Duration**:
```kotlin
// In ThreatCache.kt
private val cacheDurationMs = 48 * 60 * 60 * 1000L  // 48 hours instead of 24
```

3. **Disable Real-time Monitoring**:
   - Toggle off in Settings when not needed

### Reducing Memory Usage

1. **Limit Connection History**:
```kotlin
// In ConnectionDao.kt
@Query("SELECT * FROM connections ORDER BY timestamp DESC LIMIT 100")
// Change 100 to 50 or 25
```

2. **Clean Old Data**:
```kotlin
// Schedule periodic cleanup
viewModelScope.launch {
    connectionRepository.deleteOldConnections(retentionDays = 3)
}
```

---

## 🎓 Learning Resources

### Understanding VPN on Android
- [Android VpnService Documentation](https://developer.android.com/reference/android/net/VpnService)
- [Packet Structure Basics](https://en.wikipedia.org/wiki/IPv4#Packet_structure)

### Jetpack Compose
- [Official Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [Compose Pathway](https://developer.android.com/courses/pathways/compose)

### OSINT & Threat Intelligence
- [AbuseIPDB API Docs](https://docs.abuseipdb.com/)
- [VirusTotal API Docs](https://developers.virustotal.com/reference/overview)

---

## 🤝 Next Steps

### Enhancements to Consider

1. **Machine Learning**:
   - Add TensorFlow Lite for pattern recognition
   - Train model on connection patterns
   - Anomaly detection

2. **Advanced Analytics**:
   - Time-series analysis
   - Geolocation mapping
   - Network graphs

3. **Export Features**:
   - PDF report generation
   - CSV export
   - Share threat intelligence

4. **Firewall Functionality**:
   - Block connections in real-time
   - Per-app rules
   - Whitelist/blacklist management

---

**Happy Coding! 🚀**

For questions or issues, refer to the main README or project documentation.
