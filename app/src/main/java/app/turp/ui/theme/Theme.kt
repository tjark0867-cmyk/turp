package app.turp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.R

// turp-Farben: fast schwarzer Grund, knallige Bonbonfarben
val Ink = Color(0xFF09090F)
val InkElevated = Color(0xFF15151E)
val InkHigh = Color(0xFF22222E)
val InkLine = Color(0xFF34344A)
val Teal = Color(0xFF2DE1C2)
val TealDark = Color(0xFF17A58D)
val Coral = Color(0xFFFF4F8B)
val CoralDark = Color(0xFFC92F66)
val Sun = Color(0xFFFFD23F)
val SunDark = Color(0xFFD9A800)
val Lilac = Color(0xFFA78BFF)
val LilacDark = Color(0xFF7458E0)
val Sky = Color(0xFF4DB8FF)
val Night = Color(0xFF16112E)
val Online = Color(0xFF5BE35B)
val TextHigh = Color(0xFFF6F6FB)
val TextLow = Color(0xFFA6A6BE)

/** Dicke, runde Sticker-Schrift (Lilita One, SIL Open Font License) */
val Display = FontFamily(Font(R.font.lilita_one, FontWeight.Normal))

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

private fun display(size: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = size.sp, letterSpacing = spacing.sp)

private val typography = Typography(
    displayLarge = display(52),
    displayMedium = display(44),
    displaySmall = display(36),
    headlineLarge = display(34),
    headlineMedium = display(30),
    headlineSmall = display(25),
    titleLarge = display(22),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = display(17, 0.3f),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/** Großes Logo-Schriftbild */
val LogoStyle = TextStyle(fontFamily = Display, fontSize = 56.sp, letterSpacing = 0.sp)

@Composable
fun TurpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
