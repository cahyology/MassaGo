package com.massago.customer.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CardSurface,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDeep,
    secondary = AmberGold,
    onSecondary = CardSurface,
    secondaryContainer = WarmSand,
    onSecondaryContainer = AmberDark,
    tertiary = LavenderAccent,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = MintSoft,
    onPrimary = SlateDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = AmberGold,
    onSecondary = SlateDark,
    secondaryContainer = AmberDark,
    onSecondaryContainer = WarmSand,
    tertiary = LavenderLight,
    background = SlateDark,
    onBackground = CardSurface,
    surface = SlateCard,
    onSurface = CardSurface,
    surfaceVariant = SlateBorder,
    onSurfaceVariant = TextMuted,
    outline = SlateBorder,
    error = StatusError
)

@Composable
fun MassaGoCustomerTheme(
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
