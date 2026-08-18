package com.massago.customer.ui.screens.splash

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.massago.customer.R
import kotlinx.coroutines.delay

@Composable
fun CustomerSplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Make status bar & navigation bar fully transparent edge-to-edge during splash
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF059669)), // Full edge-to-edge emerald green
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim)
        ) {
            // Official Pure White Vector Emblem from logo.svg
            Image(
                painter = painterResource(id = R.drawable.brand_logo_vector),
                contentDescription = "MassaGo Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Modern, lighter, aesthetic typography
            Text(
                text = "MassaGo",
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MASSAGE & WELLNESS",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
fun MassageSilhouetteEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Therapist Head
        drawCircle(
            color = Color.White,
            radius = w * 0.08f,
            center = Offset(w * 0.32f, h * 0.22f)
        )

        // Therapist Torso & Arms (Leaning forward in massage posture)
        val therapistPath = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            cubicTo(
                w * 0.22f, h * 0.34f,
                w * 0.26f, h * 0.28f,
                w * 0.34f, h * 0.28f
            )
            cubicTo(
                w * 0.39f, h * 0.28f,
                w * 0.44f, h * 0.32f,
                w * 0.48f, h * 0.37f
            )
            lineTo(w * 0.56f, h * 0.44f)
            cubicTo(
                w * 0.58f, h * 0.46f,
                w * 0.56f, h * 0.48f,
                w * 0.53f, h * 0.49f
            )
            cubicTo(
                w * 0.50f, h * 0.49f,
                w * 0.47f, h * 0.47f,
                w * 0.45f, h * 0.45f
            )
            lineTo(w * 0.41f, h * 0.41f)
            lineTo(w * 0.41f, h * 0.54f)
            close()
        }
        drawPath(therapistPath, color = Color.White)

        // Client Head (Relaxed on headrest)
        drawCircle(
            color = Color.White,
            radius = w * 0.065f,
            center = Offset(w * 0.76f, h * 0.47f)
        )

        // Client Body (Lying relaxed on massage table)
        val clientBodyPath = Path().apply {
            moveTo(w * 0.42f, h * 0.52f)
            cubicTo(
                w * 0.48f, h * 0.48f,
                w * 0.56f, h * 0.48f,
                w * 0.64f, h * 0.50f
            )
            cubicTo(
                w * 0.69f, h * 0.51f,
                w * 0.73f, h * 0.52f,
                w * 0.75f, h * 0.54f
            )
            cubicTo(
                w * 0.75f, h * 0.56f,
                w * 0.72f, h * 0.58f,
                w * 0.68f, h * 0.59f
            )
            cubicTo(
                w * 0.60f, h * 0.60f,
                w * 0.50f, h * 0.60f,
                w * 0.42f, h * 0.59f
            )
            close()
        }
        drawPath(clientBodyPath, color = Color.White)

        // Minimalist Massage Bed Surface
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.16f, h * 0.62f),
            size = Size(w * 0.68f, h * 0.045f),
            cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
        )

        // Minimalist Bed Legs
        drawLine(
            color = Color.White,
            start = Offset(w * 0.26f, h * 0.665f),
            end = Offset(w * 0.24f, h * 0.82f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color.White,
            start = Offset(w * 0.74f, h * 0.665f),
            end = Offset(w * 0.76f, h * 0.82f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
    }
}
