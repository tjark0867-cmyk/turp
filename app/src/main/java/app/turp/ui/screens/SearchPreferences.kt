package app.turp.ui.screens

import app.turp.data.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import app.turp.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.data.CountryMode
import app.turp.data.DiscoverFilter
import app.turp.ui.components.Countries
import app.turp.ui.components.CountryPickerDialog
import app.turp.ui.components.InterestCategory
import app.turp.ui.components.InterestCategoryChips
import app.turp.ui.components.InterestDef
import app.turp.ui.components.InterestGrid
import app.turp.ui.components.InterestRows
import app.turp.ui.components.InterestSearchField
import app.turp.ui.components.InterestSlots
import app.turp.ui.components.PrimaryButton
import app.turp.ui.components.findInterests
import app.turp.ui.components.toast
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import kotlin.math.roundToInt

/**
 * Such-Einstellungen fürs Entdecken: bis zu 4 Interessen, Land, Alter.
 * Wird unter der Kopfzeile statt der Karten angezeigt.
 */
@Composable
fun SearchPreferences(
    initial: DiscoverFilter,
    bounds: IntRange,
    myCountry: String,
    onClose: () -> Unit,
    onSave: (DiscoverFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var interests by remember { mutableStateOf(initial.interests) }
    var mode by remember { mutableStateOf(initial.countryMode) }
    var other by remember { mutableStateOf(initial.otherCountry) }
    var range by remember { mutableStateOf(initial.minAge.toFloat()..initial.maxAge.toFloat()) }
    var sharedFirst by remember { mutableStateOf(initial.sharedFirst) }
    var gender by remember { mutableStateOf(initial.gender) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<InterestCategory?>(null) }
    var searchFocused by remember { mutableStateOf(false) }
    var pickCountry by remember { mutableStateOf(false) }

    val searching = searchFocused || query.isNotEmpty()
    val items = remember(query, category) { findInterests(query, category) }
    val max = DiscoverFilter.MAX_SEARCH_INTERESTS

    fun current() = DiscoverFilter(
        interests = interests,
        countryMode = if (mode == CountryMode.OTHER && other.isBlank()) CountryMode.WORLD else mode,
        otherCountry = other,
        minAge = range.start.roundToInt(),
        maxAge = range.endInclusive.roundToInt(),
        sharedFirst = sharedFirst,
        gender = gender,
    )

    fun toggle(def: InterestDef) {
        interests = when {
            def.value in interests -> interests - def.value
            interests.size >= max -> {
                context.toast(t("Höchstens {0} – tippe oben eins an, um es zu entfernen.", max))
                interests
            }
            else -> interests + def.value
        }
    }

    fun closeSearch() {
        query = ""
        focus.clearFocus()
    }

    BackHandler(enabled = searching) { closeSearch() }
    BackHandler(enabled = !searching) { onClose() }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = t("Schließen"),
                    tint = TextHigh,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                t("Such-Einstellungen"),
                color = TextHigh,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            if (current() != DiscoverFilter.defaults(bounds)) {
                TextButton(onClick = {
                    val d = DiscoverFilter.defaults(bounds)
                    interests = d.interests
                    mode = d.countryMode
                    other = ""
                    range = d.minAge.toFloat()..d.maxAge.toFloat()
                    sharedFirst = d.sharedFirst
                    gender = d.gender
                }) { Text(t("Zurücksetzen"), color = TextLow, fontSize = 13.sp) }
            }
        }
        Text(
            t("Freunde finden, die das mögen"),
            color = TextLow,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        InterestSlots(selected = interests, slots = max, onRemove = { interests = interests - it })
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InterestSearchField(
                query = query,
                onQuery = { query = it },
                placeholder = t("Bis zu {0} Interessen wählen", max),
                onFocus = { searchFocused = it },
                modifier = Modifier.weight(1f)
            )
            if (searching) {
                TextButton(onClick = ::closeSearch) { Text(t("Fertig"), color = Teal, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(10.dp))
        InterestCategoryChips(selected = category, onSelect = { category = it })
        Spacer(Modifier.height(4.dp))

        if (searching) {
            InterestGrid(
                items = items,
                selected = interests,
                onToggle = ::toggle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
            )
        } else {

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                InterestRows(items = items, selected = interests, onToggle = ::toggle)
                Spacer(Modifier.height(14.dp))

                PrefLabel(t("Zeig mir"))
                val adults = bounds.first >= 18
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionPill(t("👥 Alle"), gender.isEmpty(), Modifier.weight(1f)) { gender = "" }
                    OptionPill(if (adults) t("🙋‍♂️ Männer") else t("🙋‍♂️ Jungs"), gender == "male", Modifier.weight(1f)) { gender = "male" }
                    OptionPill(if (adults) t("🙋‍♀️ Frauen") else t("🙋‍♀️ Mädchen"), gender == "female", Modifier.weight(1f)) { gender = "female" }
                }
                Spacer(Modifier.height(18.dp))

                PrefLabel(t("Die wohnen in"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionPill(
                        t("{0} Mein Land", Countries.flag(myCountry)),
                        mode == CountryMode.MINE,
                        Modifier.weight(1f)
                    ) { mode = CountryMode.MINE }
                    OptionPill(t("🌍 Weltweit"), mode == CountryMode.WORLD, Modifier.weight(1f)) { mode = CountryMode.WORLD }
                    OptionPill(
                        if (other.isNotBlank()) "${Countries.flag(other)} ${Countries.name(other)}" else t("🧭 Anderes"),
                        mode == CountryMode.OTHER,
                        Modifier.weight(1f)
                    ) { pickCountry = true }
                }
                Spacer(Modifier.height(18.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PrefLabel(t("Alter"), Modifier.weight(1f))
                    val hi = range.endInclusive.roundToInt()
                    Text(
                        "${range.start.roundToInt()} – ${if (hi >= 80) "80+" else "${hi}"}",
                        color = TextHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = bounds.first.toFloat()..bounds.last.toFloat(),
                    steps = (bounds.last - bounds.first - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = Teal,
                        activeTrackColor = Teal,
                        inactiveTrackColor = InkHigh,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                if (bounds.first == 13) {
                    Text(
                        t("Bei kumi siehst du nur Leute zwischen 13 und 17 – Erwachsene sind getrennt."),
                        color = TextLow,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InkHigh)
                        .clickable { sharedFirst = !sharedFirst }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t("🤝 Gemeinsamkeiten zuerst"), color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            t("Leute mit denselben Interessen wie du kommen zuerst"),
                            color = TextLow,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = sharedFirst,
                        onCheckedChange = { sharedFirst = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Ink)
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            PrimaryButton(text = t("Speichern"), onClick = { onSave(current()) })
            Spacer(Modifier.height(4.dp))
        }
    }

    if (pickCountry) {
        CountryPickerDialog(
            allowAll = false,
            onDismiss = { pickCountry = false },
            onPick = { code ->
                pickCountry = false
                if (code != null) {
                    other = code
                    mode = CountryMode.OTHER
                }
            }
        )
    }
}

@Composable
private fun PrefLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextHigh,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun OptionPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        color = if (selected) Teal else TextHigh,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .background(if (selected) Teal.copy(alpha = 0.14f) else InkHigh, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Teal else InkLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    )
}
