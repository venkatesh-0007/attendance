package com.attendance.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalAccentColor = staticCompositionLocalOf { Color(0xFF3B82F6) }

private val DarkColorScheme = darkColorScheme(
    primary = MacBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0x333B82F6),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFFCBD5E1),
    onTertiary = Color(0xFF0F172A),
    background = SpaceNavyDeep,
    onBackground = Color(0xFFF8FAFC),
    surface = SpaceNavyDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SpaceNavyMid,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = MacCritical,
    onError = Color.White,
    errorContainer = Color(0x33EF4444),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = MacBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0x223B82F6),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun AttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val effectiveAccent = accentColor ?: MacBlue

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val dynamicColorScheme = baseScheme.copy(
        primary = effectiveAccent,
        primaryContainer = effectiveAccent.copy(alpha = if (darkTheme) 0.22f else 0.12f),
        onPrimaryContainer = if (darkTheme) effectiveAccent else effectiveAccent
    )

    CompositionLocalProvider(LocalAccentColor provides effectiveAccent) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content
        )
    }
}
