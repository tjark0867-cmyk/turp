package app.turp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// turp-Farben: nachtblauer Grund, Türkis als Hauptfarbe, Koralle als Akzent
val Ink = Color(0xFF0E0F1A)
val InkElevated = Color(0xFF171A2C)
val InkHigh = Color(0xFF22263D)
val InkLine = Color(0xFF353A58)
val Teal = Color(0xFF2DE1C2)
val Coral = Color(0xFFFF5C8A)
val Sun = Color(0xFFFFC93C)
val Lilac = Color(0xFF9B8CFF)
val Night = Color(0xFF1B1440)
val TextHigh = Color(0xFFF4F5FB)
val TextLow = Color(0xFFA3A7C2)

private val colors = darkColorScheme(
    primary = Teal,
    onPrimary = Ink,
    primaryContainer = Color(0xFF0F4A42),
    onPrimaryContainer = Teal,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1830),
    onSecondaryContainer = Coral,
    tertiary = Sun,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextHigh,
    surface = InkElevated,
    onSurface = TextHigh,
    surfaceVariant = InkHigh,
    onSurfaceVariant = TextLow,
    surfaceTint = Teal,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkElevated,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHigh,
    outline = InkLine,
    outlineVariant = InkLine,
    error = Color(0xFFFF6B6B),
    onError = Ink,
)

private val base = Typography()

private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Großes Logo-Schriftbild */
val LogoStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)

@Composable
fun TurpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
