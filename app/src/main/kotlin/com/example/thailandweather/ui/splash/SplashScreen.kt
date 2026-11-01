package com.example.thailandweather.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.R
import com.example.thailandweather.ui.theme.AetherialPalette
import kotlinx.coroutines.delay

/**
 * Aetherial Pastels Splash Screen
 *
 * Implements the serene, atmospheric personality of Aetherial Pastels:
 * - Fluid atmospheric gradient canvas
 * - Ambient chromatic halo glow surrounding the circular logo
 * - Plus Jakarta Sans typographic hierarchy
 * - Prominent signature tagline: "Just Thailand. Made with love ❤️"
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Entrance animations
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
        // Allow user to enjoy the ethereal splash for 2.2 seconds, then transition
        delay(2200)
        onSplashFinished()
    }

    val transition = rememberTransition(transitionState, label = "SplashEntrance")

    val alphaAnim by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, easing = FastOutSlowInEasing) },
        label = "Alpha"
    ) { state -> if (state) 1f else 0f }

    val scaleAnim by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "Scale"
    ) { state -> if (state) 1f else 0.82f }

    // Pulsing halo animation for ambient chromatic diffusion
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientHalo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground

    val backgroundGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF161B2D),
                Color(0xFF1D2237),
                Color(0xFF2B2F43)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                AetherialPalette.Background,
                AetherialPalette.SkyLavender,
                AetherialPalette.PowderBlue.copy(alpha = 0.7f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ambient chromatic halo & app logo
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer diffuse atmospheric glow
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isDark) AetherialPalette.DarkAccent.copy(alpha = haloAlpha * 0.4f)
                                    else AetherialPalette.PowderBlue.copy(alpha = haloAlpha),
                                    if (isDark) AetherialPalette.TertiaryFixedDim.copy(alpha = haloAlpha * 0.2f)
                                    else AetherialPalette.ButteryPeach.copy(alpha = haloAlpha * 0.8f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Inner frosted container with top-lit glass edge
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF22263A) else Color.White.copy(alpha = 0.92f)
                        )
                        .border(
                            BorderStroke(
                                2.dp,
                                if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.85f)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_circle),
                        contentDescription = "Thailand Weather App Logo",
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "Thailand Weather",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Design system edition pill badge
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.SkyLavender,
                border = BorderStroke(
                    1.dp,
                    if (isDark) AetherialPalette.DarkBorder else Color.White.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isDark) AetherialPalette.DarkAccent else AetherialPalette.Secondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aetherial Pastels Edition",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) AetherialPalette.DarkText else AetherialPalette.DeepSlateNavy,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User requested splash text: "Just Thailand. Made with love [heart]"
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDark) Color(0x332B2F43) else Color.White.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x44575D7D) else Color.White.copy(alpha = 0.7f)
                ),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Just Thailand. Made with love ❤️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) AetherialPalette.DarkText else AetherialPalette.DeepSlateNavy,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }

        // Bottom atmospheric status & micro indicators
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Three gentle pulsing atmospheric dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isDark) AetherialPalette.InversePrimary else AetherialPalette.Primary)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isDark) AetherialPalette.SecondaryFixedDim else AetherialPalette.PowderBlue)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isDark) AetherialPalette.TertiaryFixedDim else AetherialPalette.PastelMint)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Open-Meteo • TMD Radar • Air4Thai",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) AetherialPalette.DarkTextMuted else AetherialPalette.SubduedSlate,
                letterSpacing = 0.4.sp
            )
        }
    }
}
