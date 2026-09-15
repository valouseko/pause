package cz.honestlead.mezera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PauseColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueWash,
    onPrimaryContainer = BlueStrong,
    secondary = BlueSoft,
    onSecondary = Color.White,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = BgDeep,
    onSurfaceVariant = InkSoft,
    outline = LineStrong,
    outlineVariant = Line,
    error = Color(0xFFFF6B81)
)

@Composable
fun MezeraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PauseColors,
        typography = AppTypography,
        content = content
    )
}
