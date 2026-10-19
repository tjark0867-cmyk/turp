package app.turp.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
import app.turp.ui.theme.Display
import app.turp.ui.theme.Online
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.LogoStyle
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage

/** Schriftzug "turp" im Sticker-Look: Farbverlauf, weiße Kontur, harter Schatten */
@Composable
fun TurpLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 56.sp) {
    val style = LogoStyle.copy(fontSize = fontSize)
    val o = (fontSize.value * 0.045f).dp
    Box(modifier) {
        Text("turp", style = style, color = Color.Black, modifier = Modifier.offset(x = o, y = o * 2.4f))
        listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to 1, -1 to 1, 1 to -1).forEach { (dx, dy) ->
            Text("turp", style = style, color = Color.White, modifier = Modifier.offset(x = o * dx, y = o * dy))
        }
        Text(
            text = "turp",
            style = style,
            color = Color.White,
            modifier = Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    val brush = Brush.linearGradient(listOf(Teal, Lilac, Coral))
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
                    }
                }
        )
    }
}

/** Gut lesbare Schriftfarbe auf einer Hintergrundfarbe */
fun readableOn(background: Color): Color = if (background.luminance() > 0.5f) Ink else Color.White

/** Etwas dunklere Variante einer Farbe (für 3D-Kanten) */
fun darken(color: Color, factor: Float = 0.7f): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

/** Schräg aufgeklebtes Namensschild mit weißem Rand und hartem Schatten */
@Composable
fun Sticker(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    rotation: Float = -4f,
    fontSize: TextUnit = 22.sp,
    textColor: Color = readableOn(background),
) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = text,
        style = TextStyle(fontFamily = Display, fontSize = fontSize),
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .drawBehind {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .background(background, shape)
            .border(2.5.dp, Color.White, shape)
            .padding(horizontal = 12.dp, vertical = 3.dp)
    )
}

/** Dicker Bonbon-Button mit 3D-Kante, der beim Drücken einsinkt */
@Composable
fun CandyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = Teal,
    contentColor: Color = readableOn(color),
    height: Dp = 52.dp,
    fontSize: TextUnit = 19.sp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val face = if (active) color else color.copy(alpha = 0.45f)
    val depth = 5.dp
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clickable(interactionSource = interaction, indication = null, enabled = active, onClick = onClick),
        propagateMinConstraints = true
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = depth)
                .background(darken(face), shape)
        )
        Box(
            Modifier
                .padding(top = if (pressed) depth else 0.dp, bottom = if (pressed) 0.dp else depth)
                .height(height)
                .background(face, shape)
                .border(2.dp, Color.White.copy(alpha = 0.28f), shape)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = contentColor, strokeWidth = 2.5.dp)
            } else {
                Text(text, style = TextStyle(fontFamily = Display, fontSize = fontSize), color = contentColor, maxLines = 1)
            }
        }
    }
}

/** Runder Bonbon-Knopf mit Symbol */
@Composable
fun RoundCandyButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = readableOn(color),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 4.dp
    Box(
        modifier
            .size(size + depth)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .size(size)
                .align(Alignment.BottomCenter)
                .background(darken(color), CircleShape)
        )
        Box(
            Modifier
                .padding(top = if (pressed) depth else 0.dp)
                .size(size)
                .background(color, CircleShape)
                .border(2.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = iconColor, modifier = Modifier.size(size * 0.48f))
        }
    }
}

/** Gem-Anzeige oben rechts; roter Punkt = Check-in heute noch offen */
@Composable
fun GemPill(gems: Long, showDot: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        Row(
            Modifier
                .background(InkHigh, CircleShape)
                .border(2.dp, Sun.copy(alpha = 0.7f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💎", fontSize = 18.sp)
            Spacer(Modifier.width(5.dp))
            Text("$gems", style = TextStyle(fontFamily = Display, fontSize = 20.sp), color = TextHigh)
        }
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(13.dp)
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
            text = "💎 $gems",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InterestSticker(text: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextLow, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
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
    Surface(shape = RoundedCornerShape(24.dp), color = InkElevated, modifier = modifier.fillMaxWidth()) {
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
    placeholder: String = "Nachricht schreiben…",
    enabled: Boolean = true,
    sendHint: String? = null,
    onPickImage: (() -> Unit)? = null,
    imageBusy: Boolean = false,
) {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPickImage != null) {
                IconButton(onClick = onPickImage, enabled = enabled && !imageBusy) {
                    if (imageBusy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Teal)
                    } else {
                        Icon(Icons.Rounded.Image, contentDescription = "Foto senden", tint = Teal)
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
                        contentDescription = "Senden",
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
                Text("Land wählen", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Suchen…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (allowAll && query.isBlank()) {
                        item { CountryRow("🌍", "Alle Länder") { onPick(null) } }
                    }
                    items(list, key = { it.code }) { c ->
                        CountryRow(Countries.flag(c.code), c.name) { onPick(c.code) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Schließen") }
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
        Text("Eigener Farbton", style = MaterialTheme.typography.labelMedium, color = TextLow)
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
        title = { Text("$name melden") },
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
                        Text(r)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text("Was ist passiert? (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deine Meldung ist anonym. Bei Gefahr wende dich bitte auch an eine erwachsene Vertrauensperson oder die Polizei (110).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason, details.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) { Text("Melden") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
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
