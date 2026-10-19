package app.turp.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.turp.ui.theme.Teal
import com.google.firebase.Timestamp
import java.text.Collator
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun parseColor(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Teal
    }

fun Color.toHex(): String = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and toArgb())

fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

/** Heutiges Datum in Deutschland, so wie der Server es für den Check-in nutzt */
fun todayBerlin(): String = LocalDate.now(ZoneId.of("Europe/Berlin")).toString()

/** "14:32", "gestern" oder "12.10." */
fun shortTime(ts: Timestamp?): String {
    if (ts == null) return ""
    val date = ts.toDate()
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return SimpleDateFormat("HH:mm", Locale.GERMANY).format(date)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (yesterday) return "gestern"
    return SimpleDateFormat("dd.MM.", Locale.GERMANY).format(date)
}

fun timeOfDay(ts: Timestamp?): String =
    if (ts == null) "" else SimpleDateFormat("HH:mm", Locale.GERMANY).format(ts.toDate())

object Countries {
    data class Country(val code: String, val name: String)

    val all: List<Country> by lazy {
        val collator = Collator.getInstance(Locale.GERMAN)
        Locale.getISOCountries()
            .map { Country(it, name(it)) }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    fun name(code: String): String =
        if (code.length != 2) "" else Locale("", code).getDisplayCountry(Locale.GERMAN)

    fun flag(code: String): String {
        if (code.length != 2 || !code.all { it in 'A'..'Z' || it in 'a'..'z' }) return "🌍"
        val base = 0x1F1E6 - 'A'.code
        val upper = code.uppercase(Locale.ROOT)
        return String(Character.toChars(base + upper[0].code)) + String(Character.toChars(base + upper[1].code))
    }

    /** Land aus den Spracheinstellungen des Handys als Vorschlag */
    fun deviceDefault(): String {
        val c = Locale.getDefault().country
        return if (c.length == 2) c.uppercase(Locale.ROOT) else "DE"
    }
}

val ALL_INTERESTS = listOf(
    "🎮 Gaming", "🎵 Musik", "🎤 Singen", "🎸 Instrumente", "⚽ Fußball", "🏀 Basketball",
    "🏋️ Fitness", "🛹 Skaten", "🎨 Zeichnen", "📸 Fotografie", "🎬 Filme", "📺 Serien",
    "📚 Lesen", "✍️ Schreiben", "🍳 Kochen", "🐶 Tiere", "🌱 Natur", "✈️ Reisen",
    "💃 Tanzen", "👗 Mode", "💻 Coden", "🧪 Wissenschaft", "🌸 Anime", "🎲 Brettspiele",
    "🧘 Yoga", "🏊 Schwimmen", "🚴 Radfahren", "🗣️ Sprachen", "😂 Memes", "🎧 Podcasts",
)

val REPORT_REASONS = listOf(
    "Belästigung oder Mobbing",
    "Sexuelle Inhalte",
    "Falsches Alter / Fake-Profil",
    "Spam oder Betrug",
    "Gewalt oder Drohungen",
    "Sonstiges",
)
