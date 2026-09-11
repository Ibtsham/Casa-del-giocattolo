package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBg,
    secondary = DarkSecondary,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = GrayBgLight,
    onSurface = GrayBgLight,
    outline = DarkSecondary,
    outlineVariant = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = GrayCardLight,
    primaryContainer = RedLight,
    onPrimaryContainer = RedDark,
    background = GrayBgLight,
    surface = GrayCardLight,
    onBackground = GrayTextPrimary,
    onSurface = GrayTextPrimary,
    outline = OutlineVariant,
    outlineVariant = BorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to force our beautiful customized brand colors!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
