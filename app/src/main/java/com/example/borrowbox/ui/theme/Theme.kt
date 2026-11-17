package com.example.borrowbox.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.borrowbox.data.repository.ThemeConfig

data class StatusColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        success = SuccessGreen,
        warning = WarningOrange,
        error = ErrorRed,
        info = InfoBlue
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = DarkPurple,
    onPrimaryContainer = WhiteText,
    secondary = LightPurple,
    onSecondary = Black,
    background = DarkBackground,
    onBackground = WhiteText,
    surface = SurfaceDark,
    onSurface = WhiteText,
    surfaceVariant = CardSurfaceDark,
    onSurfaceVariant = SecondaryTextDark,
    error = ErrorRedDark,
    onError = Black,
    outline = SecondaryTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = LightPurple,
    onPrimaryContainer = DarkPurple,
    secondary = DarkPurple,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = Color.White,
    onSurfaceVariant = SecondaryTextLight,
    error = ErrorRed,
    onError = Color.White,
    outline = SecondaryTextLight
)

@Composable
fun BorrowBoxTheme(
    themeConfig: ThemeConfig = ThemeConfig.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeConfig) {
        ThemeConfig.LIGHT -> false
        ThemeConfig.DARK -> true
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val statusColors = if (darkTheme) {
        StatusColors(SuccessGreenDark, WarningOrangeDark, ErrorRedDark, InfoBlueDark)
    } else {
        StatusColors(SuccessGreen, WarningOrange, ErrorRed, InfoBlue)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Assuming Typography is defined or default
            content = content
        )
    }
}
