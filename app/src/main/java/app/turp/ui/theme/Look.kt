package app.turp.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.turp.R
import kotlin.random.Random

/**
 * Aussehen der App: dunkel (Standard) oder Pappe. Wird in den Einstellungen gewählt
 * und gespeichert; beim Wechsel wird die Activity neu aufgebaut.
 */
object Look {
    private const val PREFS = "look"
    private const val KEY = "cardboard"

    var cardboard by mutableStateOf(false)
        private set

    fun init(context: Context) {
        cardboard = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    fun set(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).commit()
        cardboard = on
    }
}

// Farben im Pappe-Modus
val PaperInk = Color(0xFF2B1A0C)        // Filzstift-Braun für Schrift und Symbole
val PaperInkLow = Color(0xFF6B4B2C)
val PaperLight = Color(0xFFEAD3AC)      // helle Pappe für Karten/Flächen
val PaperMid = Color(0xFFDDBD8E)        // etwas dunkler: Chips, Eingabefelder
val PaperEdge = Color(0xFFA9773F)       // Schnittkante
val PaperBar = Color(0xFFB98447)        // Leisten oben/unten (dunklere Pappe)

/**
 * Form mit gerissenem Rand. Immer gleich für denselben [seed], damit nichts beim
 * Neuzeichnen „zittert“. Kanten lassen sich einzeln glatt lassen (z. B. Bildschirmrand).
 */
class TornShape(
    private val seed: Int = 7,
    private val jag: Dp = 1.8.dp,
    private val step: Dp = 7.dp,
    private val top: Boolean = true,
    private val right: Boolean = true,
    private val bottom: Boolean = true,
    private val left: Boolean = true,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val j = with(density) { jag.toPx() }
        val st = with(density) { step.toPx() }.coerceAtLeast(2f)
        val r = Random(seed)
        fun off() = (r.nextFloat() - 0.5f) * 2f * j
        val w = size.width
        val h = size.height
        val path = Path()
        // oben: links -> rechts
        path.moveTo(0f + if (left) j else 0f, if (top) j + off() * 0.6f else 0f)
        var x = st
        while (x < w - st * 0.5f) {
            path.lineTo(x, if (top) j + off() else 0f); x += st
        }
        path.lineTo(w - if (right) j else 0f, if (top) j + off() * 0.6f else 0f)
        // rechts: oben -> unten
        var y = st
        while (y < h - st * 0.5f) {
            path.lineTo(if (right) w - j + off() else w, y); y += st
        }
        path.lineTo(if (right) w - j + off() * 0.6f else w, h - if (bottom) j else 0f)
        // unten: rechts -> links
        x = w - st
        while (x > st * 0.5f) {
            path.lineTo(x, if (bottom) h - j + off() else h); x -= st
        }
        path.lineTo(0f + if (left) j else 0f, if (bottom) h - j + off() * 0.6f else h)
        // links: unten -> oben
        y = h - st
        while (y > st * 0.5f) {
            path.lineTo(if (left) j + off() else 0f, y); y -= st
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Kreis mit leicht ausgefranstem Rand */
class TornCircleShape(private val seed: Int = 3, private val jag: Dp = 1.4.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val j = with(density) { jag.toPx() }
        val r = Random(seed)
        val cx = size.width / 2
        val cy = size.height / 2
        val rx = cx - j
        val ry = cy - j
        val n = 48
        val path = Path()
        for (i in 0 until n) {
            val a = (2 * Math.PI * i / n).toFloat()
            val d = 1f + (r.nextFloat() - 0.5f) * 2f * j / maxOf(rx, 1f)
            val px = cx + rx * d * kotlin.math.cos(a)
            val py = cy + ry * d * kotlin.math.sin(a)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Papier-Struktur als kachelbarer Pinsel */
@Composable
fun rememberPaperBrush(): ShaderBrush {
    val tile: ImageBitmap = ImageBitmap.imageResource(R.drawable.paper_tile)
    return remember(tile) { ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)) }
}

/**
 * Zeichnet eine Pappe-Fläche hinter den Inhalt: weicher Schatten, Farbe, Papierstruktur
 * und eine dünne, dunklere Schnittkante – alles in der Form [shape].
 */
@Composable
fun Modifier.cardboard(
    color: Color,
    shape: Shape = TornShape(),
    edge: Color? = PaperEdge,
    shadow: Boolean = true,
    texture: Float = 1f,
): Modifier {
    val brush = rememberPaperBrush()
    return this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val sh = 2.dp.toPx()
        val stroke = Stroke(width = 1.3.dp.toPx())
        onDrawBehind {
            if (shadow && color.alpha > 0f) {
                translate(0f, sh) { drawOutline(outline, Color.Black.copy(alpha = 0.16f)) }
                translate(0f, sh * 0.5f) { drawOutline(outline, Color.Black.copy(alpha = 0.10f)) }
            }
            drawOutline(outline, color)
            if (texture > 0f && color.alpha > 0f) drawOutline(outline, brush, alpha = texture * color.alpha)
            if (edge != null && color.alpha > 0f) drawOutline(outline, edge.copy(alpha = edge.alpha * color.alpha), style = stroke)
        }
    }
}

/**
 * Passende Pappe-Form zu einer vorhandenen Form: runde Knöpfe bleiben (leicht ausgefranst) rund,
 * Pillen und Rechtecke werden zu gerissenen Papierstücken.
 */
fun cardboardShapeFor(shape: Shape, seed: Int): Shape =
    if (shape == CircleShape) AdaptiveTornShape(seed) else TornShape(seed)

/** Quadratisch → ausgefranster Kreis, sonst gerissenes Rechteck */
class AdaptiveTornShape(private val seed: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val square = kotlin.math.abs(size.width - size.height) < 0.15f * maxOf(size.width, size.height)
        return if (square) TornCircleShape(seed).createOutline(size, layoutDirection, density)
        else TornShape(seed).createOutline(size, layoutDirection, density)
    }
}
