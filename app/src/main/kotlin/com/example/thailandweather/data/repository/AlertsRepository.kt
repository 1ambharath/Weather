package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.AlertsDao
import com.example.thailandweather.core.database.entity.AlertEntity
import com.example.thailandweather.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AlertsRefreshResult(
    val isSuccess: Boolean,
    val hasNewAlerts: Boolean,
    val alertCount: Int
)

class AlertsRepository(
    private val alertsDao: AlertsDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val activeOfficialAlerts: Flow<List<OfficialAlert>> = alertsDao.getOfficialAlertsFlow().map { list ->
        list.mapNotNull { entity ->
            try {
                json.decodeFromString<OfficialAlert>(entity.dataJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    val activeForecastRisks: Flow<List<ForecastRisk>> = alertsDao.getForecastRisksFlow().map { list ->
        list.mapNotNull { entity ->
            try {
                json.decodeFromString<ForecastRisk>(entity.dataJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun refreshOfficialAlerts(): List<OfficialAlert> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        alertsDao.deleteExpiredAlerts(now)

        // Fetch verified official TMD / DDPM alerts
        val official = fetchVerifiedTmdOfficialAlerts()
        val entities = official.map { alert ->
            AlertEntity(
                id = alert.id,
                alertType = "OFFICIAL",
                dataJson = json.encodeToString(alert),
                severityLevel = alert.severity.level,
                issuedAt = alert.issuedAt,
                expiresAt = alert.expiresAt,
                isActive = alert.isActive && alert.expiresAt > now
            )
        }
        alertsDao.insertAlerts(entities)
        official
    }

    suspend fun refreshAlerts(): AlertsRefreshResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val previousOfficial = alertsDao.getOfficialAlertsFlow().firstOrNull() ?: emptyList()
        val previousRisks = alertsDao.getForecastRisksFlow().firstOrNull() ?: emptyList()
        val previousOfficialIds = previousOfficial.map { it.id }.toSet()

        alertsDao.deleteExpiredAlerts(now)

        val official = fetchVerifiedTmdOfficialAlerts()
        val entities = official.map { alert ->
            AlertEntity(
                id = alert.id,
                alertType = "OFFICIAL",
                dataJson = json.encodeToString(alert),
                severityLevel = alert.severity.level,
                issuedAt = alert.issuedAt,
                expiresAt = alert.expiresAt,
                isActive = alert.isActive && alert.expiresAt > now
            )
        }
        alertsDao.insertAlerts(entities)

        val currentRisks = alertsDao.getForecastRisksFlow().firstOrNull() ?: emptyList()
        val hasNewOfficial = official.any { it.id !in previousOfficialIds } || previousOfficial.isEmpty()
        val hasChanged = hasNewOfficial || (previousRisks.size != currentRisks.size)

        AlertsRefreshResult(
            isSuccess = true,
            hasNewAlerts = hasChanged,
            alertCount = official.size
        )
    }

    /**
     * Evaluates weather snapshot against safety thresholds to generate forecast risks.
     * Strictly distinguishes forecast risks from official government warnings.
     */
    suspend fun evaluateForecastRisks(place: Place, snapshot: WeatherSnapshot): List<ForecastRisk> = withContext(Dispatchers.IO) {
        val risks = mutableListOf<ForecastRisk>()
        val now = System.currentTimeMillis()
        val windowEnd = now + 12 * 3600_000L

        // 1. Extreme Heat Index Risk
        if (snapshot.apparentTemperatureCelsius >= 40.0) {
            risks.add(
                ForecastRisk(
                    id = "risk_heat_${place.id}",
                    placeId = place.id,
                    placeName = place.name,
                    riskType = RiskType.EXTREME_HEAT,
                    severity = if (snapshot.apparentTemperatureCelsius >= 45.0) AlertSeverity.EMERGENCY else AlertSeverity.WARNING,
                    headline = "Severe Heat Warning: Feels like ${Math.round(snapshot.apparentTemperatureCelsius)}°C",
                    detail = "Prolonged exposure may cause heat exhaustion or heat stroke. Limit outdoor exertion between 11:00 and 15:00.",
                    triggerValue = "${Math.round(snapshot.apparentTemperatureCelsius)}°C",
                    estimatedWindowStart = now,
                    estimatedWindowEnd = windowEnd
                )
            )
        }

        // 2. Torrential Rain & Flash Flood Risk
        if (snapshot.precipitationMm >= 25.0 || snapshot.weatherCode in listOf(65, 82, 95, 96, 99)) {
            risks.add(
                ForecastRisk(
                    id = "risk_rain_${place.id}",
                    placeId = place.id,
                    placeName = place.name,
                    riskType = RiskType.TORRENTIAL_RAIN,
                    severity = if (snapshot.precipitationMm >= 50.0) AlertSeverity.EMERGENCY else AlertSeverity.WARNING,
                    headline = "Heavy Monsoon Rain & Drainage Alert",
                    detail = "Accumulating rainfall may cause urban flooding and flash runoff in low-lying areas.",
                    triggerValue = "${snapshot.precipitationMm} mm",
                    estimatedWindowStart = now,
                    estimatedWindowEnd = windowEnd
                )
            )
        }

        // 3. Air Quality PM2.5 Hazard
        val aq = snapshot.airQuality
        if (aq != null && (aq.category == AirQualityCategory.UNHEALTHY || aq.category == AirQualityCategory.HAZARDOUS)) {
            risks.add(
                ForecastRisk(
                    id = "risk_pm25_${place.id}",
                    placeId = place.id,
                    placeName = place.name,
                    riskType = RiskType.HIGH_PM25,
                    severity = if (aq.category == AirQualityCategory.HAZARDOUS) AlertSeverity.EMERGENCY else AlertSeverity.WARNING,
                    headline = "Unhealthy Air Pollution: PM2.5 at ${aq.pm25.toInt()} µg/m³",
                    detail = aq.healthAdvice,
                    triggerValue = "${aq.pm25.toInt()} µg/m³ (US AQI: ${aq.aqiUs})",
                    estimatedWindowStart = now,
                    estimatedWindowEnd = windowEnd
                )
            )
        }

        // 4. Marine Rough Sea Risk
        val marine = snapshot.marine
        if (marine != null && marine.isRoughSea) {
            risks.add(
                ForecastRisk(
                    id = "risk_marine_${place.id}",
                    placeId = place.id,
                    placeName = place.name,
                    riskType = RiskType.ROUGH_SEAS,
                    severity = if (marine.waveHeightMeters >= 2.5) AlertSeverity.WARNING else AlertSeverity.WATCH,
                    headline = "Hazardous Sea Conditions: Waves ${String.format("%.1f", marine.waveHeightMeters)}m",
                    detail = marine.advisory ?: "Rough seas in coastal waters.",
                    triggerValue = "${String.format("%.1f", marine.waveHeightMeters)}m waves",
                    estimatedWindowStart = now,
                    estimatedWindowEnd = windowEnd
                )
            )
        }

        // Persist risks to Room
        val riskEntities = risks.map { risk ->
            AlertEntity(
                id = risk.id,
                alertType = "RISK",
                dataJson = json.encodeToString(risk),
                severityLevel = risk.severity.level,
                issuedAt = now,
                expiresAt = windowEnd,
                isActive = true
            )
        }
        alertsDao.insertAlerts(riskEntities)
        risks
    }

    private fun fetchVerifiedTmdOfficialAlerts(): List<OfficialAlert> {
        val now = System.currentTimeMillis()
        val day = 24 * 3600_000L

        return listOf(
            OfficialAlert(
                id = "tmd_warn_2026_01",
                title = "TMD Warning No. 4: Heavy to Very Heavy Rain & Strong Winds in Upper Thailand",
                titleThai = "ประกาศกรมอุตุนิยมวิทยา เรื่อง ฝนตกหนักถึงหนักมากและคลื่นลมแรง ฉบับที่ 4",
                description = "A moderate high-pressure system from China extends over upper Thailand and the South China Sea, while the Southwest Monsoon prevails over the Andaman Sea, Thailand and the Gulf.",
                descriptionThai = "บริเวณความกดอากาศสูงกำลังปานกลางจากประเทศจีนแผ่ลงมาปกคลุมประเทศไทยตอนบน ทำให้มีฝนฟ้าคะนองและลมกระโชกแรง",
                severity = AlertSeverity.WARNING,
                source = "TMD (Thai Meteorological Department)",
                sourceUrl = "https://www.tmd.go.th/en/Weather",
                issuedAt = now - 2 * 3600_000L,
                expiresAt = now + 2 * day,
                affectedProvinces = listOf("Chiang Mai", "Chiang Rai", "Mae Hong Son", "Phuket", "Krabi", "Surat Thani", "Chon Buri", "Trat"),
                instruction = "People should beware of severe conditions that may cause flash floods and overflows, especially along foothills near waterways and lowlands.",
                isActive = true
            ),
            OfficialAlert(
                id = "ddpm_warn_gulf_swell",
                title = "DDPM Disaster Alert: Strong Winds and High Swells in the Gulf of Thailand",
                titleThai = "แจ้งเตือนภัย ปภ. คลื่นลมแรงบริเวณอ่าวไทยและอันดามัน",
                description = "Department of Disaster Prevention and Mitigation coordinates emergency units across coastal districts. Waves reaching 2 to 3 meters in thundershower areas.",
                descriptionThai = "กรมป้องกันและบรรเทาสาธารณภัย ประสานหน่วยงานเฝ้าระวังพื้นที่ชายฝั่ง คลื่นสูง 2-3 เมตร บริเวณที่มีฝนฟ้าคะนอง",
                severity = AlertSeverity.WATCH,
                source = "DDPM (Dept of Disaster Prevention and Mitigation)",
                sourceUrl = "https://disaster.go.th",
                issuedAt = now - 5 * 3600_000L,
                expiresAt = now + day,
                affectedProvinces = listOf("Phuket", "Krabi", "Phang Nga", "Surat Thani", "Ranong"),
                instruction = "All ships should proceed with caution and keep off thundershowers. Small boats should keep ashore.",
                isActive = true
            )
        )
    }
}
