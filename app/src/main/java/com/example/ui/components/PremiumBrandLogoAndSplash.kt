package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SoundAndHapticFeedback
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val LogoEmerald = Color(0xFF00E676)
private val LogoGold = Color(0xFFFFD54F)
private val LogoCyan = Color(0xFF38BDF8)
private val LogoDeepNavy = Color(0xFF060C1A)
private val LogoObsidian = Color(0xFF040812)

/**
 * Custom-drawn Institutional Bullish Shield + Ascending Candlestick & Arrow Crest Logo.
 * Used in TopAppBar, Welcome Hero, QR Hub, and the Startup Splash Animation.
 */
@Composable
fun TradingZonePremiumLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PremiumLogoTransition")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoOrbitAngle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoGlowPulse"
    )

    val effectiveAngle = if (animated) orbitAngle else 35f
    val effectiveGlow = if (animated) glowPulse else 0.9f

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) / 2f

        // 1. Soft outer radial emerald/gold aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    LogoEmerald.copy(alpha = 0.28f * effectiveGlow),
                    LogoGold.copy(alpha = 0.12f * effectiveGlow),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius * 1.05f
            ),
            radius = radius * 1.05f,
            center = Offset(cx, cy)
        )

        // 2. Hexagonal Shield Base
        val hexPath = Path().apply {
            for (i in 0 until 6) {
                val angleRad = Math.toRadians((60.0 * i - 90.0))
                val px = cx + (radius * 0.88f) * cos(angleRad).toFloat()
                val py = cy + (radius * 0.88f) * sin(angleRad).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }

        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0E1B33), LogoDeepNavy, Color(0xFF081426)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // 3. Rotating Gold/Emerald Orbital Accent Arc
        rotate(degrees = effectiveAngle, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        LogoGold.copy(alpha = 0.85f),
                        LogoEmerald,
                        LogoCyan.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(cx - radius * 0.92f, cy - radius * 0.92f),
                size = Size(radius * 1.84f, radius * 1.84f),
                style = Stroke(width = (w * 0.045f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
            )
        }

        // 4. Crisp Metallic Gold & Emerald Hexagon Border
        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                colors = listOf(LogoGold, LogoEmerald, LogoGold),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            style = Stroke(
                width = (w * 0.055f).coerceAtLeast(2f),
                join = StrokeJoin.Round
            )
        )

        // 5. Three Institutional Ascending Candlesticks inside the Crest
        val barWidth = w * 0.095f
        val barCorner = CornerRadius(barWidth / 2f, barWidth / 2f)

        // Left candle (Gold)
        val c1X = cx - radius * 0.36f
        drawLine(
            color = LogoGold.copy(alpha = 0.9f),
            start = Offset(c1X, cy + radius * 0.34f),
            end = Offset(c1X, cy - radius * 0.08f),
            strokeWidth = (w * 0.03f).coerceAtLeast(1.2f),
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = LogoGold,
            topLeft = Offset(c1X - barWidth / 2f, cy + radius * 0.02f),
            size = Size(barWidth, radius * 0.24f),
            cornerRadius = barCorner
        )

        // Center candle (Cyan/Emerald)
        val c2X = cx - radius * 0.03f
        drawLine(
            color = LogoEmerald,
            start = Offset(c2X, cy + radius * 0.26f),
            end = Offset(c2X, cy - radius * 0.28f),
            strokeWidth = (w * 0.03f).coerceAtLeast(1.2f),
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = LogoEmerald,
            topLeft = Offset(c2X - barWidth / 2f, cy - radius * 0.16f),
            size = Size(barWidth, radius * 0.32f),
            cornerRadius = barCorner
        )

        // Right tall breakout candle (Neon Emerald)
        val c3X = cx + radius * 0.30f
        drawLine(
            color = LogoGold,
            start = Offset(c3X, cy + radius * 0.18f),
            end = Offset(c3X, cy - radius * 0.48f),
            strokeWidth = (w * 0.032f).coerceAtLeast(1.2f),
            cap = StrokeCap.Round
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(LogoGold, LogoEmerald)
            ),
            topLeft = Offset(c3X - barWidth / 2f, cy - radius * 0.36f),
            size = Size(barWidth, radius * 0.44f),
            cornerRadius = barCorner
        )

        // 6. Dynamic Bullish Breakout Trendline Arrow sweeping upward across the shield
        val arrowPath = Path().apply {
            moveTo(cx - radius * 0.46f, cy + radius * 0.30f)
            lineTo(cx - radius * 0.10f, cy - radius * 0.02f)
            lineTo(cx + radius * 0.08f, cy + radius * 0.10f)
            lineTo(cx + radius * 0.44f, cy - radius * 0.38f)
        }
        drawPath(
            path = arrowPath,
            brush = Brush.horizontalGradient(
                colors = listOf(LogoGold, Color.White, LogoEmerald)
            ),
            style = Stroke(
                width = (w * 0.068f).coerceAtLeast(2.2f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Arrowhead at top-right
        val headPath = Path().apply {
            moveTo(cx + radius * 0.22f, cy - radius * 0.38f)
            lineTo(cx + radius * 0.45f, cy - radius * 0.40f)
            lineTo(cx + radius * 0.43f, cy - radius * 0.16f)
        }
        drawPath(
            path = headPath,
            color = LogoGold,
            style = Stroke(
                width = (w * 0.068f).coerceAtLeast(2.2f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Full-screen Ultrasound Startup Splash Animation with Premium Crest Logo,
 * sonar pulse rings, and 1-tap instant skip.
 */
@Composable
fun TradingZoneStartupSplashOverlay(
    visible: Boolean,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val scaleAnim = remember { Animatable(0.68f) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            SoundAndHapticFeedback.playSuccess(context)
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
            )
            delay(150)
            onSplashFinished()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(360)) + scaleOut(
            targetScale = 1.08f,
            animationSpec = tween(360, easing = FastOutSlowInEasing)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0E1B34),
                            LogoDeepNavy,
                            LogoObsidian
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onSplashFinished()
                }
                .testTag("startup_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(28.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LogoGold.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LogoGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "👑 OFFICIAL INSTITUTIONAL EDITION",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = LogoGold,
                        letterSpacing = 1.1.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                TradingZonePremiumLogo(
                    size = 128.dp,
                    animated = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "TRADING ZONE",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "LIVE ADVISORY • MCX COMMODITY • TRADE SIGNAL P&L",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LogoEmerald,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sleek animated progress bar
                Box(
                    modifier = Modifier
                        .width(210.dp)
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim.value.coerceIn(0.08f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(LogoGold, LogoEmerald, LogoCyan)
                                ),
                                RoundedCornerShape(50)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "🛡️ 2-Step Security & Multi-Device Guard Active • Tap anywhere to enter",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
