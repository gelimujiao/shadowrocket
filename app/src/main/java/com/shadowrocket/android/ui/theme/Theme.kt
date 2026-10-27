package com.shadowrocket.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ShadowrocketBlue = Color(0xFF007AFF)
val ShadowrocketDarkBlue = Color(0xFF0A58CA)
val ShadowrocketGreen = Color(0xFF34C759)
val ShadowrocketOrange = Color(0xFFFF9500)
val ShadowrocketRed = Color(0xFFFF3B30)

private val DarkColorScheme = darkColorScheme(
    primary = ShadowrocketBlue,
    secondary = ShadowrocketGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = ShadowrocketBlue,
    secondary = ShadowrocketDarkBlue,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E)
)

@Composable
fun ShadowrocketTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
