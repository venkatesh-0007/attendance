package com.attendance.app.ui.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.data.model.*
import com.attendance.app.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MARK: - Reusable Liquid Glass Container
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = GlassCardBg,
    borderBrush: Brush = Brush.linearGradient(
        listOf(
            GlassCardBorderTop,
            GlassCardBorderMid,
            GlassCardBorderBottom
        )
    ),
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderBrush, shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

// MARK: - 1. PRESENT ATTENDANCE (Liquid Glass Tile)
@Composable
fun PresentAttendanceGlassCard(
    attendance: AttendanceResponse,
    targetThreshold: Double = 75.0,
    modifier: Modifier = Modifier
) {
    val overallPct = attendance.overallPercentage.toFloat()
    val isSafe = overallPct >= targetThreshold

    // Animated count-up percentage (0f -> overallPct)
    val animatedPct by animateFloatAsState(
        targetValue = overallPct,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "MacPercentageAnim"
    )

    val statusColor = if (isSafe) MacSafe else MacCritical
    val statusGlow = if (isSafe) MacSafeGlow else MacCriticalGlow
    val progressGradient = if (isSafe) {
        Brush.horizontalGradient(listOf(MacSafeGradientStart, MacSafeGradientEnd))
    } else {
        Brush.horizontalGradient(listOf(MacCriticalGradientStart, MacCriticalGradientEnd))
    }

    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        borderBrush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.26f),
                statusColor.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        contentPadding = PaddingValues(14.dp)
    ) {
        // Top Row: Glowing Percentage + Monospaced Counts Pill (Numbers only: 412/502)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing animated percentage
            Text(
                text = String.format(Locale.US, "%.1f%%", animatedPct),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = statusColor,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 6f
                }
            )

            // Numbers only: attended / held in frosted capsule (No "Classes held", no "Safe Standing")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${attendance.attendedCount}/${attendance.heldCount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Liquid Progress Track with 75% Target Tick Line
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth

            // Frosted background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            )

            // Liquid animated progress bar
            val progressFraction = (animatedPct / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(progressGradient)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(3.dp), spotColor = statusGlow)
            )

            // Target goal tick mark (e.g. 75%)
            val targetFraction = (targetThreshold / 100.0).coerceIn(0.0, 1.0).toFloat()
            val targetOffset = totalWidth * targetFraction - 1.dp
            Box(
                modifier = Modifier
                    .offset(x = targetOffset)
                    .width(2.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(1.dp), spotColor = Color.White)
            )
        }
    }
}

// MARK: - 2. SAFE TO SKIP PERIODS (Liquid Glass Tile)
@Composable
fun SafeToSkipGlassCard(
    attendance: AttendanceResponse,
    targetThreshold: Double = 75.0,
    modifier: Modifier = Modifier
) {
    val percentage = attendance.overallPercentage
    val attended = attendance.attendedCount
    val held = attendance.heldCount
    val targetFactor = targetThreshold / 100.0

    val periodsCanSkip = if (held > 0 && percentage >= targetThreshold) {
        val calculated = kotlin.math.floor((attended - targetFactor * held) / targetFactor).toInt()
        maxOf(0, calculated)
    } else 0

    val periodsNeedToAttend = if (held > 0 && percentage < targetThreshold) {
        val divisor = 1.0 - targetFactor
        val calculated = if (divisor > 0.0) {
            kotlin.math.ceil((targetFactor * held - attended) / divisor).toInt()
        } else 0
        maxOf(0, calculated)
    } else 0

    val isSafe = percentage >= targetThreshold
    val statusColor = if (isSafe) MacSafe else MacCritical
    val labelText = if (isSafe) "Safe to Skip:" else "Must Attend:"
    val countText = if (isSafe) "$periodsCanSkip Periods" else "$periodsNeedToAttend Periods"

    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        borderBrush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                statusColor.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Squircle glowing icon badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(0.75.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Title label
                Text(
                    text = labelText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            // Prominently highlighted period count capsule (No "Buffer above 75%" subtitle!)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.16f))
                    .border(0.75.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.5.dp)
            ) {
                Text(
                    text = countText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
        }
    }
}

// MARK: - 3. TODAY'S ATTENDANCE FROM ATTENDANCE GRID (Liquid Glass Tile)
@Composable
fun TodayAttendanceGlassCard(
    attendance: AttendanceResponse,
    modifier: Modifier = Modifier,
    onNavigateToGrid: () -> Unit
) {
    val todayDate = SimpleDateFormat("dd/MM", Locale.US).format(Date())
    val gridData = attendance.getTodayGridAttendance(todayDate)
    val timeline = gridData?.timeline?.takeIf { it.isNotEmpty() } ?: attendance.getTodayAttendanceTimeline(todayDate)
    val records = gridData?.records ?: emptyList()

    val (presentCount, absentCount) = if (gridData != null && gridData.timeline.isNotEmpty()) {
        gridData.presentCount to gridData.absentCount
    } else if (timeline.isNotEmpty()) {
        timeline.count { it == AttendanceStatus.PRESENT } to timeline.count { it == AttendanceStatus.ABSENT }
    } else {
        val p = records.sumOf { rec -> rec.statusString.uppercase().count { it == 'P' } }
        val a = records.sumOf { rec -> rec.statusString.uppercase().count { it == 'A' } }
        p to a
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "TodayCardPress"
    )

    LiquidGlassCard(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                onClick = onNavigateToGrid
            ),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        // Header Row: TODAY'S ATTENDANCE (dd/MM) + 7P • 0A Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TODAY'S ATTENDANCE",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.65f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "(${gridData?.dateHeader ?: todayDate})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            if (presentCount > 0 || absentCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "${presentCount}P",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (presentCount > 0) MacSafe else Color.White.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                    Text(
                        text = "${absentCount}A",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (absentCount > 0) MacCritical else Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Grid",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Period Timeline Dots Row (P1 •, P2 •, P3 •, ...)
        if (timeline.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeline.forEachIndexed { index, status ->
                    val dotColor = when (status) {
                        AttendanceStatus.PRESENT -> MacSafe
                        AttendanceStatus.ABSENT -> MacCritical
                        AttendanceStatus.HOLIDAY -> MacPurple
                        AttendanceStatus.LEAVE -> MacAmber
                        else -> Color.White.copy(alpha = 0.3f)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(dotColor.copy(alpha = 0.12f))
                            .border(0.5.dp, dotColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "P${index + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = dotColor)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Marked subjects in today's grid
        if (records.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                records.forEach { record ->
                    val isP = record.status == AttendanceStatus.PRESENT || record.statusString.contains("P", ignoreCase = true)
                    val isA = record.status == AttendanceStatus.ABSENT || (record.statusString.contains("A", ignoreCase = true) && !record.statusString.contains("P", ignoreCase = true))
                    val recColor = if (isP) MacSafe else if (isA) MacCritical else MacPurple
                    val statusText = if (isP) "Present" else if (isA) "Absent" else record.status.title

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Monospaced marks pill: [PPPPPPP]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(recColor.copy(alpha = 0.15f))
                                .border(0.5.dp, recColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = record.statusString.uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = recColor
                            )
                        }

                        // Subject Title
                        Text(
                            text = record.subjectName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Status Badge
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = recColor
                        )
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "No attendance marks recorded in grid for today yet",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// MARK: - Liquid Glass Navigation Card
@Composable
fun GlassMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "GlassCardPressAnim"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(GlassCardBg)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        GlassCardBorderTop,
                        GlassCardBorderMid,
                        GlassCardBorderBottom
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Glowing circular/squircle icon badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBgColor)
                    .border(0.75.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(11.dp), spotColor = iconTint.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// MARK: - Legacy Components (Kept for backwards compatibility)
@Composable
fun AttendanceGauge(percentage: Double, threshold: Int, modifier: Modifier = Modifier) {
    val targetProgress = (percentage / 100f).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "GaugeAnimation"
    )
    val indicatorColor = if (percentage >= threshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(160.dp),
            strokeWidth = 12.dp,
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.getDefault(), "%.2f%%", percentage),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PeriodCircle(periodNumber: Int, status: Char) {
    val containerColor = when (status.uppercaseChar()) {
        'P' -> MaterialTheme.colorScheme.primaryContainer
        'A' -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (status.uppercaseChar()) {
        'P' -> MaterialTheme.colorScheme.onPrimaryContainer
        'A' -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(containerColor, CircleShape)
    ) {
        Text(
            text = status.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
