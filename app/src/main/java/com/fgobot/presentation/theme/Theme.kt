/**
 * FGO Bot - Jetpack Compose Theme Configuration
 * 
 * This file defines the Material Design 3 theme for the FGO Bot application.
 * It provides both light and dark theme support with custom FGO-inspired colors.
 * 
 * Features:
 * - Material Design 3 color scheme
 * - Dynamic color support (Android 12+)
 * - Light and dark theme variants
 * - Custom FGO-inspired color palette
 */

package com.fgobot.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// FGO-inspired color palette
private val FGOBlue = Color(0xFF1976D2)
private val FGOBlueDark = Color(0xFF0D47A1)
private val FGOGold = Color(0xFFFFC107)
private val FGOGoldDark = Color(0xFFFF8F00)
private val FGORed = Color(0xFFD32F2F)
private val FGOGreen = Color(0xFF388E3C)

/**
 * Light color scheme for the FGO Bot application
 * Uses FGO-inspired colors with Material Design 3 guidelines
 */
private val LightColorScheme = lightColorScheme(
    primary = FGOBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = FGOBlueDark,
    
    secondary = FGOGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = FGOGoldDark,
    
    tertiary = FGOGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E8),
    onTertiaryContainer = Color(0xFF1B5E20),
    
    error = FGORed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFF90CAF9)
)

/**
 * Dark color scheme for the FGO Bot application
 * Optimized for dark mode with proper contrast ratios
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = FGOBlueDark,
    primaryContainer = FGOBlueDark,
    onPrimaryContainer = Color(0xFFE3F2FD),
    
    secondary = FGOGold,
    onSecondary = Color.Black,
    secondaryContainer = FGOGoldDark,
    onSecondaryContainer = Color(0xFFFFF8E1),
    
    tertiary = Color(0xFFA5D6A7),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFE8F5E8),
    
    error = Color(0xFFEF9A9A),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),
    
    background = Color(0xFF10131C),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF10131C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = FGOBlue
)

/**
 * FGOBotTheme - Main theme composable for the application
 * 
 * This composable applies the FGO Bot theme to its content, supporting both
 * light and dark modes with optional dynamic color theming on Android 12+.
 * 
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic colors on Android 12+ (defaults to true)
 * @param content The composable content to theme
 */
@Composable
fun FGOBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 