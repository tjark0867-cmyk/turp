package app.turp.ui.components

import app.turp.data.t

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
    if (sameDay) return SimpleDateFormat("HH:mm", app.turp.data.I18n.locale()).format(date)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (yesterday) return t("gestern")
    return SimpleDateFormat("dd.MM.", app.turp.data.I18n.locale()).format(date)
}

/** Wie lange her: "jetzt", "5 Min.", "4 Std.", "2 Tage", danach das Datum */
fun timeAgo(ts: Timestamp?): String {
    if (ts == null) return ""
    val minutes = ((System.currentTimeMillis() - ts.toDate().time) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> t("jetzt")
        minutes < 60 -> t("{0} Min.", minutes)
        minutes < 24 * 60 -> t("{0} Std.", minutes / 60)
        minutes < 7 * 24 * 60 -> (minutes / (24 * 60)).let { if (it == 1L) t("1 Tag") else t("{0} Tage", it) }
        else -> SimpleDateFormat("dd.MM.", app.turp.data.I18n.locale()).format(ts.toDate())
    }
}

/** Kurz für die Chat-Liste: "jetzt", "5 Min.", "3 Std.", "3 T.", ab einer Woche "7 T.+" */
fun chatAge(ts: Timestamp?): String {
    if (ts == null) return ""
    val minutes = ((System.currentTimeMillis() - ts.toDate().time) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> t("jetzt")
        minutes < 60 -> t("{0} Min.", minutes)
        minutes < 24 * 60 -> t("{0} Std.", minutes / 60)
        minutes < 7 * 24 * 60 -> t("{0} T.", minutes / (24 * 60))
        else -> t("7 T.+")
    }
}

/** Gilt als "Online", wenn die letzte Meldung höchstens so lange her ist */
const val ONLINE_WINDOW_MS = 4 * 60_000L

/** Für "Zuletzt online …": "vor 5 Min.", "vor 18 Std.", "vor 2 Tagen", "am 03.10." */
fun lastOnline(ts: Timestamp): String {
    val minutes = ((System.currentTimeMillis() - ts.toDate().time) / 60_000).coerceAtLeast(1)
    return when {
        minutes < 60 -> t("vor {0} Min.", minutes)
        minutes < 24 * 60 -> t("vor {0} Std.", minutes / 60)
        minutes < 7 * 24 * 60 -> (minutes / (24 * 60)).let { if (it == 1L) t("vor 1 Tag") else t("vor {0} Tagen", it) }
        else -> t("am {0}", SimpleDateFormat("dd.MM.", app.turp.data.I18n.locale()).format(ts.toDate()))
    }
}

/** Liegen beide Zeitpunkte am selben Kalendertag? */
fun sameDay(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return true
    val x = Calendar.getInstance().apply { time = a.toDate() }
    val y = Calendar.getInstance().apply { time = b.toDate() }
    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
}

/** Datum für die Trenner im Chat: "Heute", "Gestern" oder "03. Okt. 2026" */
fun dayLabel(ts: Timestamp?): String {
    if (ts == null) return t("Heute")
    val now = Timestamp.now()
    if (sameDay(ts, now)) return t("Heute")
    val yesterday = Timestamp(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
    if (sameDay(ts, yesterday)) return t("Gestern")
    return SimpleDateFormat("dd. MMM yyyy", app.turp.data.I18n.locale()).format(ts.toDate())
}

fun timeOfDay(ts: Timestamp?): String =
    if (ts == null) "" else SimpleDateFormat("HH:mm", app.turp.data.I18n.locale()).format(ts.toDate())

object Countries {
    data class Country(val code: String, val name: String)

    val all: List<Country> by lazy {
        val collator = Collator.getInstance(app.turp.data.I18n.locale())
        Locale.getISOCountries()
            .map { Country(it, name(it)) }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    fun name(code: String): String =
        if (code.length != 2) "" else Locale("", code).getDisplayCountry(app.turp.data.I18n.locale())

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

/** Meldegründe – gespeichert wird der deutsche Text, angezeigt die Übersetzung */
val REPORT_REASONS = listOf(
    "Belästigung oder Mobbing",
    "Sexuelle Inhalte",
    "Falsches Alter / Fake-Profil",
    "Spam oder Betrug",
    "Gewalt oder Drohungen",
    "Sonstiges",
)
