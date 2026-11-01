package com.example.thailandweather.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Aetherial Pastels Design System Shape Vocabulary:
 *
 * Employs pill-shaped, ultra-smooth squircle geometry (roundedness level 3):
 * - Cards & Primary Modules: Large squircle radii (rounded-xl, 24dp - 48dp)
 *   creating organic, cloud-like boundaries.
 * - Metric Badges & Pills: Full pill rounding (rounded-full, 9999px / 50%)
 *   offering a tactile pebble-like appearance for metrics, chips, and tags.
 * - Action Buttons & Chips: Pill geometries (rounded-full) to maintain an
 *   approachable, non-mechanical visual tone across all touchpoints.
 */
val AetherialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),  // sm: 0.5rem (8dp)
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),     // DEFAULT: 1rem (16dp)
    large = RoundedCornerShape(24.dp),      // md: 1.5rem (24dp)
    extraLarge = RoundedCornerShape(32.dp)  // lg: 2rem (32dp) / xl: 3rem (48dp)
)

object AetherialRadius {
    val Sm = RoundedCornerShape(8.dp)
    val Default = RoundedCornerShape(16.dp)
    val Md = RoundedCornerShape(24.dp)
    val Lg = RoundedCornerShape(32.dp)
    val Xl = RoundedCornerShape(48.dp)
    val Pill = RoundedCornerShape(percent = 50)
}
