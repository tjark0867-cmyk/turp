package app.turp.ui.components

import app.turp.data.t

import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import app.turp.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.turp.ui.theme.Coral
import androidx.compose.runtime.currentCompositeKeyHash
import app.turp.ui.theme.cardboard
import app.turp.ui.theme.TornCircleShape
import app.turp.ui.theme.TornShape
import app.turp.ui.theme.PaperInk
import app.turp.ui.theme.Look
import app.turp.ui.theme.Display
import app.turp.ui.theme.Online
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TealDark
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage

/** Seitenverhältnis des Logo-Bildes (Breite / Höhe) */
private const val LogoAspect = 1280f / 472f

/**
 * Schriftzug "kumi" im Magazin-Schnipsel-Look: jeder Buchstabe ein eigenes
 * Papierstück. [fontSize] bestimmt die Größe wie bei einem Text.
 */
@Composable
fun TurpLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 56.sp) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = "kumi",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height((fontSize.value * 1.2f).dp)
            .aspectRatio(LogoAspect)
    )
}

/** Gut lesbare Schriftfarbe auf einer Hintergrundfarbe */
fun readableOn(background: Color): Color =
    if (background.alpha > 0.6f && background.luminance() > 0.45f) {
        if (Look.cardboard) PaperInk else Color(0xFF0B0B0F)
    } else {
        Color.White
    }

/** Dunklere Variante einer Farbe */
fun darken(color: Color, factor: Float = 0.7f): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

/** Namensschild in der Profilfarbe, leicht schräg */
@Composable
fun Sticker(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    rotation: Float = -3f,
    fontSize: TextUnit = 22.sp,
    textColor: Color = readableOn(background),
) {
    val shape = RoundedCornerShape(9.dp)
    Text(
        text = text,
        style = TextStyle(fontFamily = Display, fontSize = fontSize),
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .then(
                if (Look.cardboard && background.alpha > 0.9f) {
                    // Pappe: ausgerissenes Schild mit dunklerer Schnittkante
                    val seed = currentCompositeKeyHash
                    Modifier.cardboard(background, remember(seed) { TornShape(seed, jag = 1.4.dp, step = 5.dp) }, edge = darken(background, 0.6f))
                } else {
                    Modifier
                        .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                        .background(background, shape)
                        .border(1.5.dp, darken(background, 0.55f), shape)
                }
            )
            .padding(horizontal = 10.dp, vertical = 1.dp)
    )
}

/** Flacher, runder Button mit leichtem Drück-Effekt */
@Composable
fun CandyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = Teal,
    contentColor: Color = readableOn(color),
    height: Dp = 50.dp,
    fontSize: TextUnit = 16.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "buttonScale")
    val active = enabled && !loading
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled || loading) 1f else 0.4f
            }
            .height(height)
            .then(
                if (Look.cardboard && color.alpha > 0.9f) {
                    val seed = currentCompositeKeyHash
                    Modifier.cardboard(color, remember(seed) { TornShape(seed, jag = 2.2.dp, step = 8.dp) })
                } else {
                    Modifier.clip(shape).background(color)
                }
            )
            .clickable(interactionSource = interaction, indication = null, enabled = active, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = contentColor, strokeWidth = 2.dp)
        } else {
            if (Look.cardboard) {
                // Pappe: Schrift wie auf den Schildern
                Text(text, color = contentColor, fontSize = fontSize * 1.08f, fontFamily = Display, maxLines = 1)
            } else {
                Text(text, color = contentColor, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** Runder, flacher Knopf mit Symbol */
@Composable
fun RoundCandyButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = readableOn(color),
    borderColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "roundScale")
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (Look.cardboard && color.alpha > 0.9f) {
                    val seed = currentCompositeKeyHash
                    Modifier.cardboard(color, remember(seed) { TornCircleShape(seed) })
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(color)
                        .then(if (borderColor != null) Modifier.border(1.5.dp, borderColor, CircleShape) else Modifier)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconColor, modifier = Modifier.size(size * 0.46f))
    }
}

/**
 * 3D-Diamant im Brillantschliff (Türkis). Wird als echtes 3D-Modell gezeichnet:
 * Tafel, Krone, Rundiste und Pavillon, beleuchtet von zwei Lichtern, mit
 * Glanzpunkten, Farbspiel und gelegentlichem Funkeln. Mit [spin] dreht er sich.
 */
@Composable
fun GemIcon(iconSize: Dp, modifier: Modifier = Modifier, spin: Boolean = false) {
    val path = remember { Path() }
    if (spin) {
        val transition = rememberInfiniteTransition(label = "gem")
        val angle = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 4200, easing = LinearEasing)),
            label = "gemSpin"
        )
        Canvas(modifier.size(iconSize)) { drawGem(angle.value, path, twinkle = true) }
    } else {
        Canvas(modifier.size(iconSize)) { drawGem(20f, path, twinkle = false) }
    }
}

private object GemModel {
    val vertices: Array<FloatArray>
    val faces: Array<IntArray>
    val flip: FloatArray
    val azimuth: FloatArray
    val tiltCos = cos(Math.toRadians(24.0)).toFloat()
    val tiltSin = sin(Math.toRadians(24.0)).toFloat()
    val light1 = normalize3(-0.45f, 0.75f, 0.5f)
    val light2 = normalize3(0.6f, 0.35f, 0.72f)

    init {
        // Eckpunkte: 0–7 Tafel, 8–23 Rundiste, 24 Spitze (Kalette)
        val v = ArrayList<FloatArray>()
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            v.add(floatArrayOf((0.56 * cos(a)).toFloat(), 0.36f, (0.56 * sin(a)).toFloat()))
        }
        for (j in 0 until 16) {
            val a = Math.toRadians(j * 22.5)
            v.add(floatArrayOf(cos(a).toFloat(), 0f, sin(a).toFloat()))
        }
        v.add(floatArrayOf(0f, -0.92f, 0f))
        vertices = v.toTypedArray()

        val f = ArrayList<IntArray>()
        f.add(IntArray(8) { it })
        for (i in 0 until 8) {
            val t0 = i
            val t1 = (i + 1) % 8
            val g0 = 8 + 2 * i
            val g1 = 8 + (2 * i + 1) % 16
            val g2 = 8 + (2 * i + 2) % 16
            f.add(intArrayOf(t0, g0, g1))
            f.add(intArrayOf(t0, g1, t1))
            f.add(intArrayOf(t1, g1, g2))
        }
        for (j in 0 until 16) f.add(intArrayOf(8 + j, 8 + (j + 1) % 16, 24))
        faces = f.toTypedArray()

        // Flächen so ausrichten, dass ihre Normale nach außen zeigt
        flip = FloatArray(faces.size)
        azimuth = FloatArray(faces.size)
        for (k in faces.indices) {
            val face = faces[k]
            val a = vertices[face[0]]
            val b = vertices[face[1]]
            val c = vertices[face[2]]
            val ux = b[0] - a[0]
            val uy = b[1] - a[1]
            val uz = b[2] - a[2]
            val wx = c[0] - a[0]
            val wy = c[1] - a[1]
            val wz = c[2] - a[2]
            val nx = uy * wz - uz * wy
            val ny = uz * wx - ux * wz
            val nz = ux * wy - uy * wx
            var mx = 0f
            var my = 0f
            var mz = 0f
            for (idx in face) {
                mx += vertices[idx][0]
                my += vertices[idx][1]
                mz += vertices[idx][2]
            }
            mx /= face.size
            my /= face.size
            mz /= face.size
            val d = nx * mx + ny * (my + 0.2f) + nz * mz
            flip[k] = if (d < 0f) -1f else 1f
            azimuth[k] = atan2(mz, mx)
        }
    }
}

private fun normalize3(x: Float, y: Float, z: Float): FloatArray {
    val l = sqrt(x * x + y * y + z * z)
    return floatArrayOf(x / l, y / l, z / l)
}

private val GemDark = Color(0xFF003831)
private val GemMid = Color(0xFF10BCA0)
private val GemLight = Color(0xFFCCFFF7)

private fun gemShade(t: Float): Color =
    if (t < 0.5f) lerp(GemDark, GemMid, (t * 2f).coerceIn(0f, 1f))
    else lerp(GemMid, GemLight, ((t - 0.5f) * 2f).coerceIn(0f, 1f))

private fun specular(nx: Float, ny: Float, nz: Float, l: FloatArray, power: Int): Float {
    val d = nx * l[0] + ny * l[1] + nz * l[2]
    if (d <= 0f) return 0f
    val rz = 2f * d * nz - l[2]
    return if (rz <= 0f) 0f else rz.pow(power)
}

private fun DrawScope.drawGem(angleDeg: Float, path: Path, twinkle: Boolean) {
    val w = size.width
    val h = size.height
    val th = Math.toRadians(angleDeg.toDouble()).toFloat()
    val ct = cos(th)
    val st = sin(th)
    val ca = GemModel.tiltCos
    val sa = GemModel.tiltSin
    val scale = min(w / 2f, h / 1.40f) * 0.92f
    val cx = w / 2f
    val cy = h / 2f - 0.142f * scale

    val vs = GemModel.vertices
    val n = vs.size
    val rx = FloatArray(n)
    val ry = FloatArray(n)
    val rz = FloatArray(n)
    val px = FloatArray(n)
    val py = FloatArray(n)
    for (i in 0 until n) {
        val v = vs[i]
        val x1 = v[0] * ct + v[2] * st
        val z1 = -v[0] * st + v[2] * ct
        val y2 = v[1] * ca - z1 * sa
        val z2 = v[1] * sa + z1 * ca
        rx[i] = x1
        ry[i] = y2
        rz[i] = z2
        px[i] = cx + x1 * scale
        py[i] = cy - y2 * scale
    }

    val stroke = Stroke(width = max(0.6f, size.minDimension * 0.004f), join = StrokeJoin.Round)
    val l1 = GemModel.light1
    val l2 = GemModel.light2
    for (k in GemModel.faces.indices) {
        val face = GemModel.faces[k]
        val a = face[0]
        val b = face[1]
        val c = face[2]
        val ux = rx[b] - rx[a]
        val uy = ry[b] - ry[a]
        val uz = rz[b] - rz[a]
        val wx = rx[c] - rx[a]
        val wy = ry[c] - ry[a]
        val wz = rz[c] - rz[a]
        val f = GemModel.flip[k]
        var nx = (uy * wz - uz * wy) * f
        var ny = (uz * wx - ux * wz) * f
        var nz = (ux * wy - uy * wx) * f
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len < 1e-6f) continue
        nx /= len
        ny /= len
        nz /= len
        if (nz <= 0.02f) continue // Rückseite, nicht sichtbar

        val diffuse = max(0f, nx * l1[0] + ny * l1[1] + nz * l1[2])
        val fire = 0.5f + 0.5f * sin(3f * GemModel.azimuth[k] + 2f * th)
        val t = when {
            k == 0 -> 0.50f + 0.22f * diffuse                      // Tafel
            k <= 24 -> 0.20f + 0.50f * diffuse + 0.18f * fire     // Krone
            else -> 0.10f + 0.58f * fire + 0.10f * diffuse        // Pavillon
        }
        val shine = (specular(nx, ny, nz, l1, 24) * 0.75f + specular(nx, ny, nz, l2, 60)).coerceIn(0f, 1f)
        val color = lerp(gemShade(t), Color.White, shine)

        path.reset()
        path.moveTo(px[face[0]], py[face[0]])
        for (m in 1 until face.size) path.lineTo(px[face[m]], py[face[m]])
        path.close()
        drawPath(path, color)
        drawPath(path, lerp(color, Color.White, 0.35f), style = stroke)
    }

    // Spiegelungen in der Tafel
    val tcy = cy - 0.36f * ca * scale
    for (i in 0 until 8) {
        val j = (i + 1) % 8
        val mx = (px[i] + px[j]) / 2f
        val my = (py[i] + py[j]) / 2f
        val ix = cx + (mx - cx) * 0.55f
        val iy = tcy + (my - tcy) * 0.55f
        val fire = 0.5f + 0.5f * sin(3f * Math.toRadians(i * 45.0 + 22.5).toFloat() + 2f * th + 1.3f)
        val color = lerp(gemShade(0.45f + 0.3f * fire), Color.White, 0.15f * fire)
        path.reset()
        path.moveTo(px[i], py[i])
        path.lineTo(px[j], py[j])
        path.lineTo(ix, iy)
        path.close()
        drawPath(path, color)
        drawPath(path, lerp(color, Color.White, 0.3f), style = stroke)
    }

    // Funkeln
    if (twinkle) {
        val tw = ((sin(3f * th) + 1f) / 2f).pow(8)
        if (tw > 0.02f) {
            val sx = cx - 0.34f * scale
            val sy = cy - 0.52f * scale
            val r = scale * 0.30f * tw
            path.reset()
            path.moveTo(sx, sy - r)
            path.lineTo(sx + r * 0.16f, sy - r * 0.16f)
            path.lineTo(sx + r, sy)
            path.lineTo(sx + r * 0.16f, sy + r * 0.16f)
            path.lineTo(sx, sy + r)
            path.lineTo(sx - r * 0.16f, sy + r * 0.16f)
            path.lineTo(sx - r, sy)
            path.lineTo(sx - r * 0.16f, sy - r * 0.16f)
            path.close()
            drawPath(path, Color.White)
        }
    }
}

/** Gem-Anzeige oben rechts; roter Punkt = Check-in heute noch offen */
@Composable
fun GemPill(gems: Long, showDot: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GemIcon(26.dp, spin = true)
            Spacer(Modifier.width(6.dp))
            Text("${gems}", color = TextHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .background(Coral, CircleShape)
                    .border(2.dp, Ink, CircleShape)
            )
        }
    }
}

@Composable
fun Avatar(
    url: String,
    name: String,
    color: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Color? = null,
    online: Boolean = false,
) {
    val accent = parseColor(color)
    Box(modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center
        ) {
            if (url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = readableOn(accent),
                    style = TextStyle(fontFamily = Display, fontSize = (size.value * 0.45f).sp)
                )
            }
            if (ring != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(2.5.dp, ring, CircleShape)
                )
            }
        }
        if (online) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(size * 0.28f)
                    .background(Online, CircleShape)
                    .border(2.dp, Ink, CircleShape)
            )
        }
    }
}

@Composable
fun GemChip(gems: Long, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = InkHigh, modifier = modifier) {
        Text(
            text = "💎 ${gems}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InterestSticker(text: String, @Suppress("UNUSED_PARAMETER") accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = TextHigh
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextLow)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextLow, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel, color = TextLow) }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Teal)
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = InkElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextLow,
        modifier = modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = Teal,
) {
    CandyButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        loading = loading,
        color = color,
        height = 56.dp,
    )
}

/** Eingabezeile für Chat und Lounge */
@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = t("Nachricht schreiben…"),
    enabled: Boolean = true,
    sendHint: String? = null,
    onPickImage: (() -> Unit)? = null,
    imageBusy: Boolean = false,
    onCamera: (() -> Unit)? = null,
    onMic: (() -> Unit)? = null,
) {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCamera != null) {
                IconButton(onClick = onCamera, enabled = enabled && !imageBusy) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = t("Foto aufnehmen"), tint = Teal)
                }
            }
            if (onPickImage != null) {
                IconButton(onClick = onPickImage, enabled = enabled && !imageBusy) {
                    if (imageBusy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Teal)
                    } else {
                        Icon(Icons.Rounded.Image, contentDescription = t("Foto senden"), tint = Teal)
                    }
                }
            }
            TextField(
                value = text,
                onValueChange = { text = it.take(1000) },
                placeholder = { Text(placeholder) },
                enabled = enabled,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = InkHigh,
                    unfocusedContainerColor = InkHigh,
                    disabledContainerColor = InkHigh,
                ),
                modifier = Modifier.weight(1f)
            )
            if (sendHint != null) {
                Text(
                    sendHint,
                    color = TextLow,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            } else if (onMic != null && text.isBlank()) {
                IconButton(onClick = onMic, enabled = enabled) {
                    Icon(Icons.Rounded.Mic, contentDescription = t("Sprachnachricht aufnehmen"), tint = Teal)
                }
            } else {
                val canSend = enabled && text.isNotBlank()
                IconButton(
                    onClick = {
                        onSend(text.trim())
                        text = ""
                    },
                    enabled = canSend
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = t("Senden"),
                        tint = if (canSend) Teal else TextLow
                    )
                }
            }
        }
    }
}

/** Auswahl des Landes mit Suche */
@Composable
fun CountryPickerDialog(
    allowAll: Boolean,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        Countries.all.filter { it.name.contains(query, ignoreCase = true) || it.code.equals(query, ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = InkElevated) {
            Column(Modifier.padding(20.dp)) {
                Text(t("Land wählen"), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(t("Suchen…")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (allowAll && query.isBlank()) {
                        item { CountryRow("🌍", t("Alle Länder")) { onPick(null) } }
                    }
                    items(list, key = { it.code }) { c ->
                        CountryRow(Countries.flag(c.code), c.name) { onPick(c.code) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("Schließen")) }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(flag: String, name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(name)
    }
}

/** Profilfarbe: Vorschläge plus frei wählbarer Farbton */
@Composable
fun ColorPicker(selected: String, onSelect: (String) -> Unit) {
    val presets = listOf("#2DE1C2", "#FF5C8A", "#FFC93C", "#9B8CFF", "#4DA3FF", "#FF8A3D", "#7CE05A", "#F05BEB")
    var hue by remember { mutableFloatStateOf(hueOf(parseColor(selected))) }
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { hex ->
                val isSelected = hex.equals(selected, ignoreCase = true)
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(parseColor(hex))
                        .then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                        .clickable {
                            onSelect(hex)
                            hue = hueOf(parseColor(hex))
                        }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(t("Eigener Farbton"), style = MaterialTheme.typography.labelMedium, color = TextLow)
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient((0..6).map { Color.hsv((it * 60f) % 360f, 0.65f, 0.95f) }))
            )
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    onSelect(Color.hsv(it, 0.65f, 0.95f).toHex())
                },
                valueRange = 0f..359f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.hsv(hue.coerceIn(0f, 359f), 0.65f, 0.95f),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

/** Dialog zum Melden einer Person */
@Composable
fun ReportDialog(name: String, onDismiss: () -> Unit, onSubmit: (reason: String, details: String) -> Unit) {
    var reason by remember { mutableStateOf(REPORT_REASONS.first()) }
    var details by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("{0} melden", name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                REPORT_REASONS.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { reason = r }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(t(r))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text(t("Was ist passiert? (optional)")) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    t("Deine Meldung ist anonym. Bei Gefahr wende dich bitte auch an eine erwachsene Vertrauensperson oder die Polizei (110)."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason, details.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text(t("Melden")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Abbrechen")) } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (danger) Coral else Teal, contentColor = Ink)
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Abbrechen")) } }
    )
}

/** Profilvideo: stumm, in Dauerschleife, mit Steuerung */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = modifier.clip(RoundedCornerShape(20.dp))
    )
}
