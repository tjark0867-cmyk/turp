package app.turp.ui.components

import app.turp.data.t

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import app.turp.ui.components.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import java.text.Normalizer
import kotlin.math.abs

private val BY_VALUE by lazy { INTEREST_CATALOG.associateBy { it.value } }
private val BY_LABEL by lazy { INTEREST_CATALOG.associateBy { it.label } }

/** "🎮 Gaming" → "Gaming" */
fun interestLabel(interest: String): String {
    val s = interest.trim()
    // Namen aus dem Katalog in der gewählten Sprache (Eigennamen bleiben gleich)
    return t(if (s.contains(" ")) s.substringAfter(" ") else s)
}

/** Name ohne Übersetzung – für die Suche im Katalog */
fun interestLabelRaw(interest: String): String {
    val s = interest.trim()
    return if (s.contains(" ")) s.substringAfter(" ") else s
}

/** Eintrag aus dem Katalog (auch für ältere Schreibweisen) */
fun interestDef(interest: String): InterestDef? = BY_VALUE[interest] ?: BY_LABEL[interestLabelRaw(interest)]

/** Kleinbuchstaben, ohne Akzente/Umlaute und Sonderzeichen – für die Suche */
fun searchKey(text: String): String {
    val lower = text.lowercase().replace("ß", "ss")
    val plain = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return plain.filter { it.isLetterOrDigit() || it == ' ' }.trim()
}

/** Katalog nach Suchtext und Kategorie filtern */
fun findInterests(query: String, category: InterestCategory?): List<InterestDef> {
    val q = searchKey(query)
    return INTEREST_CATALOG.filter { def ->
        (category == null || def.category == category) &&
            (q.isEmpty() || searchKey(def.label).contains(q) || searchKey(t(def.label)).contains(q) || searchKey(def.words).contains(q))
    }.let { list ->
        // Treffer am Wortanfang zuerst
        if (q.isEmpty()) list else list.sortedByDescending { searchKey(it.label).startsWith(q) || searchKey(t(it.label)).startsWith(q) }
    }
}

/** Sticker zum Interesse – oder das Emoji, falls es (noch) keinen gibt */
@Composable
fun InterestIcon(interest: String, size: Dp, modifier: Modifier = Modifier) {
    val def = interestDef(interest)
    if (def != null) {
        Image(
            painter = painterResource(def.icon),
            contentDescription = t(def.label),
            modifier = modifier.size(size)
        )
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(interest.trim().substringBefore(" "), fontSize = (size.value * 0.55f).sp)
        }
    }
}

/** Kleiner Klebestreifen mit dem Namen, in der Farbe der Kategorie */
@Composable
fun InterestTape(text: String, color: Color, modifier: Modifier = Modifier, rotation: Float = 0f, fontSize: Int = 10) {
    Text(
        text = text,
        color = readableOn(color),
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 2).sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .background(color, RoundedCornerShape(3.dp))
            .border(1.dp, darken(color, 0.6f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

/** Beschriftung auf einem kleinen Pappstreifen (Kraftpapier, ausgerissene Kanten) */
@Composable
fun CardboardLabel(text: String, modifier: Modifier = Modifier, rotation: Float = 0f, fontSize: Int = 11) {
    Box(
        modifier.graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(app.turp.R.drawable.label_strip),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = text,
            color = Color(0xFF3B2410),
            fontFamily = app.turp.ui.theme.Display,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

internal fun tiltOf(key: String, range: Int = 7): Float = ((abs(key.hashCode()) % (range * 2 + 1)) - range).toFloat()

/** Auswahl-Kachel: Sticker mit Namens-Streifen; ausgewählt mit türkisem Haken */
@Composable
fun InterestPickTile(
    def: InterestDef,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 58.dp,
) {
    val tilt = tiltOf(def.value, 3)
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Teal.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(top = 4.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InterestIcon(
                def.value,
                iconSize,
                Modifier.graphicsLayer {
                    rotationZ = tilt
                    val s = if (selected) 1.06f else 1f
                    scaleX = s
                    scaleY = s
                }
            )
            InterestTape(
                text = t(def.label),
                color = def.category.color,
                rotation = -tilt / 2f,
                modifier = Modifier
                    .offset(y = (-9).dp)
                    .widthIn(max = iconSize + 16.dp)
            )
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Teal, CircleShape)
                    .border(2.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = t("Ausgewählt"), tint = Ink, modifier = Modifier.size(13.dp))
            }
        }
    }
}

/** Suchfeld für Interessen */
@Composable
fun InterestSearchField(
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFocus: (Boolean) -> Unit = {},
) {
    val focus = LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(InkHigh, RoundedCornerShape(14.dp))
            .border(1.dp, InkLine, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextLow, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, color = TextLow, fontSize = 15.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = { onQuery(it.take(40)) },
                singleLine = true,
                textStyle = TextStyle(color = TextHigh, fontSize = 15.sp),
                cursorBrush = SolidColor(Teal),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocus(it.isFocused) }
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = t("Suche leeren"),
                tint = TextLow,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onQuery("") }
            )
        }
    }
}

/** Kategorien als Chips: Alle, Games, Musik … */
@Composable
fun InterestCategoryChips(
    selected: InterestCategory?,
    onSelect: (InterestCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(t("Alle"), Teal, selected == null) { onSelect(null) }
        InterestCategory.entries.forEach { c ->
            CategoryChip(t(c.label), c.color, selected == c) { onSelect(if (selected == c) null else c) }
        }
    }
}

@Composable
private fun CategoryChip(text: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) color else InkHigh, shape)
            .border(1.dp, if (selected) darken(color, 0.6f) else InkLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = if (selected) readableOn(color) else TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Großes Sticker-Raster (für die Suche und "Profil bearbeiten") */
@Composable
fun InterestGrid(
    items: List<InterestDef>,
    selected: Collection<String>,
    onToggle: (InterestDef) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 6.dp),
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text(
                t("Dazu gibt es noch kein Interesse 🤔"),
                color = TextLow,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(78.dp),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items, key = { it.value }) { def ->
            InterestPickTile(def, def.value in selected, onClick = { onToggle(def) })
        }
    }
}

/** Zwei Reihen Sticker zum Seitwärts-Wischen */
@Composable
fun InterestRows(
    items: List<InterestDef>,
    selected: Collection<String>,
    onToggle: (InterestDef) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier.height(196.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items, key = { it.value }) { def ->
            InterestPickTile(def, def.value in selected, onClick = { onToggle(def) }, modifier = Modifier.width(80.dp))
        }
    }
}

/** Gestrichelte Plätze für die gewählten Interessen; antippen entfernt */
@Composable
fun InterestSlots(
    selected: List<String>,
    slots: Int,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(slots) { i ->
            val value = selected.getOrNull(i)
            Box(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (value == null) InkHigh.copy(alpha = 0.6f) else InkHigh)
                    .drawBehind {
                        if (value == null) {
                            drawRoundRect(
                                color = InkLine,
                                cornerRadius = CornerRadius(14.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                                )
                            )
                        }
                    }
                    .then(
                        if (value != null) {
                            Modifier.clickable(interactionSource = null, indication = null) { onRemove(value) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (value != null) {
                    InterestIcon(value, 50.dp, Modifier.graphicsLayer { rotationZ = tiltOf(value, 6) })
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(18.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = t("Entfernen"), tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                } else {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = InkLine, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Sticker-Ansicht eines gewählten Interesses mit Namen (z. B. im Profil) */
@Composable
fun InterestStickerWithTape(interest: String, size: Dp, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val def = interestDef(interest)
    val tilt = tiltOf(interest, 5)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            InterestIcon(interest, size, Modifier.graphicsLayer { rotationZ = tilt })
            if (highlight) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(Teal, CircleShape)
                        .border(2.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = t("Gemeinsam"), tint = Ink, modifier = Modifier.size(13.dp))
                }
            }
        }
        InterestTape(
            text = interestLabel(interest),
            color = if (highlight) Teal else def?.category?.color ?: Teal,
            rotation = -tilt / 2f,
            fontSize = 11,
            modifier = Modifier
                .offset(y = (-8).dp)
                .widthIn(max = size + 22.dp)
        )
    }
}
