package com.voiceofmelody.songdailytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = InstagramCoral,
    onPrimary = Color.White,
    secondary = InstagramPurple,
    onSecondary = Color.White,
    tertiary = MusicCyan,
    onTertiary = Color.White,
    background = SlateDarkBackground,
    onBackground = TextPrimaryDark,
    surface = SlateDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = InstagramCoral,
    onPrimary = Color.White,
    secondary = InstagramPurple,
    onSecondary = Color.White,
    tertiary = MusicCyan,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFE),
    onBackground = Color(0xFF1F1F24),
    surface = Color.White,
    onSurface = Color(0xFF1F1F24),
    surfaceVariant = Color(0xFFF1F1F5),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to true for dark slate look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
