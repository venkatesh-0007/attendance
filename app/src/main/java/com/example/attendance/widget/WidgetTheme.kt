package com.example.attendance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

object WidgetThemeHelper {

    fun getAccentColor(hex: String?): Color {
        return hex?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                null
            }
        } ?: Color(0xFF4F46E5) // Default Indigo
    }

    fun isDarkTheme(context: Context, darkModeSetting: String?): Boolean {
        return when (darkModeSetting) {
            "DARK" -> true
            "LIGHT" -> false
            else -> {
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    @Composable
    fun AttendanceWidgetTheme(
        context: Context,
        darkModeSetting: String?,
        accentHex: String?,
        content: @Composable () -> Unit
    ) {
        val accent = getAccentColor(accentHex)
        val isDark = isDarkTheme(context, darkModeSetting)

        val lightColors = lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.15f),
            onPrimaryContainer = accent,
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF64748B),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF1E293B),
            outline = Color(0xFFCBD5E1)
        )

        val darkColors = darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            surface = Color(0xFF18181B),
            onSurface = Color(0xFFF4F4F5),
            surfaceVariant = Color(0xFF27272A),
            onSurfaceVariant = Color(0xFFA1A1AA),
            background = Color(0xFF09090B),
            onBackground = Color(0xFFF4F4F5),
            outline = Color(0xFF3F3F46)
        )

        GlanceTheme(
            colors = ColorProviders(light = lightColors, dark = darkColors)
        ) {
            content()
        }
    }
}
