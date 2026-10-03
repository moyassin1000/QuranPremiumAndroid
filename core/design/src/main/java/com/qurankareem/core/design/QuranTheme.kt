package com.qurankareem.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald = Color(0xFF0D5C46)
private val EmeraldDeep = Color(0xFF071A14)
private val Gold = Color(0xFFD6B56C)
private val Ivory = Color(0xFFF8F3E8)
private val Paper = Color(0xFFFFFBF2)
private val SoftGreen = Color(0xFFE2EFE9)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Gold,
    background = Paper,
    surface = Color.White,
    onBackground = EmeraldDeep,
    onSurface = EmeraldDeep,
    surfaceVariant = SoftGreen,
    onSurfaceVariant = Color(0xFF49645A),
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = EmeraldDeep,
    secondary = Color(0xFF85C7AE),
    background = EmeraldDeep,
    surface = Color(0xFF0B251D),
    onBackground = Ivory,
    onSurface = Ivory,
    surfaceVariant = Color(0xFF14372D),
    onSurfaceVariant = Color(0xFFBFD7CD),
)

private val AmoledColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color.Black,
    secondary = Color(0xFF8FD7BB),
    background = Color.Black,
    surface = Color(0xFF050505),
    onBackground = Ivory,
    onSurface = Ivory,
    surfaceVariant = Color(0xFF111713),
    onSurfaceVariant = Color(0xFFC8D4CF),
)

@Composable
fun QuranPremiumTheme(
    darkTheme: Boolean = false,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when {
            amoled -> AmoledColors
            darkTheme -> DarkColors
            else -> LightColors
        },
        content = content,
    )
}
