package com.attendance.app.theme

import androidx.compose.ui.graphics.Color

// Brand Seed: Monochrome Zinc / Obsidian
val SeedColor = Color(0xFF18181B)

// Dark Theme Colors (Monochrome Slate/Zinc)
val PrimaryDark = Color(0xFFFAFAFA)
val OnPrimaryDark = Color(0xFF09090B)
val PrimaryContainerDark = Color(0xFF27272A)
val OnPrimaryContainerDark = Color(0xFFFAFAFA)

val SecondaryDark = Color(0xFFE4E4E7)
val OnSecondaryDark = Color(0xFF18181B)

val TertiaryDark = Color(0xFFA1A1AA)
val OnTertiaryDark = Color(0xFF18181B)

val SurfaceDark = Color(0xFF09090B)
val OnSurfaceDark = Color(0xFFFAFAFA)
val SurfaceVariantDark = Color(0xFF18181B)
val OnSurfaceVariantDark = Color(0xFFA1A1AA)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF450A0A)
val OnErrorContainerDark = Color(0xFFF87171)

// Light Theme Colors (Monochrome Slate/Zinc)
val PrimaryLight = Color(0xFF09090B)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFF4F4F5)
val OnPrimaryContainerLight = Color(0xFF09090B)

val SecondaryLight = Color(0xFF27272A)
val OnSecondaryLight = Color(0xFFFFFFFF)

val TertiaryLight = Color(0xFF71717A)
val OnTertiaryLight = Color(0xFFFFFFFF)

val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF09090B)
val SurfaceVariantLight = Color(0xFFF4F4F5)
val OnSurfaceVariantLight = Color(0xFF71717A)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEF2F2)
val OnErrorContainerLight = Color(0xFF991B1B)

// Mac Liquid Glass & Space Midnight Design System Tokens
val SpaceNavyDeep = Color(0xFF070B14)
val SpaceNavyDark = Color(0xFF0A0F1D)
val SpaceNavyMid = Color(0xFF0F172A)
val SpaceNavySurface = Color(0xFF1E293B)
val SpaceNavyLight = Color(0xFF334155)

val GlassCardBg = Color(0xB80F172A)          // Translucent dark slate
val GlassCardBgSecondary = Color(0x991E293B) // Slightly brighter frosted slate
val GlassCardBorderTop = Color(0x3BFFFFFF)   // Specular top light edge
val GlassCardBorderMid = Color(0x14FFFFFF)   // Soft side light edge
val GlassCardBorderBottom = Color(0x05FFFFFF)// Subtly dark bottom edge
val GlassPillBg = Color(0x1FFFFFFF)          // Ultra-thin frosted pill fill
val GlassPillBorder = Color(0x2EFFFFFF)      // Frosted pill border

// Mac Attendance Palette
val MacSafe = Color(0xFF10B981)
val MacSafeGlow = Color(0x5910B981)
val MacSafeGradientStart = Color(0xFF34D399)
val MacSafeGradientEnd = Color(0xFF059669)

val MacCritical = Color(0xFFEF4444)
val MacCriticalGlow = Color(0x59EF4444)
val MacCriticalGradientStart = Color(0xFFF87171)
val MacCriticalGradientEnd = Color(0xFFDC2626)

val MacBlue = Color(0xFF3B82F6)
val MacBlueGlow = Color(0x403B82F6)
val MacPurple = Color(0xFF8B5CF6)
val MacPurpleGlow = Color(0x408B5CF6)
val MacCyan = Color(0xFF06B6D4)
val MacAmber = Color(0xFFF59E0B)

data class AppAccentColor(
    val id: String,
    val name: String,
    val hex: String,
    val color: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val glowColor: Color
)

val AppAccentPresets = listOf(
    AppAccentColor(
        id = "blue",
        name = "Electric Blue",
        hex = "#3B82F6",
        color = Color(0xFF3B82F6),
        gradientStart = Color(0xFF60A5FA),
        gradientEnd = Color(0xFF2563EB),
        glowColor = Color(0x503B82F6)
    ),
    AppAccentColor(
        id = "emerald",
        name = "Emerald Green",
        hex = "#10B981",
        color = Color(0xFF10B981),
        gradientStart = Color(0xFF34D399),
        gradientEnd = Color(0xFF059669),
        glowColor = Color(0x5010B981)
    ),
    AppAccentColor(
        id = "purple",
        name = "Cyber Purple",
        hex = "#8B5CF6",
        color = Color(0xFF8B5CF6),
        gradientStart = Color(0xFFA78BFA),
        gradientEnd = Color(0xFF7C3AED),
        glowColor = Color(0x508B5CF6)
    ),
    AppAccentColor(
        id = "cyan",
        name = "Neon Cyan",
        hex = "#06B6D4",
        color = Color(0xFF06B6D4),
        gradientStart = Color(0xFF22D3EE),
        gradientEnd = Color(0xFF0891B2),
        glowColor = Color(0x5006B6D4)
    ),
    AppAccentColor(
        id = "amber",
        name = "Sunset Amber",
        hex = "#F59E0B",
        color = Color(0xFFF59E0B),
        gradientStart = Color(0xFFFBBF24),
        gradientEnd = Color(0xFFD97706),
        glowColor = Color(0x50F59E0B)
    ),
    AppAccentColor(
        id = "rose",
        name = "Coral Rose",
        hex = "#F43F5E",
        color = Color(0xFFF43F5E),
        gradientStart = Color(0xFFFB7185),
        gradientEnd = Color(0xFFE11D48),
        glowColor = Color(0x50F43F5E)
    ),
    AppAccentColor(
        id = "monochrome",
        name = "Obsidian Slate",
        hex = "#94A3B8",
        color = Color(0xFF94A3B8),
        gradientStart = Color(0xFFCBD5E1),
        gradientEnd = Color(0xFF64748B),
        glowColor = Color(0x4094A3B8)
    )
)

