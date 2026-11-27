package br.com.gabriel.gastocerto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6C3E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5F4B9),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF4F6354),
    secondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFF3B6470),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF6FBF3),
    surface = Color(0xFFF6FBF3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5ED),
    surfaceContainer = Color(0xFFEAEFE7),
    surfaceContainerHigh = Color(0xFFE5EAE2),
    surfaceContainerHighest = Color(0xFFDFE4DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D79E),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF00522A),
    onPrimaryContainer = Color(0xFFA5F4B9),
    secondary = Color(0xFFB5CCBA),
    secondaryContainer = Color(0xFF374B3D),
    tertiary = Color(0xFFA3CDDB),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0F1511),
    surface = Color(0xFF0F1511),
    surfaceContainerLowest = Color(0xFF0A0F0B),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF252B27),
    surfaceContainerHighest = Color(0xFF303632),
)

/** GastoCerto theme (Material 3), following the system light/dark setting. */
@Composable
fun GastoCertoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

/** Color for amounts: income/positive in the primary color, expenses/negative in the error color. */
@Composable
fun amountColor(positive: Boolean): Color =
    if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
