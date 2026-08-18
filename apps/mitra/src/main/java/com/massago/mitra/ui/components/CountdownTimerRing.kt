package com.massago.mitra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.ui.theme.AmberGold
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.StatusAlertRed

@Composable
fun CountdownTimerRing(
    remainingSeconds: Int,
    totalSeconds: Int = 30,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp
) {
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedSweepAngle by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(400),
        label = "countdown_sweep"
    )

    val ringColor = when {
        remainingSeconds <= 5 -> StatusAlertRed
        remainingSeconds <= 12 -> AmberGold
        else -> EmeraldPrimary
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background Track
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Active Countdown Arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${remainingSeconds}s",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ringColor,
            fontSize = 16.sp
        )
    }
}
