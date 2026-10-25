package app.turp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.turp.ui.theme.Look
import androidx.compose.material3.Icon as M3Icon

/*
 * Ersatz für material3.Icon. Im Pappe-Modus bekommen dunkle Symbole einen hellen,
 * ausgeschnittenen Sticker-Rand und einen kleinen Schatten – wie ein aufgeklebter Aufkleber.
 * Helle oder farbige Symbole (z. B. auf Fotos) bleiben unverändert.
 */

private val StickerEdge = Color(0xFFFBF4E6)

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = Icon(rememberVectorPainter(imageVector), contentDescription, modifier, tint)

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val sticker = Look.cardboard && tint.alpha > 0.5f && tint.luminance() < 0.35f
    if (!sticker) {
        M3Icon(painter, contentDescription, modifier, tint)
        return
    }
    Box(modifier, propagateMinConstraints = true) {
        // Schatten
        M3Icon(painter, null, Modifier.offset(0.6.dp, 1.8.dp), Color.Black.copy(alpha = 0.20f))
        // heller Sticker-Rand rundherum
        val r = 1.5.dp
        for ((dx, dy) in listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f, -0.7f to -0.7f, 0.7f to -0.7f, -0.7f to 0.7f, 0.7f to 0.7f)) {
            M3Icon(painter, null, Modifier.offset(r * dx, r * dy), StickerEdge)
        }
        M3Icon(painter, contentDescription, Modifier, tint)
    }
}
