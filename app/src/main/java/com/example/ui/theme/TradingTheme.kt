package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemePreference

private val ObsidianTerminalColors = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF032218),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF060B16),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0E172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF172540),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA),
    outline = Color(0xFF2D3E5E)
)

private val CyberEmeraldColors = darkColorScheme(
    primary = Color(0xFF00F5A0),
    onPrimary = Color(0xFF022014),
    primaryContainer = Color(0xFF043B29),
    onPrimaryContainer = Color(0xFF9FFFE0),
    secondary = Color(0xFF00D9F5),
    onSecondary = Color(0xFF01242B),
    secondaryContainer = Color(0xFF063C47),
    onSecondaryContainer = Color(0xFFB5F5FF),
    tertiary = Color(0xFFFACC15),
    onTertiary = Color(0xFF302402),
    background = Color(0xFF040D0A),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF091914),
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF112921),
    onSurfaceVariant = Color(0xFF86EFAC),
    error = Color(0xFFFF4D6D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF4A0E1C),
    onErrorContainer = Color(0xFFFFCCD5),
    outline = Color(0xFF1E4638)
)

private val RoyalGoldColors = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF271701),
    primaryContainer = Color(0xFF4D3004),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF032218),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFEAB308),
    onTertiary = Color(0xFF2E2002),
    background = Color(0xFF0C0A07),
    onBackground = Color(0xFFFEF3C7),
    surface = Color(0xFF17130D),
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = Color(0xFF261F14),
    onSurfaceVariant = Color(0xFFD6C5A3),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA),
    outline = Color(0xFF473A24)
)

private val MidnightSapphireColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF032333),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF13163D),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF10B981),
    onTertiary = Color(0xFF032218),
    background = Color(0xFF050A18),
    onBackground = Color(0xFFF0F9FF),
    surface = Color(0xFF0B1530),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF16244A),
    onSurfaceVariant = Color(0xFF93C5FD),
    error = Color(0xFFF43F5E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFFE4E6),
    outline = Color(0xFF293E73)
)

private val LightTradingColors = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFCBD5E1)
)

private val TradingTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val spec = spring<Color>(stiffness = Spring.StiffnessMediumLow)
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer")
    val background by animateColorAsState(target.background, spec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "outline")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = secondaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline
    )
}

@Composable
fun TradingTheme(
    themePreference: AppThemePreference = AppThemePreference.DARK,
    content: @Composable () -> Unit
) {
    val targetScheme = when (themePreference) {
        AppThemePreference.DARK -> ObsidianTerminalColors
        AppThemePreference.CYBER_EMERALD -> CyberEmeraldColors
        AppThemePreference.ROYAL_GOLD -> RoyalGoldColors
        AppThemePreference.MIDNIGHT_SAPPHIRE -> MidnightSapphireColors
        AppThemePreference.LIGHT -> LightTradingColors
        AppThemePreference.SYSTEM -> if (isSystemInDarkTheme()) ObsidianTerminalColors else LightTradingColors
    }
    MaterialTheme(
        colorScheme = animateScheme(targetScheme),
        typography = TradingTypography,
        content = content
    )
}
