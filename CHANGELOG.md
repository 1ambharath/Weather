# Changelog

All notable changes to the **Thailand Weather Intelligence** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v1.0.2] - 2026-11-02

### 🚀 What Was Added
* **3-Tier Meteorological Intelligence Briefing**:
  * **Overall Summary (National Synthesis)**: High-level overview covering nationwide monsoon troughs, regional synoptic patterns, and general air quality spread across all 77 provinces.
  * **User Selected Area Briefing (Dedicated Local Analysis)**: Dedicated primary card providing live telemetry chips (`Temp / Feels-like`, `Rain %`, `Wind`, `UV`, `Marine`), risk assessment badge (`LOW RISK`, `MODERATE RISK`, etc.), concise localized bulleted forecast, and tailored travel recommendations.
  * **News & Incident Context**: Integrated government weather bulletins (TMD), emergency disaster risk reports (DDPM), river basin/dam flood discharge notifications, and recent meteorological news articles.
* **Interactive Active Area Switcher**:
  * Added quick-switch dropdown menu directly inside the AI Briefing view, enabling users to seamlessly pivot their localized briefing across all saved destinations (Bangkok, Chiang Mai, Phuket, etc.) and refresh on demand.
* **Room Database v3 Schema**:
  * Added `selectedAreaBriefingJson` column to `AiBriefingEntity` to persist localized telemetry, risk ratings, and travel advice for instant offline reading.

### 🛠️ What Was Fixed & Optimized
* **Multi-Place Telemetry Aggregation**:
  * Updated `DefaultAiRepository.buildCompactContext` to inject comprehensive meteorological telemetry for both the selected active place and all saved destinations into Groq LLM synthesis context.
* **Deterministic In-Place Updates**:
  * Verified production APK signature consistency (`v2 + v3`) signed with `release.jks`. Users can update smoothly from `v1.0.0` and `v1.0.1` without uninstalling.

### 📦 Artifact Verification
* **File Name**: `thailand-weather-v1.0.2.apk`
* **File Size**: 3,180,469 bytes (~3.18 MB)
* **SHA-256**: `77D093608B6BCBD4CF87BB53D6D678083F54C1FF832ED6DF57D8897C19FD9BF6`
* **Certificate SHA-256**: `6bf5bb7ca85e36749ce63faa92e4d04628de5be088b5a2538adbc5f75c7e4dd8`

---

## [v1.0.1] - 2026-11-02

### 🛠️ What Was Fixed
* **Dynamic 24-Hour Sliding Window (Fixes #1)**:
  * Resolved issue where hourly outlook started at `00:00` midnight of today and stopped at `23:00`, displaying past hours instead of future hours.
  * Implemented an ongoing-hour sliding window in `OpenMeteoWeatherProvider.kt` that detects the current hour in `Asia/Bangkok` time and dynamically selects the next 24 consecutive hours spanning today into tomorrow.
* **Date Context on Hourly Cards (Fixes #1)**:
  * Added day indicator (`Today`, `Tomorrow`, or weekday) and calendar date (`d MMM`, e.g. `2 Nov`, `3 Nov`) to each hourly card in `WeatherComponents.kt`.
  * Users can now clearly identify which day each forecast hour belongs to when scrolling across midnight.
* **Dynamic Range Section Header**:
  * Updated the hourly section header from a static label to show the active date range (e.g., `NEXT 24H · TODAY → TOMORROW`).
* **Timezone Consistency**:
  * Standardized timestamp parsing and formatting to Thailand's local timezone (`Asia/Bangkok`) so forecast hours remain accurate for travelers and trip planners worldwide.

### 📦 Artifact Verification
* **File Name**: `thailand-weather-v1.0.1.apk`
* **File Size**: 3,164,086 bytes (~3.16 MB)
* **SHA-256**: `093E58CEF43FD1CF4F517C74CEF5E6174206627EF517CC170225D114E7EA53E0`
* **Certificate SHA-256**: `6bf5bb7ca85e36749ce63faa92e4d04628de5be088b5a2538adbc5f75c7e4dd8`

---

## [v1.0.0] - 2026-11-01

### 🚀 What Was Added
* **End-to-End Meteorological Suite**: Real-time atmospheric telemetry across all 77 Thai provinces with live temperature, humidity, wind gusts, UV index, and cloud cover.
* **TMD Doppler Radar Monitoring**: Interactive radar sweeps across Thailand's radar network (-60m to Live) with reflectivity playback.
* **AI Meteorological Briefings**: On-device natural language analysis powered by Groq's high-throughput LLaMA 3.3 70B model with travel-specific advice and clothing recommendations.
* **Island & Marine Tracking**: Real-time swell height, wave periods, and ferry advisories for Phuket, Koh Samui, Krabi, Pattaya, and Hua Hin.
* **Proximity Clustering**: Haversine clustering algorithm dynamically organizing regional weather stations within custom radii (10 km – 100 km).
* **Two-Tier Warning System**: Strict separation between official TMD/DDPM government severe weather advisories and local forecast thresholds.
* **NomadKit UI & Aesthetics**: Designed using modern Jetpack Compose and Material Design 3 with a warm sand (#D4A373) palette tailored for travel.

### 🛠️ What Was Fixed
* **In-Place App Updates & Signature Consistency**:
  * Configured deterministic Gradle release signing (`signingConfigs.release`) bound to `release.jks`.
  * Added dual-scheme APK signatures (v2 + v3) verified by `apksigner`.
  * Resolved `INSTALL_FAILED_UPDATE_INCOMPATIBLE` so users can seamlessly update the app with each new release without uninstalling previous versions.
* **Uninstallation & State Persistence**:
  * Set `android:hasFragileUserData="false"` in `AndroidManifest.xml` to prevent Android from retaining stale sandbox data when uninstalled.
  * Overhauled `backup_rules.xml` and `data_extraction_rules.xml` to exclude cached Room databases (`thailand_weather.db*`), DataStore protos (`user_preferences.preferences_pb`), WorkManager DBs, and encrypted credential files from Google Drive Auto-Backup.
* **KeyStore Orphan Cleanup**:
  * Implemented `purgeOrphanedPreferences()` in `SecureKeyStorage.kt` to catch hardware key evictions following app uninstall/reinstall, gracefully resetting API key state instead of throwing unrecoverable decryption exceptions.

### 🔄 What Was Changed & Optimized
* **R8 Code & Resource Shrinking**:
  * Configured R8 optimization and resource shrinking with comprehensive keep rules in `app/proguard-rules.pro`.
  * Reduced release APK size from **~22.5 MB down to ~3.16 MB** (>85% footprint reduction).
* **Development Environment Separation**:
  * Configured `applicationIdSuffix = ".debug"` for debug builds so test builds do not overwrite production installations on the same device.
* **Offline & Network Resilience**:
  * Cached seed provinces and offline data queries for seamless performance in low-connectivity island environments.

### 🔒 Security
* Sensitive Groq API credentials are encrypted with hardware-backed Android KeyStore AES-256-GCM.
* Keystore credentials managed securely via local `keystore.properties` (excluded from version control).
* Added ProGuard rules preserving cryptographic interfaces while obfuscating internal application logic.

### 📦 Artifact Verification
* **File Name**: `thailand-weather-v1.0.0.apk`
* **File Size**: 3,164,072 bytes (~3.16 MB)
* **SHA-256**: `E543F7106A298C47AC782269550610F053B2B32B2041ED8B7DCEBC5900E2C495`
* **Certificate SHA-256**: `6bf5bb7ca85e36749ce63faa92e4d04628de5be088b5a2538adbc5f75c7e4dd8`

