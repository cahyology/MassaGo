package com.massago.mitra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldPrimary
import kotlin.math.roundToInt

@Composable
fun SlideToConfirm(
    modifier: Modifier = Modifier,
    text: String = "Geser untuk Terima Order",
    icon: ImageVector = Icons.Default.ChevronRight,
    backgroundColor: Color = EmeraldPrimary,
    onConfirmed: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(backgroundColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val thumbSizePx = with(density) { 50.dp.toPx() }
        val maxDragPx = (constraints.maxWidth.toFloat() - thumbSizePx - with(density) { 8.dp.toPx() }).coerceAtLeast(1f)

        val animatedOffset by animateFloatAsState(
            targetValue = if (isConfirmed) maxDragPx else offsetX,
            animationSpec = tween(150),
            label = "slide_offset"
        )

        // Center Text
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = backgroundColor,
                fontSize = 15.sp
            )
        }

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt() + with(density) { 4.dp.roundToPx() }, 0) }
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(backgroundColor, EmeraldDark)
                    )
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!isConfirmed) {
                            val newOffset = (offsetX + delta).coerceIn(0f, maxDragPx)
                            offsetX = newOffset
                        }
                    },
                    onDragStopped = {
                        if (offsetX > maxDragPx * 0.75f) {
                            isConfirmed = true
                            onConfirmed()
                        } else {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
