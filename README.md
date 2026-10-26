# 🇹🇭 Thailand Weather Intelligence (ไทย เวทเธอร์ อินเทลลิเจนซ์)

> A production-minded, native Android application engineered specifically for Thailand's tropical meteorology, monsoons, official alerts, environmental air quality (PM2.5), Doppler radar, marine conditions, and disaster news.

---

## 1. Product Overview & Key Features

Thailand's tropical climate poses distinct, life-impacting challenges: sudden tropical monsoon depressions, urban flash flooding along the Chao Phraya basin, dangerous apparent heat indices exceeding 42°C, coastal wave swells affecting passenger ferries, and seasonal atmospheric PM2.5 smog trapping northern valleys. 

**Thailand Weather Intelligence** eliminates the friction of switching between separate weather applications, government department websites (TMD, DDPM, PCD), news portals, and radar tools by combining them into one unified, location-aware native Android experience.

### Core Capabilities
* **Full Forecast Suite**: Current observations, 24-hour hourly outlooks, 7-day daily forecasts, precipitation probabilities, rainfall sums, wind speed/direction, relative humidity, apparent feels-like temperature, and UV indices.
* **Environmental & Air Quality**: Real-time PM2.5, PM10, European AQI, US AQI, and official Pollution Control Department (PCD / Air4Thai) health recommendations.
* **Coastal & Marine Weather**: Wave heights, wave periods, swell wave conditions, sea surface water temperatures, and rough sea advisories for Thailand's islands and coastal hubs (Phuket, Koh Samui, Pattaya, Krabi, Hua Hin, Koh Tao).
* **Unlimited Places & Proximity Engine**: Add unlimited locations across all 77 Thai provinces, custom coordinate pins, or search landmarks. Supports custom grouping (e.g. *Central Capital*, *Andaman Coast*, *Northern Highlands*) alongside automatic **Haversine proximity clustering** (5, 10, 25, 50 km).
* **Interactive Thailand Map & TMD Radar Viewer**: Compose-native map canvas with province contours, clickable pins, reflectivity radar sweeps, and live Doppler loop playback with time scrubbing (-60m, -45m, -30m, -15m, Live).
* **Verified News & Incident Ingestion**: Curated Thailand weather reports, river discharge updates, and severe storm news with automated headline deduplication and offline bookmarking.
* **Two-Tier Alert Safety Architecture**: Strictly distinguishes **Official Government Warnings** (TMD & DDPM with CAP compliance) from **App-Generated Forecast Risks** (Extreme Heat, Torrential Downpours, Gale Winds, Rough Sea swells, Hazardous PM2.5).
* **Offline-First Resilience**: Powered by Room SQLite and DataStore; cached forecasts, saved places, and bookmarked articles remain accessible offline with prominent stale-data indicators.

---

## 2. Architecture & Tech Stack

```
┌────────────────────────────────────────────────────────────────────────┐
│                        UI Layer (Jetpack Compose)                      │
│   ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────┐ │
│   │    Home    │ │   Explore  │ │   Places   │ │    News    │ │Alerts│ │
│   │ (Snapshot) │ │(Map/Radar) │ │(Clusters)  │ │ (Articles) │ │ (CAP)│ │
│   └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └───┬──┘ │
└─────────┼──────────────┼──────────────┼──────────────┼────────────┼────┘
          ▼              ▼              ▼              ▼            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     ViewModel & State Layer (M3)                       │
│    StateFlow / UI State / User Intent Handlers / Units & Locale Flow   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Domain / Repository Layer                       │
│  WeatherRepository | PlacesRepository | AlertsRepository | NewsRepo    │
│            Haversine Proximity Clustering Engine                       │
└───────────────────┬───────────────────────────────┬────────────────────┘
                    ▼                               ▼
┌───────────────────────────────────────┐ ┌──────────────────────────────┐
│       Local Storage (Room & DataStore)│ │  Remote Integration Layer    │
│  - PlacesDao (unlimited places)       │ │  - WeatherProvider Interface │
│  - WeatherCacheDao (TTL & stale flags)│ │    ├─ OpenMeteoProvider      │
│  - AlertsDao (official vs risk)       │ │    └─ Fallback/Direct TMD    │
│  - NewsDao (deduplicated articles)    │ │  - TMD Radar & Satellite API │
│  - UserPreferencesDataStore           │ │  - TMD RSS / News Ingestion  │
└───────────────────────────────────────┘ └──────────────────────────────┘
```

* **Kotlin & Coroutines**: StateFlow reactive streams, structured concurrency, and async pipelines.
* **Jetpack Compose & Material 3**: Dynamic theming (Light/Dark mode), customized semantic hazard color tokens, and responsive layouts.
* **Room Database 2.6.1**: Local source of truth for places, weather snapshots, alerts, and articles.
* **AndroidX DataStore**: Asynchronous preference storage for temperature units (C/F), wind units (km/h vs mph), language (Thai/English), and notification channel toggles.
* **Retrofit 2.11 & OkHttp 4.12 & Kotlinx Serialization**: Resilient HTTP client with JSON parsing, connection timeouts, and logging.
* **WorkManager 2.9.1**: Background periodic synchronization complying with Android battery and Doze restrictions.
* **Android Notification Channels**: Distinct channels for Official Warnings (High importance), Forecast Risks (Default importance), and Environmental Advisories.

---

## 3. Verified Data Sources & Licensing

| Source | Role | Endpoints & Assets | Licensing / Terms |
| :--- | :--- | :--- | :--- |
| **Open-Meteo** | Primary forecasting engine | `api.open-meteo.com/v1/forecast`<br>`air-quality-api.open-meteo.com/v1/air-quality`<br>`marine-api.open-meteo.com/v1/marine`<br>`geocoding-api.open-meteo.com/v1/search` | **CC-BY 4.0** (Open Access). In-app attribution clearly displayed. |
| **Thai Meteorological Department (TMD)** | Official warnings, Doppler radar & satellite | `tmdradar.tmd.go.th`<br>`tmd.go.th/en/Weather`<br>`tmd.go.th/en/service/rss` | Royal Thai Government public meteorological notices and radar imagery. |
| **Pollution Control Department (PCD)** | Air quality thresholds | `air4thai.pcd.go.th` | Thai National Environmental Quality Standards (PM2.5 µg/m³ breakpoints). |
| **DDPM (Disaster Prevention)** | Flood & river basin warnings | `disaster.go.th` | Public safety advisories. |
| **Thai News Publishers** | Climate & severe weather headlines | Bangkok Post, The Nation Thailand, Thai PBS World | Headline, summary, timestamp, and deep link attribution. |

---

## 4. Project Structure

```
Weather/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── kotlin/com/example/thailandweather/
│   │   │   │   ├── WeatherApplication.kt          # Global Application container & DI
│   │   │   │   ├── MainActivity.kt                # Single Activity Compose entry
│   │   │   │   ├── core/
│   │   │   │   │   ├── model/WeatherModels.kt     # Domain data structures
│   │   │   │   │   ├── database/                  # Room Database, Entities, DAOs
│   │   │   │   │   ├── datastore/                 # DataStore Preferences
│   │   │   │   │   └── util/                      # Haversine, WeatherUtils, WMO
│   │   │   │   ├── data/
│   │   │   │   │   ├── provider/                  # WeatherProvider Interface & OpenMeteo
│   │   │   │   │   ├── remote/                    # Retrofit DTOs & Radar Catalog
│   │   │   │   │   └── repository/                # Places, Weather, Alerts, News Repos
│   │   │   │   ├── notification/                  # Android Notification Channels & Cooldowns
│   │   │   │   ├── worker/                        # WorkManager periodic sync
│   │   │   │   └── ui/
│   │   │   │       ├── components/                # Hazard badges, AirQualityCard, MarineCard
│   │   │   │       ├── home/                      # HomeScreen & HomeViewModel
│   │   │   │       ├── explore/                   # ExploreScreen, Canvas Map & Radar Loops
│   │   │   │       ├── places/                    # PlacesScreen & Comparison Dashboard
│   │   │   │       ├── news/                      # NewsScreen, Filters & Bookmarks
│   │   │   │       ├── alerts/                    # AlertsScreen & Detail Sheets
│   │   │   │       ├── settings/                  # SettingsScreen & Attribution
│   │   │   │       ├── theme/                     # Material 3 Color, Theme & Typography
│   │   │   │       └── navigation/                # Bottom Navigation Bar
│   │   └── test/                                  # Automated Unit Test Suites
│   └── build.gradle.kts
├── backend/                                       # Optional Python/FastAPI microservice
│   ├── main.py                                    # Ingestion & geospatial matching
│   ├── data_sources.py
│   ├── requirements.txt
│   └── README.md
├── gradle/
│   ├── wrapper/gradle-wrapper.properties
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 5. Build, Test & Run Instructions

### Prerequisites
* Java JDK 21+ (`openjdk 21` configured)
* Android SDK 34 - 36 (installed in `~/Library/Android/sdk` or specified via `ANDROID_HOME`)

### Building the Debug APK
To assemble the full debug APK:
```bash
./gradlew assembleDebug --no-daemon
```
The resulting APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Automated Tests
To run all unit test suites (Haversine calculations, WeatherProvider DTO mapping, AirQuality calculations, News deduplication, and Alert risk evaluation):
```bash
./gradlew test --no-daemon
```

### Running the Backend Microservice (Optional)
```bash
cd backend
pip install -r requirements.txt
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
Interactive API docs will be available at `http://localhost:8000/docs`.

---

## 6. Known Limitations & Operational Considerations
1. **Direct TMD Radar Availability**: Doppler radar loop GIFs from `tmdradar.tmd.go.th` depend on government server uptime. If a regional Doppler radar station is undergoing calibration, the app marks it and seamlessly falls back to the nationwide composite overlay.
2. **Offline Warning Boundaries**: In airplane/offline mode, Room SQLite serves previously retrieved warnings with clear `isStale = true` indicators. The app never fabricates newly issued disaster declarations when offline.
3. **Emergency Disclaimers**: The app clearly labels app-generated forecast risks (such as estimated Heat Indices or monsoon rain warnings) distinctly from legal official orders issued by the Director-General of TMD or the Department of Disaster Prevention and Mitigation.
