# Changelog

All notable changes to the **Thailand Weather Intelligence** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
