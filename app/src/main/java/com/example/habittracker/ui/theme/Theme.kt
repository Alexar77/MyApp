package com.example.habittracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color(0xFFF5F7FB),
    primaryContainer = Color(0xFF1B2E52),
    onPrimaryContainer = Color(0xFFF5F7FB),
    secondary = Color(0xFFFF6B5E),
    onSecondary = Color(0xFF08111F),
    secondaryContainer = Color(0xFF4A2524),
    onSecondaryContainer = Color(0xFFFFD8D4),
    tertiary = Color(0xFFF6C453),
    onTertiary = Color(0xFF08111F),
    tertiaryContainer = Color(0xFF473617),
    onTertiaryContainer = Color(0xFFFFE8AE),
    background = Color(0xFF08111F),
    onBackground = Color(0xFFF5F7FB),
    surface = Color(0xFF0D1626),
    onSurface = Color(0xFFF5F7FB),
    surfaceVariant = Color(0xFF111B2E),
    onSurfaceVariant = Color(0xFFA7B3C9),
    surfaceTint = Color(0xFF2F6BFF),
    error = Color(0xFFFF7C7C),
    onError = Color(0xFF180D0D),
    errorContainer = Color(0xFF4C2327),
    onErrorContainer = Color(0xFFFFDAD8),
    outline = Color(0xFF4B5C79),
    outlineVariant = Color(0xFF23314A),
    scrim = Color(0xCC02060E)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Immutable
data class AppSpacing(
    val xs: androidx.compose.ui.unit.Dp = 8.dp,
    val sm: androidx.compose.ui.unit.Dp = 12.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 20.dp,
    val xl: androidx.compose.ui.unit.Dp = 24.dp,
    val xxl: androidx.compose.ui.unit.Dp = 32.dp
)

@Immutable
data class AppExtraColors(
    val brandSurface: Color = Color(0xFF1B2E52),
    val accent: Color = Color(0xFFFF6B5E),
    val success: Color = Color(0xFF36D399),
    val warning: Color = Color(0xFFF6C453),
    val infoSurface: Color = Color(0xFF122846)
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalAppExtraColors = staticCompositionLocalOf { AppExtraColors() }

object MyAppTheme {
    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current

    val extraColors: AppExtraColors
        @Composable get() = LocalAppExtraColors.current
}

@Composable
fun MyAppTheme(
    content: @Composable () -> Unit
) {
    MyAppTheme(
        darkTheme = isSystemInDarkTheme(),
        content = content
    )
}

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppDarkColorScheme

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalAppExtraColors provides AppExtraColors()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
