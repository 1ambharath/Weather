package com.example.thailandweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Aetherial Pastels Design System Color Palette
 *
 * Material Design 3 reimagined through an ethereal, atmospheric lens
 * tailored specifically for environmental and weather tracking.
 * Serene, reassuring, and delicately luminous with soft atmospheric washes
 * and deep slate navy typographic anchors.
 */
object AetherialPalette {
    // Core Surfaces & Canvas (Light) - Fresh Atmospheric Sky-Wash Canvas
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDim = Color(0xFFF1F5F9)
    val SurfaceBright = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF8FAFC)
    val SurfaceContainer = Color(0xFFFFFFFF)
    val SurfaceContainerHigh = Color(0xFFF1F5F9)
    val SurfaceContainerHighest = Color(0xFFE2E8F0)
    val SurfaceVariant = Color(0xFFEDF2F7)
    val Background = Color(0xFFF3F6FD) // Luminous atmospheric sky-wash canvas from reference

    // Primary & Ink Typography Anchors
    val Primary = Color(0xFF4F46E5) // Vibrant Royal Indigo Brand Accent
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEEF2FF)
    val OnPrimaryContainer = Color(0xFF312E81)
    val InversePrimary = Color(0xFFA5B4FC)

    // Secondary & Accent
    val Secondary = Color(0xFF0284C7) // Sky Ocean Blue Accent
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE0F2FE)
    val OnSecondaryContainer = Color(0xFF0369A1)

    // Tertiary
    val Tertiary = Color(0xFF16A34A) // Refreshing Emerald Mint
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFDCFCE7)
    val OnTertiaryContainer = Color(0xFF15803D)

    // Functional Ink & Typography
    val DeepSlateNavy = Color(0xFF0F172A) // WCAG AAA anchor for headlines, 31° temp, and key metrics
    val SubduedSlate = Color(0xFF334155)  // Secondary labels & body
    val FaintNavy = Color(0xFF64748B)     // Inactive / track / timestamp indicators

    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF475569)
    val OnBackground = Color(0xFF0F172A)

    // Outlines & Borders
    val Outline = Color(0xFFE2E8F0)
    val OutlineVariant = Color(0xFFCBD5E1)
    val SurfaceTint = Color(0xFF4F46E5)

    // Atmospheric Environmental Tones (Rich Pastel Sets based on Reference Design)
    // 1. Sky Blue Pastel (Humidity, clear conditions)
    val PastelSky = Color(0xFFE0F2FE)
    val PastelSkyBorder = Color(0xFFBAE6FD)
    val SkyBlue = Color(0xFF0284C7)
    val SkyBlueText = Color(0xFF0369A1)
    val SkyBlueDeep = Color(0xFF0C4A6E)

    // 2. Mint Green Pastel (Wind, AQI, Good Air, Live Status)
    val PastelMint = Color(0xFFDCFCE7)
    val PastelMintBorder = Color(0xFFBBF7D0)
    val MintGreen = Color(0xFF16A34A)
    val MintGreenText = Color(0xFF15803D)
    val MintGreenDeep = Color(0xFF065F46)

    // 3. Warm Amber/Sun Pastel (UV Index, Solar, Sunset, High Heat)
    val PastelAmber = Color(0xFFFEF3C7)
    val PastelAmberBorder = Color(0xFFFDE68A)
    val Amber = Color(0xFFD97706)
    val AmberWarm = Color(0xFFF59E0B)
    val AmberText = Color(0xFFB45309)
    val AmberDeep = Color(0xFF78350F)

    // 4. Lavender Violet Pastel (Pressure, AI Briefing, Digest)
    val PastelPurple = Color(0xFFEDE9FE)
    val PastelPurpleBorder = Color(0xFFDDD6FE)
    val Purple = Color(0xFF8B5CF6)
    val PurpleAccent = Color(0xFF7C3AED)
    val PurpleText = Color(0xFF6D28D9)
    val PurpleDeep = Color(0xFF581C87)

    // 5. Coral Red Pastel (TMD Warnings, Storms, Severe weather)
    val PastelCoral = Color(0xFFFEE2E2)
    val PastelCoralBorder = Color(0xFFFECDD3)
    val Coral = Color(0xFFEF4444)
    val CoralText = Color(0xFFDC2626)
    val CoralDeep = Color(0xFF991B1B)

    // 6. Royal Indigo Pastel (Active tabs, NOW indicator)
    val PastelIndigo = Color(0xFFEEF2FF)
    val PastelIndigoBorder = Color(0xFFC7D2FE)
    val Indigo = Color(0xFF6366F1)
    val IndigoAccent = Color(0xFF4F46E5)
    val IndigoText = Color(0xFF4338CA)

    // Backwards-compatible atmospheric washes
    val SkyLavender = PastelIndigo
    val PowderBlue = PastelSky
    val BlushRose = PastelCoral
    val ButteryPeach = PastelAmber
    val LavenderAction = Color(0xFFE0E7FF)

    // Fixed & Tonal Variants
    val PrimaryFixed = Color(0xFFEEF2FF)
    val PrimaryFixedDim = Color(0xFFC7D2FE)
    val OnPrimaryFixed = Color(0xFF312E81)
    val OnPrimaryFixedVariant = Color(0xFF4338CA)

    val SecondaryFixed = Color(0xFFE0F2FE)
    val SecondaryFixedDim = Color(0xFFBAE6FD)
    val OnSecondaryFixed = Color(0xFF0C4A6E)
    val OnSecondaryFixedVariant = Color(0xFF0369A1)

    val TertiaryFixed = Color(0xFFDCFCE7)
    val TertiaryFixedDim = Color(0xFFBBF7D0)
    val OnTertiaryFixed = Color(0xFF065F46)
    val OnTertiaryFixedVariant = Color(0xFF15803D)

    // Semantics & Status
    val Error = Color(0xFFEF4444)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFEE2E2)
    val OnErrorContainer = Color(0xFF991B1B)

    val Success = Color(0xFF16A34A)
    val SuccessContainer = Color(0xFFDCFCE7)

    val Warning = Color(0xFFF59E0B)
    val WarningContainer = Color(0xFFFEF3C7)

    val Info = Color(0xFF0284C7)
    val InfoContainer = Color(0xFFE0F2FE)

    // Dark Mode Tokens (Ethereal Deep Atmospheric Slate)
    val DarkBackground = Color(0xFF0F172A)
    val DarkSurface = Color(0xFF1E293B)
    val DarkSurfaceRaised = Color(0xFF334155)
    val DarkSurfaceContainer = Color(0xFF1E293B)
    val DarkText = Color(0xFFF8FAFC)
    val DarkTextMuted = Color(0xFF94A3B8)
    val DarkBorder = Color(0xFF334155)
    val DarkAccent = Color(0xFF818CF8)
}

/**
 * Backward compatibility alias mapping DribbblePalette to Aetherial Pastels tokens.
 * All existing screens referencing DribbblePalette inherit Aetherial Pastels aesthetics immediately.
 */
object DribbblePalette {
    // Primary & Ink
    val Primary = AetherialPalette.DeepSlateNavy
    val OnPrimary = Color(0xFFFFFFFF)
    val Ink = AetherialPalette.DeepSlateNavy
    val Link = AetherialPalette.IndigoAccent

    // Accent mapped to vibrant Indigo
    val Accent = AetherialPalette.IndigoAccent
    val OnAccent = Color(0xFFFFFFFF)

    // Surfaces & Canvas (Light)
    val Canvas = AetherialPalette.Background
    val SurfaceMuted = AetherialPalette.SurfaceContainerLow
    val SurfaceRaised = AetherialPalette.Surface
    val Hairline = AetherialPalette.Outline

    // Typography & Content
    val TextPrimary = AetherialPalette.DeepSlateNavy
    val TextMuted = AetherialPalette.FaintNavy

    // Decorative Accent Scale (Vibrant Pastel Sets)
    val Accent1 = AetherialPalette.PastelIndigo
    val Accent2 = AetherialPalette.PastelSky
    val Accent3 = AetherialPalette.PastelMint
    val Accent4 = AetherialPalette.PastelCoral
    val Accent5 = AetherialPalette.PastelAmber

    // Neutrals
    val NeutralDark = AetherialPalette.DeepSlateNavy
    val NeutralRose = AetherialPalette.PastelIndigo

    // Semantics
    val Success = AetherialPalette.Success
    val SuccessContainer = AetherialPalette.SuccessContainer
    val Warning = AetherialPalette.Warning
    val WarningContainer = AetherialPalette.WarningContainer
    val Error = AetherialPalette.Error
    val ErrorContainer = AetherialPalette.ErrorContainer
    val Info = AetherialPalette.Info
    val InfoContainer = AetherialPalette.InfoContainer

    // Dark Mode Palette
    val DarkBg = AetherialPalette.DarkBackground
    val DarkSurface = AetherialPalette.DarkSurface
    val DarkSurfaceRaised = AetherialPalette.DarkSurfaceRaised
    val DarkText = AetherialPalette.DarkText
    val DarkTextMuted = AetherialPalette.DarkTextMuted
    val DarkBorder = AetherialPalette.DarkBorder
    val DarkAccent = AetherialPalette.DarkAccent
}

/**
 * Backward compatibility alias mapping NomadKitPalette to Aetherial Pastels tokens.
 */
object NomadKitPalette {
    // Core Brand Colors
    val PrimarySand: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val SecondaryOcean: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val TertiaryForest: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary

    // Containers & Supporting
    val PrimarySandLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground) AetherialPalette.DarkSurfaceRaised else AetherialPalette.SkyLavender

    val PrimarySandDark: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val PrimarySandDeep: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val SecondaryOceanLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PowderBlue

    val SecondaryOceanDark: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val TertiaryForestLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelMint

    val TertiaryForestDark: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    // Canvas & Cards
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background

    val SurfaceDefault: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface

    val SurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant

    val Outline: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val OutlineVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val OnBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onBackground

    val OnSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val OnSurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    // Semantics
    val Success = AetherialPalette.Success
    val Warning = AetherialPalette.Warning
    val Error = AetherialPalette.Error
    val Info = AetherialPalette.Info

    val SuccessContainer = AetherialPalette.SuccessContainer
    val WarningContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground) Color(0xFF352B24) else AetherialPalette.WarningContainer

    val ErrorContainer = AetherialPalette.ErrorContainer
    val InfoContainer = AetherialPalette.InfoContainer

    // Dark Theme Tones
    val DarkBackground = AetherialPalette.DarkBackground
    val DarkSurface = AetherialPalette.DarkSurface
    val DarkSurfaceVariant = AetherialPalette.DarkSurfaceRaised
    val DarkOnBackground = AetherialPalette.DarkText
    val DarkOnSurface = AetherialPalette.DarkText
    val DarkOnSurfaceVariant = AetherialPalette.DarkTextMuted
    val DarkOutline = AetherialPalette.DarkBorder
    val DarkOutlineVariant = AetherialPalette.DarkBorder

    val DarkPrimarySand = AetherialPalette.DarkText
    val DarkPrimaryContainer = AetherialPalette.DarkSurfaceRaised
    val DarkSecondaryOcean = AetherialPalette.DarkAccent
    val DarkSecondaryContainer = AetherialPalette.DarkSurface
    val DarkTertiaryForest = AetherialPalette.DarkTextMuted
    val DarkTertiaryContainer = AetherialPalette.DarkSurface
}

// Light Material3 Scheme
val primaryLight = AetherialPalette.Primary
val onPrimaryLight = AetherialPalette.OnPrimary
val primaryContainerLight = AetherialPalette.PrimaryContainer
val onPrimaryContainerLight = AetherialPalette.OnPrimaryContainer

val secondaryLight = AetherialPalette.Secondary
val onSecondaryLight = AetherialPalette.OnSecondary
val secondaryContainerLight = AetherialPalette.SecondaryContainer
val onSecondaryContainerLight = AetherialPalette.OnSecondaryContainer

val tertiaryLight = AetherialPalette.Tertiary
val onTertiaryLight = AetherialPalette.OnTertiary
val tertiaryContainerLight = AetherialPalette.TertiaryContainer
val onTertiaryContainerLight = AetherialPalette.OnTertiaryContainer

val backgroundLight = AetherialPalette.Background
val onBackgroundLight = AetherialPalette.OnBackground
val surfaceLight = AetherialPalette.Surface
val onSurfaceLight = AetherialPalette.OnSurface
val surfaceVariantLight = AetherialPalette.SurfaceVariant
val onSurfaceVariantLight = AetherialPalette.OnSurfaceVariant
val outlineLight = AetherialPalette.Outline
val outlineVariantLight = AetherialPalette.OutlineVariant

val errorLight = AetherialPalette.Error
val onErrorLight = AetherialPalette.OnError
val errorContainerLight = AetherialPalette.ErrorContainer
val onErrorContainerLight = AetherialPalette.OnErrorContainer

// Dark Material3 Scheme
val primaryDark = AetherialPalette.InversePrimary
val onPrimaryDark = AetherialPalette.OnPrimaryFixed
val primaryContainerDark = AetherialPalette.SurfaceTint
val onPrimaryContainerDark = AetherialPalette.PrimaryFixed

val secondaryDark = AetherialPalette.SecondaryFixedDim
val onSecondaryDark = AetherialPalette.OnSecondaryFixed
val secondaryContainerDark = AetherialPalette.OnSecondaryFixedVariant
val onSecondaryContainerDark = AetherialPalette.SecondaryFixed

val tertiaryDark = AetherialPalette.TertiaryFixedDim
val onTertiaryDark = AetherialPalette.OnTertiaryFixed
val tertiaryContainerDark = AetherialPalette.OnTertiaryFixedVariant
val onTertiaryContainerDark = AetherialPalette.TertiaryFixed

val backgroundDark = AetherialPalette.DarkBackground
val onBackgroundDark = AetherialPalette.DarkText
val surfaceDark = AetherialPalette.DarkSurface
val onSurfaceDark = AetherialPalette.DarkText
val surfaceVariantDark = AetherialPalette.DarkSurfaceRaised
val onSurfaceVariantDark = AetherialPalette.DarkTextMuted
val outlineDark = AetherialPalette.DarkBorder
val outlineVariantDark = AetherialPalette.DarkBorder

val errorDark = Color(0xFFF87171)
val onErrorDark = Color(0xFF450A0A)
val errorContainerDark = Color(0xFF991B1B)
val onErrorContainerDark = Color(0xFFFEE2E2)
