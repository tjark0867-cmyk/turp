package app.turp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.turp.ui.theme.Look
import app.turp.ui.theme.PaperInk
import app.turp.ui.theme.cardboard
import app.turp.ui.theme.cardboardShapeFor
import androidx.compose.material3.Surface as M3Surface

/*
 * Ersatz für material3.Surface mit gleicher Bedienung. Im dunklen Modus reicht er
 * alles unverändert durch. Im Pappe-Modus werden deckende Flächen zu Pappstücken:
 * gerissener Rand, Papierstruktur, Schnittkante, leichter Schatten.
 * Halb durchsichtige Flächen (z. B. über Fotos) bleiben wie sie sind.
 */

/** Soll diese Farbe im Pappe-Modus als Pappe gezeichnet werden? */
private fun paperize(color: Color) = Look.cardboard && color.alpha > 0.9f

/** Schriftfarbe auf Pappe: helle Flächen → Braun, dunkle → Weiß */
private fun paperContent(color: Color, given: Color): Color =
    if (given == Color.White && color.luminance() > 0.45f) PaperInk else given

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    if (!paperize(color)) {
        M3Surface(
            modifier = modifier, shape = shape, color = color, contentColor = contentColor,
            tonalElevation = tonalElevation, shadowElevation = shadowElevation, border = border, content = content
        )
        return
    }
    val seed = currentCompositeKeyHash
    val paperShape = remember(shape, seed) { cardboardShapeFor(shape, seed) }
    M3Surface(
        modifier = modifier.cardboard(color, paperShape),
        shape = paperShape,
        color = Color.Transparent,
        contentColor = paperContent(color, contentColor),
        content = content,
    )
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    if (!paperize(color)) {
        M3Surface(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, color = color,
            contentColor = contentColor, tonalElevation = tonalElevation, shadowElevation = shadowElevation,
            border = border, interactionSource = interactionSource ?: remember { MutableInteractionSource() }, content = content
        )
        return
    }
    val seed = currentCompositeKeyHash
    val paperShape = remember(shape, seed) { cardboardShapeFor(shape, seed) }
    M3Surface(
        onClick = onClick,
        modifier = modifier.cardboard(color, paperShape),
        enabled = enabled,
        shape = paperShape,
        color = Color.Transparent,
        contentColor = paperContent(color, contentColor),
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        content = content,
    )
}
