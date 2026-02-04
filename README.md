# IntelTrace - OSINT-Powered Android Threat Detection

An Android security application that monitors network traffic in real-time to detect suspicious connections using OSINT threat intelligence.

## 🎯 Features

- **Real-time Network Monitoring**: Captures and analyzes all network connections via VPN service
- **OSINT Threat Intelligence**: Integrates with AbuseIPDB, VirusTotal, and URLhaus
- **Smart Detection Engine**: Multi-layered threat analysis with IOC matching
- **Zero Root Required**: Works on non-rooted devices using VpnService API
- **Local-First Privacy**: All data stored locally with optional OSINT queries
- **Material Design 3 UI**: Modern, intuitive interface built with Jetpack Compose

## 📋 Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 28+ (Android 9.0+)
- JDK 11 or newer
- Git

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd IntelTrace_v3
```

### 2. Get API Keys (Optional but Recommended)

#### AbuseIPDB
1. Go to [AbuseIPDB](https://www.abuseipdb.com/)
2. Sign up for a free account
3. Navigate to API section and create an API key
4. Free tier: 1000 queries/day

#### VirusTotal
1. Go to [VirusTotal](https://www.virustotal.com/)
2. Sign up for a free account
3. Get your API key from your profile
4. Free tier: 4 requests/minute

### 3. Configure API Keys

Open `app/build.gradle.kts` and update the API keys:

```kotlin
buildConfigField("String", "ABUSEIPDB_API_KEY", "\"YOUR_ACTUAL_KEY_HERE\"")
buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"YOUR_ACTUAL_KEY_HERE\"")
```

**Or** configure them in the app's Settings screen after installation.

### 4. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Connect an Android device or start an emulator (Android 9.0+)
4. Click Run (Shift+F10)

## 📱 How to Use

### First Launch

1. **Grant VPN Permission**: On first use, the app will request VPN permission
2. **Enable Monitoring**: Toggle the VPN switch on the Dashboard
3. **Configure Settings**: Optionally set threat threshold, API keys, etc.

### Dashboard

- **VPN Status**: Toggle network monitoring on/off
- **Statistics**: View connection counts, threats, and alerts
- **Quick Actions**: Navigate to detailed views

### Connections Screen

- View all network connections in real-time
- Filter by suspicious connections only
- See threat scores and geographic information

### Understanding Threat Scores

- **0-19**: Safe (Green)
- **20-39**: Low Risk (Light Green)
- **40-59**: Medium Risk (Yellow)
- **60-79**: High Risk (Orange)
- **80-100**: Critical (Red)

## 🏗️ Project Structure

```
app/src/main/java/com/example/inteltrace_v3/
├── core/
│   ├── detection/          # Threat detection engine
│   │   ├── ThreatDetectionEngine.kt
│   │   └── IOCMatcher.kt
│   ├── di/                 # Dependency injection
│   │   └── AppModule.kt
│   ├── utils/              # Utility functions
│   └── vpn/                # VPN service implementation
│       ├── IntelTraceVpnService.kt
│       └── PacketParser.kt
├── data/
│   ├── local/
│   │   ├── cache/          # In-memory caching
│   │   ├── database/       # Room database
│   │   └── preferences/    # SharedPreferences
│   ├── remote/             # API services
│   └── repository/         # Data repositories
├── domain/
│   └── models/             # Domain models
└── presentation/
    ├── dashboard/          # Main dashboard UI
    ├── connections/        # Connections list UI
    └── navigation/         # App navigation
```

## 🔧 Configuration Options

### Settings (In-App)

- **Real-time Monitoring**: Enable/disable active scanning
- **Auto-block**: Automatically block high-threat connections
- **Threat Threshold**: Set minimum score for alerts (default: 50)
- **Notifications**: Enable/disable threat notifications
- **Critical Alerts Only**: Only show critical threats
- **Data Retention**: How long to keep connection logs (1-30 days)

### Build Variants

- **Debug**: Includes logging, no code obfuscation
- **Release**: Optimized, obfuscated code with ProGuard

## 📊 Technical Details

### Architecture

- **Pattern**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **UI**: Jetpack Compose
- **Async**: Kotlin Coroutines + Flow

### VPN Service

- Uses Android's `VpnService` API to create a local VPN
- Intercepts packets at the IP layer
- Parses TCP/UDP headers without deep packet inspection
- Forwards packets to maintain connectivity

### Threat Detection

1. **IOC Matching**: Check against known malicious IPs
2. **Port Analysis**: Detect suspicious port usage
3. **OSINT Query**: Query AbuseIPDB/VirusTotal for IP reputation
4. **Scoring Algorithm**: Combine multiple signals for final threat score
5. **Alerting**: Generate alerts based on configured threshold

## 🔒 Privacy & Security

- **No Data Collection**: All analysis is done locally
- **Optional OSINT**: OSINT queries only for flagged connections
- **Local Storage**: All data stored in app's private database
- **Encrypted APIs**: HTTPS for all external API calls
- **No Analytics**: No third-party tracking or analytics

## 🐛 Troubleshooting

### VPN Won't Start

- Ensure no other VPN app is running
- Check if app has VPN permission
- Try revoking and re-granting permission

### No Connections Detected

- Ensure VPN is active (check Dashboard)
- Try opening a browser or any network app
- Check if device has internet connectivity

### API Rate Limits

- Free tiers have request limits
- App caches results for 24 hours
- Consider upgrading API plans for heavy use

### High Battery Drain

- Reduce threat threshold to minimize API calls
- Disable real-time monitoring when not needed
- Increase cache duration in code

## 📝 Development Notes

### Adding New OSINT Sources

1. Create service interface in `data/remote/api/`
2. Add data models in `data/remote/models/`
3. Update `ThreatRepository` to query new source
4. Adjust threat scoring algorithm

### Customizing Threat Detection

Edit `ThreatDetectionEngine.kt`:
- Modify `calculateFinalScore()` for custom scoring
- Add port patterns in `checkSuspiciousPort()`
- Update IOC database in `IOCMatcher.kt`

### Building for Production

1. Update API keys in build.gradle
2. Enable ProGuard rules
3. Test thoroughly on multiple devices
4. Generate signed APK/Bundle

## 🤝 Contributing

This is a student project for educational purposes. Contributions are welcome!

## 📄 License

Educational use only. See project documentation for details.

## ⚠️ Disclaimer

This application is for educational and research purposes. Users are responsible for:
- Complying with local laws regarding network monitoring
- Obtaining necessary API keys
- Using the app ethically and responsibly

Network monitoring should only be performed on devices you own or have explicit permission to monitor.

## 📧 Support

For issues or questions, please refer to project documentation or contact the development team.

---

**Built with ❤️ using Kotlin, Jetpack Compose, and OSINT**
