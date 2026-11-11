# WiFix

Android app that automates captive portal login for institutional WiFi networks. Connects to the network gateway, fetches the login form, and submits credentials via HTTP.

## Features

- **Automatic gateway discovery** – Uses Android LinkProperties to detect the captive portal gateway IP from the network routing table
- **Captive portal integration** – Registers as a captive portal handler; Android can open the app when a portal is detected
- **Background service** – Periodic login attempts (every 30 minutes) when WiFixing is enabled
- **Credential persistence** – Username and password stored locally in SharedPreferences
- **Debug logs** – In-app log viewer (tap logo) with a 100-entry circular buffer
- **Network status** – Shows gateway IP, connection state, and captive portal detection

## Requirements

- Android 7.0+ (API 24)
- Android Studio or compatible IDE
- JDK 11

## Development

### Prerequisites

1. Install [Android Studio](https://developer.android.com/studio) or the Android SDK command-line tools
2. Set `ANDROID_HOME` or ensure the SDK is at the default location
3. Accept licenses: `sdkmanager --licenses`

### Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Run tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Clean

```bash
./gradlew clean
```

## Project structure

```
app/src/main/java/team/codialo/wifix/
├── MainActivity.kt
├── CaptivePortalActivity.kt
├── logic/
│   ├── login_logic.kt
│   └── WifiFixBackgroundService.kt
└── ui/
    ├── screens/
    │   ├── home.kt
    │   └── logs_modal.kt
    └── theme/
```

## Configuration

The app targets captive portals that use the common form fields `4Tredir`, `magic`, `username`, and `password`. Gateway discovery uses LinkProperties when available; otherwise it falls back to a known gateway (e.g. 172.16.222.1 for IIIT networks).

Portal URLs and query parameters may need adjustment for other networks. See `login_logic.kt` for the current endpoint format.

## Permissions

- `INTERNET` – Login and connectivity checks
- `ACCESS_NETWORK_STATE` – Network monitoring
- `ACCESS_WIFI_STATE` – WiFi status
- `CHANGE_WIFI_STATE` – Network refresh

## License

GPL-3.0. See [LICENSE](LICENSE) for details. If you fork or modify this project, you must release your changes under the same license.
