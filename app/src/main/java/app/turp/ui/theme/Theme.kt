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

// kumi-Farben: tiefes Schwarz, Grautöne und genau EINE Akzentfarbe (Mint).
val Ink = Color(0xFF050506)
val InkElevated = Color(0xFF121214)
val InkHigh = Color(0xFF1C1C20)
val InkLine = Color(0xFF2A2A30)
val Teal = Color(0xFF2DE1C2)          // Akzent: Logo, aktive Icons, Haupt-Buttons
val TealDark = Color(0xFF1AA88F)
val Coral = Color(0xFFFF4D6D)         // nur für Warnungen, Abzeichen, Ablehnen
val CoralDark = Color(0xFFC7344F)
val Sun = Color(0xFFFFC94D)
val SunDark = Color(0xFFD9A21F)
val Lilac = Color(0xFF9C8CFF)
val LilacDark = Color(0xFF6E5BE0)
val Sky = Color(0xFF4DB8FF)
val Night = Color(0xFF0D0B17)
val Online = Color(0xFF3DDC84)
val TextHigh = Color(0xFFF5F5F7)
val TextLow = Color(0xFF8E8E96)

/** Runde Marken-Schrift (Lilita One, SIL OFL) – nur für Logo und Namensschilder */
val Display = FontFamily(Font(R.font.lilita_one, FontWeight.Normal))

/** Filzstift-Schrift für handgeschriebene Stellen (Namensschild, Kofferanhänger) */
val Marker = FontFamily(Font(R.font.permanent_marker, FontWeight.Normal))

private val colors = darkColorScheme(
    primary = Teal,
    onPrimary = Ink,
    primaryContainer = Color(0xFF0B3B34),
    onPrimaryContainer = Teal,
    secondary = Color(0xFF2A2A30),
    onSecondary = TextHigh,
    secondaryContainer = InkHigh,
    onSecondaryContainer = TextHigh,
    tertiary = Teal,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextHigh,
    surface = InkElevated,
    onSurface = TextHigh,
    surfaceVariant = InkHigh,
    onSurfaceVariant = TextLow,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkElevated,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHigh,
    outline = InkLine,
    outlineVariant = InkLine,
    error = Coral,
    onError = Color.White,
)

private val base = Typography()

private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Logo-Schriftbild */
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
