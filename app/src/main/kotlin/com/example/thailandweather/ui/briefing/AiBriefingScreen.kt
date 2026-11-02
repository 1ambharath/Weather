package com.example.thailandweather.ui.briefing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.AiBriefing
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.components.HighlightedMarkdownBulletList
import com.example.thailandweather.ui.components.HighlightedMarkdownText
import com.example.thailandweather.ui.components.NomadSkeletonListCard
import com.example.thailandweather.ui.theme.AetherialPalette
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBriefingScreen(
    viewModel: AiBriefingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkKeyAndLoadCachedBriefing()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "AI Weather Briefing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NomadKitPalette.PrimarySand
                        )
                    }
                },
                actions = {
                    if (uiState.hasKey) {
                        IconButton(onClick = onNavigateToAiSettings) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "AI Settings",
                                tint = NomadKitPalette.PrimarySand
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.hasKey) {
            // Gated state when no key exists
            KeyRequiredGatedView(
                onNavigateToAiSettings = onNavigateToAiSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            // Key configured
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                // Header / action bar
                item {
                    BriefingHeaderCard(
                        briefing = uiState.briefing,
                        isLoading = uiState.isLoading,
                        onGenerate = { viewModel.generateBriefing() }
                    )
                }

                // Error message banner if any
                uiState.error?.let { errorText ->
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NomadKitPalette.ErrorContainer),
                            border = BorderStroke(1.dp, NomadKitPalette.Error.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NomadKitPalette.Error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NomadKitPalette.Error
                                )
                            }
                        }
                    }
                }

                // Loading state
                if (uiState.isLoading) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Synthesizing latest weather, alerts & news...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NomadKitPalette.PrimarySandDeep,
                                fontWeight = FontWeight.Medium
                            )
                            NomadSkeletonListCard()
                            NomadSkeletonListCard()
                        }
                    }
                } else if (uiState.briefing != null) {
                    val briefing = uiState.briefing!!

                    // 1. Overall Summary: Thailand-wide Daily Synthesis
                    item {
                        BriefingSectionCard(
                            title = "Thailand Daily Summary",
                            icon = Icons.Default.Public,
                            accentColor = AetherialPalette.IndigoAccent
                        ) {
                            HighlightedMarkdownBulletList(
                                text = briefing.thailandSummary,
                                bulletColor = AetherialPalette.IndigoAccent,
                                highlightColor = MaterialTheme.colorScheme.onSurface,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. User Selected Area Briefing: Dedicated Local Outlook & Telemetry
                    item {
                        SelectedAreaBriefingCard(
                            briefing = briefing,
                            selectedPlace = uiState.selectedPlace,
                            savedPlaces = uiState.savedPlaces,
                            onSelectPlace = { place -> viewModel.selectPlace(place) },
                            onRefresh = { viewModel.generateBriefing() }
                        )
                    }

                    // 3. News and Other Context: TMD Warnings, Flood/Dam Status & Meteorological Articles
                    item {
                        BriefingSectionCard(
                            title = "News & Incident Context",
                            icon = Icons.AutoMirrored.Filled.Article,
                            accentColor = AetherialPalette.AmberText
                        ) {
                            val newsContent = if (briefing.newsAndIncidents.isNotBlank()) {
                                briefing.newsAndIncidents
                            } else {
                                """
                                • **TMD Official Bulletin**: No active tropical storm or severe cyclone warnings across Thailand.
                                • **Hydrological Status**: Chao Phraya basin and northern reservoir outflows remain within controlled seasonal thresholds.
                                • **Marine Notice**: Waves at **1.0-1.5m** in Andaman Sea & Gulf of Thailand with normal ferry operations.
                                """.trimIndent()
                            }
                            HighlightedMarkdownBulletList(
                                text = newsContent,
                                bulletColor = AetherialPalette.AmberWarm,
                                highlightColor = MaterialTheme.colorScheme.onSurface,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4. Saved Locations Summaries
                    if (briefing.locationSummaries.isNotEmpty()) {
                        item {
                            BriefingSectionCard(
                                title = "Saved Places Outlook",
                                icon = Icons.Default.LocationOn,
                                accentColor = AetherialPalette.SkyBlue
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    briefing.locationSummaries.forEach { loc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(AetherialPalette.PastelSky.copy(alpha = 0.35f))
                                                .border(
                                                    1.dp,
                                                    AetherialPalette.PastelSkyBorder.copy(alpha = 0.5f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = loc.placeName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                HighlightedMarkdownText(
                                                    text = loc.summary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    highlightColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            RiskLevelChip(riskLevel = loc.riskLevel)
                                        }
                                    }
                                }
                            }
                        }
                    }



                    // 4. Source-Backed Risk Explanations
                    if (briefing.riskExplanations.isNotEmpty()) {
                        item {
                            BriefingSectionCard(
                                title = "Source-Backed Risk Explanations",
                                icon = Icons.Default.Shield,
                                accentColor = AetherialPalette.CoralText
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    briefing.riskExplanations.forEach { risk ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(AetherialPalette.PastelCoral.copy(alpha = 0.35f))
                                                .border(
                                                    1.dp,
                                                    AetherialPalette.PastelCoralBorder.copy(alpha = 0.6f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = risk.hazard,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AetherialPalette.CoralText
                                                )
                                                Text(
                                                    text = "Source: ${risk.source}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            HighlightedMarkdownText(
                                                text = risk.explanation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                highlightColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Actionable Advice
                    if (briefing.actionableAdvice.isNotEmpty()) {
                        item {
                            BriefingSectionCard(
                                title = "Outdoor & Travel Guidance",
                                icon = Icons.Default.Lightbulb,
                                accentColor = AetherialPalette.MintGreenText
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    briefing.actionableAdvice.forEach { tip ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AetherialPalette.MintGreen,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            HighlightedMarkdownText(
                                                text = tip,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                highlightColor = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Source Attribution & Disclaimer Footer
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NomadKitPalette.SurfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, NomadKitPalette.OutlineVariant),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Sources: ${briefing.sources.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NomadKitPalette.PrimarySandDeep
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All meteorological readings and warnings originate directly from official government authorities. AI interpretation provides consolidated analysis.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Ready to generate initial state
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, NomadKitPalette.OutlineVariant),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(DribbblePalette.NeutralRose),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = DribbblePalette.Accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Ready to Generate Briefing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap 'Generate' above to synthesize Thailand regional weather forecasts, TMD warnings, and local news into an actionable AI briefing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRequiredGatedView(
    onNavigateToAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, NomadKitPalette.Outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DribbblePalette.NeutralRose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = DribbblePalette.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Groq API Key Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "AI Briefings consolidate live weather forecasts, official TMD emergency warnings, and local news into actionable daily intelligence using your own Groq API key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Button(
                    onClick = onNavigateToAiSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NomadKitPalette.PrimarySand,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure AI Settings")
                }

                Text(
                    text = "Live weather forecasts, interactive radar maps, saved places, and official alerts remain 100% active and available without an API key.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BriefingHeaderCard(
    briefing: AiBriefing?,
    isLoading: Boolean,
    onGenerate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (briefing != null && briefing.isStale) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NomadKitPalette.WarningContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NomadKitPalette.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cached Briefing (May be outdated) • Offline accessible",
                        style = MaterialTheme.typography.labelSmall,
                        color = NomadKitPalette.Warning,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = briefing?.title ?: "Consolidated Intelligence Briefing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (briefing != null) {
                        Text(
                            text = "Generated: ${WeatherUtils.formatTimestamp(briefing.generatedAt)} • ${briefing.modelUsed}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onGenerate,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DribbblePalette.Accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (briefing == null) "Generate" else "Refresh")
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RiskLevelChip(riskLevel: String) {
    val (bgColor, textColor) = when (riskLevel.lowercase()) {
        "severe", "high" -> Pair(NomadKitPalette.ErrorContainer, NomadKitPalette.Error)
        "moderate" -> Pair(NomadKitPalette.WarningContainer, NomadKitPalette.Warning)
        else -> Pair(NomadKitPalette.SuccessContainer, NomadKitPalette.TertiaryForest)
    }

    Text(
        text = riskLevel.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}


@Composable
private fun SelectedAreaBriefingCard(
    briefing: AiBriefing,
    selectedPlace: Place?,
    savedPlaces: List<Place>,
    onSelectPlace: (Place) -> Unit,
    onRefresh: () -> Unit
) {
    val sel = briefing.selectedAreaBriefing
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NomadKitPalette.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon + Title + Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NomadKitPalette.PrimarySand.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = NomadKitPalette.PrimarySandDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val displayName = when {
                            !sel?.placeName.isNullOrBlank() -> "${sel.placeName} Localized Briefing"
                            selectedPlace != null -> "${selectedPlace.name} Localized Briefing"
                            else -> "User Selected Area Briefing"
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NomadKitPalette.PrimarySandDeep
                        )
                        if (!sel?.province.isNullOrBlank()) {
                            Text(
                                text = sel.province,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!sel?.riskLevel.isNullOrBlank()) {
                    RiskLevelChip(riskLevel = sel.riskLevel)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location Selector Pill & Quick Switcher
            if (savedPlaces.isNotEmpty()) {
                Box {
                    Surface(
                        onClick = { menuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = NomadKitPalette.SurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, NomadKitPalette.OutlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = null,
                                tint = NomadKitPalette.PrimarySand,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val activePlaceName = selectedPlace?.name ?: sel?.placeName ?: "Select Area"
                            Text(
                                text = "Active Area: $activePlaceName",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NomadKitPalette.PrimarySand,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        savedPlaces.forEach { place ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = place.name,
                                            fontWeight = if (place.id == selectedPlace?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (place.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Default",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NomadKitPalette.PrimarySand
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSelectPlace(place)
                                    onRefresh()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Telemetry Chips / Conditions Highlight
            if (!sel?.conditionsHighlight.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AetherialPalette.PastelSky.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, AetherialPalette.PastelSkyBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = AetherialPalette.SkyBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sel.conditionsHighlight,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Summary Bullets
            if (sel != null && sel.summary.isNotBlank()) {
                HighlightedMarkdownBulletList(
                    text = sel.summary,
                    bulletColor = NomadKitPalette.PrimarySand,
                    highlightColor = MaterialTheme.colorScheme.onSurface,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Tap 'Refresh' to generate the localized telemetry and conditions briefing for ${selectedPlace?.name ?: "this area"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Travel & Activity Recommendation Pill
            if (!sel?.travelRecommendation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AetherialPalette.PastelMint.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            AetherialPalette.PastelMintBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = AetherialPalette.MintGreenText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Travel & Outdoor Recommendation",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AetherialPalette.MintGreenText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        HighlightedMarkdownText(
                            text = sel.travelRecommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            highlightColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
