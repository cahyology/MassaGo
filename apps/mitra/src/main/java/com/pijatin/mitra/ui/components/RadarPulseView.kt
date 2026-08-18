package com.pijatin.mitra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pijatin.mitra.ui.theme.EmeraldDark
import com.pijatin.mitra.ui.theme.EmeraldPrimary

@Composable
fun RadarPulseView(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_transition")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isOnline) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.minDimension / 2f) * 0.9f

                // Draw expanding rings
                listOf(wave1, wave2, wave3).forEach { progress ->
                    val currentRadius = maxRadius * progress
                    val alpha = (1f - progress).coerceIn(0f, 0.7f)

                    drawCircle(
                        color = EmeraldPrimary.copy(alpha = alpha * 0.25f),
                        radius = currentRadius,
                        center = centerOffset
                    )

                    drawCircle(
                        color = EmeraldPrimary.copy(alpha = alpha),
                        radius = currentRadius,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Center Location Pin
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isOnline) {
                        Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDark))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B)))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Posisi Terapis",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "PijatIn Mitra - Radar Pulse")
@Composable
fun RadarPulseViewPreview() {
    com.pijatin.mitra.ui.theme.PijatInMitraTheme {
        RadarPulseView(
            modifier = Modifier.size(240.dp),
            isOnline = true
        )
    }
}
