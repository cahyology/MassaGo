package com.pijatin.mitra.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ☀️ Clean Modern Light Theme (Gojek / Grab Pearl Look)
private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CardLight,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDeepForest,
    
    secondary = AmberGold,
    onSecondary = CardLight,
    secondaryContainer = AmberLight,
    onSecondaryContainer = AmberDark,
    
    tertiary = LavenderAroma,
    onTertiary = CardLight,
    tertiaryContainer = LavenderLight,
    onTertiaryContainer = LavenderDark,
    
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLightSubtle,
    onSurfaceVariant = TextSecondaryLight,
    
    error = StatusAlertRed,
    onError = CardLight,
    outline = BorderLight
)

// 🌙 Midnight Luxury Dark Theme (OLED Friendly)
private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = BgDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    
    secondary = AmberGold,
    onSecondary = BgDark,
    secondaryContainer = CardDarkElevated,
    onSecondaryContainer = AmberGold,
    
    tertiary = LavenderAroma,
    onTertiary = BgDark,
    tertiaryContainer = CardDarkElevated,
    onTertiaryContainer = LavenderLight,
    
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    
    error = StatusAlertRed,
    onError = TextPrimaryDark,
    outline = BorderDark
)

@Composable
fun PijatInTheme(
    darkTheme: Boolean = false, // Default Light Mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun PijatInMitraTheme(
    darkTheme: Boolean = false, // Default Light Mode
    content: @Composable () -> Unit
) {
    PijatInTheme(darkTheme = darkTheme, content = content)
}
