package com.example.thailandweather.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.*
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.theme.AetherialPalette
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

/**
 * NomadKit Hazard Badge
 * Follows guideline: "Don't rely on color alone to convey meaning".
 * Couples color tone with clear iconography and descriptive uppercase text.
 */
@Composable
fun HazardBadge(
    severity: AlertSeverity,
    isOfficial: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (severity) {
        AlertSeverity.ADVISORY -> Pair(NomadKitPalette.Info, Icons.Default.Info)
        AlertSeverity.WATCH -> Pair(NomadKitPalette.Warning, Icons.Default.WarningAmber)
        AlertSeverity.WARNING -> Pair(NomadKitPalette.Error, Icons.Default.Warning)
        AlertSeverity.EMERGENCY -> Pair(Color(0xFF991B1B), Icons.Default.Dangerous)
    }

    val label = if (isOfficial) "OFFICIAL ${severity.label.uppercase()}" else "RISK: ${severity.label.uppercase()}"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = severity.label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Air Quality Card with NomadKit Forest/Warning/Error indicators.
 */
@Composable
fun AirQualityCard(
    airQuality: AirQualitySnapshot?,
    modifier: Modifier = Modifier
) {
    if (airQuality == null) return

    val category = airQuality.category
    val catColor = Color(category.hexColor)
    val catIcon = when (category) {
        AirQualityCategory.VERY_GOOD, AirQualityCategory.GOOD -> Icons.Default.CheckCircle
        AirQualityCategory.MODERATE -> Icons.Default.Info
        AirQualityCategory.UNHEALTHY_SENSITIVE, AirQualityCategory.UNHEALTHY -> Icons.Default.Warning
        AirQualityCategory.HAZARDOUS -> Icons.Default.Dangerous
    }

    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) AetherialPalette.DarkSurface else AetherialPalette.PastelMint.copy(alpha = 0.28f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) AetherialPalette.DarkBorder else Color.White.copy(alpha = 0.75f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DribbblePalette.NeutralRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Air Quality",
                            tint = DribbblePalette.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Air Quality Index",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "PCD / Air4Thai Standard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = catColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, catColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = category.label,
                            tint = catColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.label.uppercase(),
                            color = catColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PM2.5 Concentration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", airQuality.pm25)} µg/m³",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                }
                Column {
                    Text(
                        text = "PM10",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", airQuality.pm10)} µg/m³",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "US AQI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${airQuality.aqiUs}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = NomadKitPalette.SecondaryOcean,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = airQuality.healthAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Coastal and Marine Conditions Card with Ocean accents.
 */
@Composable
fun MarineConditionsCard(
    marine: MarineForecast?,
    modifier: Modifier = Modifier
) {
    if (marine == null) return

    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground
    val cautionColor = if (marine.isRoughSea) NomadKitPalette.Warning else NomadKitPalette.SecondaryOcean

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) AetherialPalette.DarkSurface else AetherialPalette.PowderBlue.copy(alpha = 0.28f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) AetherialPalette.DarkBorder else Color.White.copy(alpha = 0.75f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DribbblePalette.NeutralRose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sailing,
                            contentDescription = "Marine",
                            tint = DribbblePalette.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Coastal & Marine Conditions",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tide, Swell & Wave Dynamics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (marine.isRoughSea) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = cautionColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cautionColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = cautionColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ROUGH SEAS",
                                color = cautionColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Wave Height", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format("%.1f", marine.waveHeightMeters)} m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (marine.waveHeightMeters >= 2.0) NomadKitPalette.Warning else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(text = "Wave Period", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format("%.1f", marine.wavePeriodSeconds)} s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "Sea Water Temp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${marine.seaWaterTemperatureCelsius?.let { "${it.toInt()}°C" } ?: "29°C"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            marine.advisory?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cautionColor.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cautionColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hourly Forecast Carousel in NomadKit light-packed style.
 */
@Composable
fun HourlyForecastSection(
    hourly: List<HourlyForecast>,
    isCelsius: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = AetherialPalette.IndigoAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hourly Outlook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AetherialPalette.DeepSlateNavy
                )
            }
            val rangeLabel = if (hourly.isNotEmpty()) {
                val endItem = hourly.take(24).last()
                "NEXT 24H · " + WeatherUtils.formatHourlyRange(hourly.first().time, endItem.time).uppercase()
            } else {
                "NEXT 24H →"
            }
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AetherialPalette.IndigoAccent,
                letterSpacing = 0.5.sp
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(hourly.take(24)) { item ->
                val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground
                val isFirst = hourly.firstOrNull() == item
                val isThunder = item.weatherCode in listOf(95, 96, 99) || item.precipitationProbability >= 80
                val isRain = item.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) || item.precipitationProbability >= 50

                val (containerColor, borderColor, iconTint, textColor) = when {
                    isDark -> Quad(AetherialPalette.DarkSurface, AetherialPalette.DarkBorder, AetherialPalette.DarkAccent, AetherialPalette.DarkText)
                    isFirst -> Quad(AetherialPalette.PastelIndigo, AetherialPalette.PastelIndigoBorder, AetherialPalette.AmberWarm, AetherialPalette.IndigoAccent)
                    isThunder -> Quad(AetherialPalette.PastelCoral, AetherialPalette.PastelCoralBorder, AetherialPalette.Coral, AetherialPalette.CoralText)
                    isRain -> Quad(AetherialPalette.PastelSky, AetherialPalette.PastelSkyBorder, AetherialPalette.SkyBlue, AetherialPalette.SkyBlueText)
                    else -> Quad(AetherialPalette.Surface, AetherialPalette.Outline, AetherialPalette.AmberWarm, AetherialPalette.DeepSlateNavy)
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.width(88.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Day Label (Today / Tomorrow / Mon)
                        Text(
                            text = WeatherUtils.formatHourlyDay(item.time),
                            fontSize = 10.sp,
                            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isFirst) AetherialPalette.IndigoAccent else textColor.copy(alpha = 0.8f)
                        )
                        // Date Label (2 Nov / 3 Nov)
                        Text(
                            text = WeatherUtils.formatHourlyDateOnly(item.time),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFirst) AetherialPalette.IndigoAccent.copy(alpha = 0.85f) else textColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        // Hour / "NOW"
                        Text(
                            text = if (isFirst) "NOW" else WeatherUtils.formatTime(item.time),
                            fontSize = 12.sp,
                            fontWeight = if (isFirst) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isFirst) AetherialPalette.IndigoAccent else textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            imageVector = getWeatherIcon(item.weatherCode),
                            contentDescription = item.weatherDescription,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = WeatherUtils.formatTemperature(item.temperatureCelsius, isCelsius),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) AetherialPalette.DarkText else AetherialPalette.DeepSlateNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (isThunder) AetherialPalette.Coral else if (isRain) AetherialPalette.SkyBlue else AetherialPalette.IndigoAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${item.precipitationProbability}%",
                                fontSize = 10.sp,
                                color = if (isThunder) AetherialPalette.CoralText else if (isRain) AetherialPalette.SkyBlueText else AetherialPalette.IndigoAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Auto-wraps important weather hazards, regions, and metrics in Markdown bold (**bold**)
 * if no bold formatting is present in the source text.
 */
fun ensureHighlightedMarkdown(rawText: String): String {
    if (rawText.contains("**")) return rawText

    val keywords = listOf(
        "TMD Warning", "TMD Alert", "DDPM Watch", "heavy to very heavy rain",
        "very heavy rain", "heavy rain", "flash flood", "strong gusts", "strong winds",
        "high swells", "thundershowers", "downpours", "street flooding", "monsoonal trough",
        "southwest monsoon", "PM2.5 inversion", "PM2.5", "good air quality", "clear skies",
        "isolated heavy downpours", "rising river discharge", "mask advisories",
        "Chao Phraya basin", "Chao Phraya", "Bangkok", "Chiang Mai", "Chiang Rai",
        "Mae Hong Son", "Phuket", "Krabi", "Surat Thani", "Chon Buri", "Trat",
        "Phang Nga", "Ranong", "Andaman Sea", "Gulf"
    )

    var result = rawText
    for (kw in keywords) {
        result = result.replace(Regex("(?i)\\b(${Regex.escape(kw)})\\b")) { match ->
            "**${match.value}**"
        }
    }

    // Highlight metrics, wave heights, and times (e.g., 2-3 m, 31°C, 16:00, 73%)
    result = result.replace(Regex("""(?i)\b(\d+(?:-\d+)?\s*(?:°C|m\b|km/h|µg/m³|%|:00\b))""")) { match ->
        "**${match.value}**"
    }

    return result
}

/**
 * Builds an AnnotatedString parsing Markdown bold markers (**word/phrase**) into highlighted bold spans.
 */
fun buildHighlightedAnnotatedString(
    text: String,
    textColor: Color,
    highlightColor: Color
): AnnotatedString {
    val preparedText = ensureHighlightedMarkdown(text)
    return buildAnnotatedString {
        val regex = Regex("""\*\*(.*?)\*\*""")
        var lastIndex = 0
        for (match in regex.findAll(preparedText)) {
            val range = match.range
            if (range.first > lastIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(preparedText.substring(lastIndex, range.first))
                }
            }
            val boldText = match.groupValues[1]
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append(boldText)
            }
            lastIndex = range.last + 1
        }
        if (lastIndex < preparedText.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(preparedText.substring(lastIndex))
            }
        }
    }
}

/**
 * Renders brief, highlighted text with Markdown bold (**bold**) support.
 */
@Composable
fun HighlightedMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = AetherialPalette.DeepSlateNavy,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    lineHeight: TextUnit = 22.sp
) {
    val annotated = remember(text, color, highlightColor) {
        buildHighlightedAnnotatedString(text, color, highlightColor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        lineHeight = lineHeight
    )
}

/**
 * Renders short, brief bullet points with stylish custom bullet dots and highlighted key terms.
 * Supports bullet format, sentence splitting, and maximum item counts.
 */
@Composable
fun HighlightedMarkdownBulletList(
    text: String,
    modifier: Modifier = Modifier,
    maxItems: Int = Int.MAX_VALUE,
    bulletColor: Color = AetherialPalette.IndigoAccent,
    highlightColor: Color = AetherialPalette.DeepSlateNavy,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    verticalSpacing: Dp = 8.dp
) {
    val items = remember(text, maxItems) {
        val rawLines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val bulletLines = if (rawLines.size <= 1 && text.contains(". ")) {
            // Split dense paragraph into brief, bite-sized bullet points
            text.split(Regex("""(?<=[.!?])\s+"""))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else {
            rawLines
        }

        bulletLines
            .map { line ->
                line.removePrefix("•").removePrefix("-").removePrefix("*").trim()
            }
            .filter { it.isNotBlank() }
            .take(maxItems)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items.forEach { bulletText ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(bulletColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                HighlightedMarkdownText(
                    text = bulletText,
                    color = textColor,
                    highlightColor = highlightColor,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Offline-First Stale Data Banner.
 * Sleek atmospheric capsule matching reference design with offline snapshot status & sync action.
 */
@Composable
fun StaleDataBanner(
    isStale: Boolean,
    dataSource: String,
    modifier: Modifier = Modifier,
    onSync: () -> Unit = {}
) {
    if (!isStale) return
    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground

    Surface(
        color = if (isDark) AetherialPalette.DarkSurface else Color.White,
        shape = RoundedCornerShape(percent = 50),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) AetherialPalette.DarkBorder else AetherialPalette.Outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline Cache",
                        tint = AetherialPalette.FaintNavy,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AetherialPalette.Coral)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OFFLINE SNAPSHOT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AetherialPalette.CoralText,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Cached snapshot • Ready for offline travel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelIndigo,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelIndigoBorder
                ),
                modifier = Modifier.clickable { onSync() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = AetherialPalette.IndigoAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AetherialPalette.IndigoAccent
                    )
                }
            }
        }
    }
}

/**
 * NomadKit Shimmer Animation Modifier for Skeleton States.
 */
@Composable
fun Modifier.nomadShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "nomadShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nomadShimmerTranslate"
    )

    val shimmerColors = listOf(
        NomadKitPalette.SurfaceVariant.copy(alpha = 0.6f),
        NomadKitPalette.PrimarySandLight.copy(alpha = 0.5f),
        NomadKitPalette.SurfaceVariant.copy(alpha = 0.6f)
    )

    return this.background(
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )
    )
}

/**
 * Shimmering skeleton for the Weather Hero Card (Offline-First).
 */
@Composable
fun NomadSkeletonHeroCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(modifier = Modifier.width(140.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).nomadShimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(90.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
                }
                Box(modifier = Modifier.size(54.dp).clip(CircleShape).nomadShimmer())
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Box(modifier = Modifier.width(110.dp).height(64.dp).clip(RoundedCornerShape(8.dp)).nomadShimmer())
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(130.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = NomadKitPalette.Outline)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).nomadShimmer())
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.width(42.dp).height(12.dp).clip(RoundedCornerShape(3.dp)).nomadShimmer())
                    }
                }
            }
        }
    }
}

/**
 * Shimmering skeleton for the Hourly Forecast Row.
 */
@Composable
fun NomadSkeletonHourlyRow(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(115.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .nomadShimmer()
                )
            }
        }
    }
}

/**
 * Shimmering skeleton for generic list cards.
 */
@Composable
fun NomadSkeletonListCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(0.85f).height(18.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).nomadShimmer())
        }
    }
}

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.CloudQueue
        3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.BlurOn
        51, 53, 55, 61, 63 -> Icons.Default.Grain
        65, 80, 81, 82 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbCloudy
    }
}
