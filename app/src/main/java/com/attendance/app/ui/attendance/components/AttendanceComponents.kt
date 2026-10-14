package com.attendance.app.ui.attendance.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.app.data.model.SimulationResult
import com.attendance.app.data.model.SubjectSummary
import com.attendance.app.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubjectCard(subject: SubjectSummary, threshold: Int, onClick: () -> Unit) {
    val percentage = subject.percentageDouble
    val isSafe = percentage >= threshold.toDouble()
    val statusColor = if (isSafe) MacSafe else MacCritical
    val statusGlow = if (isSafe) MacSafeGlow else MacCriticalGlow
    val progressGradient = if (isSafe) {
        Brush.horizontalGradient(listOf(MacSafeGradientStart, MacSafeGradientEnd))
    } else {
        Brush.horizontalGradient(listOf(MacCriticalGradientStart, MacCriticalGradientEnd))
    }

    val targetProgress = (percentage / 100f).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "SubjectProgressAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "SubjectCardPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
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
                indication = ripple(color = Color.White.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                // Glowing percentage
                Text(
                    text = subject.percentage,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Liquid progress bar
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                val totalWidth = maxWidth
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                )
                // Fill
                Box(
                    modifier = Modifier
                        .width(totalWidth * animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressGradient)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(3.dp), spotColor = statusGlow)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attended / Held count in monospaced capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Attended: ${subject.attended} / ${subject.held}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Frosted Status Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isSafe) "On Track" else "Low Attendance",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceSimulatorCard(
    simResult: SimulationResult,
    simulatedLeaves: List<Long>,
    onAddLeaveClick: () -> Unit,
    onRemoveDate: (Long) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSafe = simResult.simulatedPercentage >= 75.0
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val accent = LocalAccentColor.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
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
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .border(0.75.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Leave Simulator",
                            tint = accent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "Leave & Bunk Simulator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (simulatedLeaves.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MacCritical.copy(alpha = 0.15f))
                            .border(0.5.dp, MacCritical.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { onResetClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacCritical
                        )
                    }
                }
            }

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Periods Can Skip / Needed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSafe) MacSafe.copy(alpha = 0.12f) else MacCritical.copy(alpha = 0.12f))
                        .border(
                            0.75.dp,
                            if (isSafe) MacSafe.copy(alpha = 0.3f) else MacCritical.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (simResult.simulatedCanSkip > 0) "${simResult.simulatedCanSkip} Periods" else "${simResult.simulatedNeedToAttend} Needed",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSafe) MacSafe else MacCritical
                        )
                        Text(
                            text = if (simResult.simulatedCanSkip > 0) "Periods Can Skip" else "Classes Required",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Projected Attendance
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", simResult.simulatedPercentage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSafe) MacSafe else MacCritical
                        )
                        Text(
                            text = if (simulatedLeaves.isEmpty()) "Current Attendance" else "Projected Attendance",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Planned leaves pills
            if (simulatedLeaves.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Planned Leaves:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        simulatedLeaves.forEach { millis ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(millis)),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove date",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveDate(millis) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liquid Glass Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.85f), accent)
                        )
                    )
                    .border(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f))
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), spotColor = accent.copy(alpha = 0.5f))
                    .clickable { onAddLeaveClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Select Future Leave Date",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
