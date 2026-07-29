package com.isyarat.pintar.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun IsyaratPintarTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = HH_Button,
        onPrimary = HH_ButtonText,
        primaryContainer = HH_Button,
        onPrimaryContainer = HH_ButtonText,
        secondary = HH_Secondary,
        onSecondary = HH_ButtonText,
        tertiary = HH_Tertiary,
        onTertiary = HH_ButtonText,
        background = HH_Background,
        onBackground = HH_Paragraph,
        surface = HH_Background,
        onSurface = HH_Headline,
        surfaceVariant = HH_Secondary,
        onSurfaceVariant = HH_Headline
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = HH_Background.toArgb()
            window.navigationBarColor = HH_Secondary.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = HH_Background.luminance() > 0.5f
            insetsController.isAppearanceLightNavigationBars = HH_Secondary.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
