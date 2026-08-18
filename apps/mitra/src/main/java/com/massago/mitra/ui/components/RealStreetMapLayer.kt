package com.massago.mitra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.model.OrderStatus
import com.massago.mitra.ui.theme.AmberGold
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.StatusAlertRed

@Composable
fun RealStreetMapLayer(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    activeOrder: Order? = null
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2800, easing = LinearEasing)),
        label = "p1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2800, delayMillis = 1400, easing = LinearEasing)),
        label = "p2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.7f, 2.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cx = (width * 0.5f + offsetX)
            val cy = (height * 0.45f + offsetY)

            // 1. Waterways / Ciliwung River
            val riverPath = Path().apply {
                moveTo(-200f + offsetX, height * 0.1f + offsetY)
                cubicTo(
                    width * 0.3f + offsetX, height * 0.2f + offsetY,
                    width * 0.7f + offsetX, height * 0.05f + offsetY,
                    width + 200f + offsetX, height * 0.25f + offsetY
                )
                lineTo(width + 200f + offsetX, height * 0.32f + offsetY)
                cubicTo(
                    width * 0.7f + offsetX, height * 0.12f + offsetY,
                    width * 0.3f + offsetX, height * 0.27f + offsetY,
                    -200f + offsetX, height * 0.17f + offsetY
                )
                close()
            }
            drawPath(path = riverPath, color = Color(0xFFBAE6FD))

            // 2. Green Parks & Urban Areas (GBK / Taman Kota)
            drawRoundRect(
                color = Color(0xFFDCFCE7),
                topLeft = Offset(width * 0.1f + offsetX, height * 0.35f + offsetY),
                size = Size(width * 0.25f * zoomScale, height * 0.12f * zoomScale),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(width * 0.65f + offsetX, height * 0.18f + offsetY),
                size = Size(width * 0.28f * zoomScale, height * 0.15f * zoomScale),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // 3. Major Highway Arterials (Sudirman, Gatot Subroto, Rasuna Said)
            // Main Arterial (Jl. Jend. Sudirman)
            val sudirmanWidth = 34.dp.toPx() * zoomScale
            drawLine(
                color = Color.White,
                start = Offset(width * 0.45f + offsetX, -200f + offsetY),
                end = Offset(width * 0.55f + offsetX, height + 200f + offsetY),
                strokeWidth = sudirmanWidth
            )
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(width * 0.45f + offsetX, -200f + offsetY),
                end = Offset(width * 0.55f + offsetX, height + 200f + offsetY),
                strokeWidth = sudirmanWidth + 4.dp.toPx()
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.45f + offsetX, -200f + offsetY),
                end = Offset(width * 0.55f + offsetX, height + 200f + offsetY),
                strokeWidth = sudirmanWidth
            )

            // Cross Arterial (Jl. Gatot Subroto)
            val gatsuWidth = 30.dp.toPx() * zoomScale
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(-200f + offsetX, height * 0.48f + offsetY),
                end = Offset(width + 200f + offsetX, height * 0.42f + offsetY),
                strokeWidth = gatsuWidth + 4.dp.toPx()
            )
            drawLine(
                color = Color.White,
                start = Offset(-200f + offsetX, height * 0.48f + offsetY),
                end = Offset(width + 200f + offsetX, height * 0.42f + offsetY),
                strokeWidth = gatsuWidth
            )

            // Secondary Roads (SCBD Boulevard, Senopati, Kuningan)
            val secWidth = 18.dp.toPx() * zoomScale
            drawLine(
                color = Color.White,
                start = Offset(-100f + offsetX, height * 0.25f + offsetY),
                end = Offset(width + 100f + offsetX, height * 0.28f + offsetY),
                strokeWidth = secWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(-100f + offsetX, height * 0.72f + offsetY),
                end = Offset(width + 100f + offsetX, height * 0.68f + offsetY),
                strokeWidth = secWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.2f + offsetX, -100f + offsetY),
                end = Offset(width * 0.15f + offsetX, height + 100f + offsetY),
                strokeWidth = secWidth
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.8f + offsetX, -100f + offsetY),
                end = Offset(width * 0.85f + offsetX, height + 100f + offsetY),
                strokeWidth = secWidth
            )

            // 4. Street Name Text Labels (Native Paint)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(100, 116, 139)
                textSize = 28f * zoomScale
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText("Jl. Jend. Sudirman", width * 0.53f + offsetX, height * 0.22f + offsetY, paint)
            drawContext.canvas.nativeCanvas.drawText("Jl. Gatot Subroto", width * 0.25f + offsetX, height * 0.46f + offsetY, paint)
            drawContext.canvas.nativeCanvas.drawText("Kawasan SCBD", width * 0.75f + offsetX, height * 0.32f + offsetY, paint)
            drawContext.canvas.nativeCanvas.drawText("Taman Senopati", width * 0.22f + offsetX, height * 0.41f + offsetY, paint)

            // 5. Radar Scan Ripple Rings when ONLINE
            if (isOnline && activeOrder == null) {
                val maxRadius = width * 0.6f * zoomScale

                drawCircle(
                    color = EmeraldPrimary.copy(alpha = (1f - pulse1).coerceIn(0f, 0.35f)),
                    radius = maxRadius * pulse1,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    color = EmeraldPrimary.copy(alpha = (1f - pulse2).coerceIn(0f, 0.35f)),
                    radius = maxRadius * pulse2,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )

                // 2.5 km Service Boundary Circle
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.08f),
                    radius = maxRadius * 0.85f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.5f),
                    radius = maxRadius * 0.85f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
                )
            }

            // 6. Active Order Route & Client Pin
            if (activeOrder != null && activeOrder.status != OrderStatus.INCOMING) {
                val clientX = width * 0.72f + offsetX
                val clientY = height * 0.25f + offsetY

                // Glowing Route Line
                val route = Path().apply {
                    moveTo(cx, cy)
                    lineTo(width * 0.52f + offsetX, height * 0.35f + offsetY)
                    lineTo(clientX, clientY)
                }

                drawPath(
                    path = route,
                    color = EmeraldPrimary.copy(alpha = 0.3f),
                    style = Stroke(width = 16.dp.toPx())
                )
                drawPath(
                    path = route,
                    color = EmeraldPrimary,
                    style = Stroke(width = 6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f))
                )

                // Client Destination Pin (Red/Amber)
                drawCircle(
                    color = AmberGold,
                    radius = 18.dp.toPx(),
                    center = Offset(clientX, clientY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(clientX, clientY)
                )
            }

            // 7. Mitra GPS Position Pin (🛵 Scooter with Headlight Pulse)
            drawCircle(
                color = EmeraldPrimary.copy(alpha = 0.25f),
                radius = 26.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = EmeraldDark,
                radius = 16.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        // Floating Badges & Controls
        if (isOnline && activeOrder == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 135.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = StatusAlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Area Ramai: SCBD & Sudirman",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}
