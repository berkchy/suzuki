package hime.suzuki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBlueScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF04182F),
    primaryContainer = Color(0xFF1B3A66),
    onPrimaryContainer = Color(0xFFD9E5FF),
    secondary = Cyan,
    onSecondary = Color(0xFF03202F),
    secondaryContainer = Color(0xFF113A50),
    onSecondaryContainer = Color(0xFFCBE9FF),
    tertiary = Indigo,
    onTertiary = Color(0xFF10173A),
    tertiaryContainer = Color(0xFF28305F),
    onTertiaryContainer = Color(0xFFE0E3FF),
    background = NightBlue,
    onBackground = TextPrimary,
    surface = DeepBlue,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBlue,
    outlineVariant = Color(0xFF22324A),
    error = ErrorRed,
    onError = Color(0xFF320909),
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xCC000000),
)

@Composable
fun SuzukiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkBlueScheme,
        typography = SuzukiTypography,
        shapes = SuzukiShapes,
        content = content,
    )
}
