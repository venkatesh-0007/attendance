package com.example.attendance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

object WidgetThemeHelper {

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
        accentHex: String? = null,
        content: @Composable () -> Unit
    ) {
        // Strict Monochrome Theme (White / Black / Grays) with Red reserved ONLY for Absents
        val lightColors = lightColorScheme(
            primary = Color(0xFF09090B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE4E4E7),
            onPrimaryContainer = Color(0xFF09090B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF09090B),
            surfaceVariant = Color(0xFFF4F4F5),
            onSurfaceVariant = Color(0xFF71717A),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF09090B),
            outline = Color(0xFFE4E4E7),
            error = Color(0xFFDC2626),
            errorContainer = Color(0xFFFEF2F2),
            onErrorContainer = Color(0xFF991B1B)
        )

        val darkColors = darkColorScheme(
            primary = Color(0xFFFAFAFA),
            onPrimary = Color(0xFF09090B),
            primaryContainer = Color(0xFF27272A),
            onPrimaryContainer = Color(0xFFFAFAFA),
            surface = Color(0xFF09090B),
            onSurface = Color(0xFFFAFAFA),
            surfaceVariant = Color(0xFF18181B),
            onSurfaceVariant = Color(0xFFA1A1AA),
            background = Color(0xFF09090B),
            onBackground = Color(0xFFFAFAFA),
            outline = Color(0xFF27272A),
            error = Color(0xFFEF4444),
            errorContainer = Color(0xFF450A0A),
            onErrorContainer = Color(0xFFFCA5A5)
        )

        GlanceTheme(
            colors = ColorProviders(light = lightColors, dark = darkColors)
        ) {
            content()
        }
    }
}
