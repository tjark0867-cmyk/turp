package app.turp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.R
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Display
import app.turp.ui.theme.PaperInk

/** Gibt beim Drücken nach wie ein Stück Pappe (ohne Material-Welle) */
@Composable
fun Modifier.paperPress(label: String, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .semantics {
            contentDescription = label
            role = Role.Button
        }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Knopf aus einem fertigen Pappe-Bild (z. B. Filter, Fähnchen) */
@Composable
fun PaperImageButton(res: Int, label: String, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, rotation: Float = 0f) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .paperPress(label, onClick)
    )
}

/** Runde Pappscheibe mit einem Bild/Symbol darauf */
@Composable
fun CardboardDiscButton(
    onClick: () -> Unit,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .size(size)
            .paperPress(label, onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.disc_cream),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}

/** Preis-Schildchen aus Pappe: 💎 + Zahl */
@Composable
fun GemPriceTag(cost: Long, modifier: Modifier = Modifier, rotation: Float = 8f, gemSize: Dp = 13.dp, fontSize: Int = 14) {
    Box(modifier.graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.label_strip),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GemIcon(gemSize)
            Spacer(Modifier.width(4.dp))
            Text("$cost", fontFamily = Display, color = PaperInk, fontSize = fontSize.sp)
        }
    }
}

/** Gems oben rechts im Pappe-Modus: Schildchen mit Zahl, roter Punkt wenn die Tagesbelohnung wartet */
@Composable
fun PaperGemPill(gems: Long, showDot: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        GemPriceTag(gems, rotation = -3f, gemSize = 22.dp, fontSize = 19, modifier = Modifier.padding(4.dp))
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(Coral, CircleShape)
                    .border(2.dp, PaperInk, CircleShape)
            )
        }
    }
}
