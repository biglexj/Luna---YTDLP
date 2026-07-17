package com.biglexj.lunafetch.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode { System, Light, Dark }

@Composable
expect fun platformDynamicColorScheme(useDarkTheme: Boolean): ColorScheme?

private val LunaLightColors = lightColorScheme(
    primary = Color(0xFF006B5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9DF2E1),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635D),
    secondaryContainer = Color(0xFFCDE8E0),
    tertiary = Color(0xFF446179),
    background = Color(0xFFF7FCFA),
    surface = Color(0xFFF7FCFA),
    surfaceVariant = Color(0xFFDAE5E1),
    outline = Color(0xFF6F7976),
)

private val LunaDarkColors = darkColorScheme(
    primary = Color(0xFF81D5C5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF9DF2E1),
    secondary = Color(0xFFB1CCC4),
    secondaryContainer = Color(0xFF334B46),
    tertiary = Color(0xFFABCBE5),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF404946),
    outline = Color(0xFF899390),
)

private val LunaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

@Composable
fun LunaFetchTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = platformDynamicColorScheme(dark) ?: if (dark) LunaDarkColors else LunaLightColors
    MaterialTheme(colorScheme = colors, shapes = LunaShapes, content = content)
}
