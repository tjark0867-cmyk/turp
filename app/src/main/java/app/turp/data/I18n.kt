package app.turp.data

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.util.Locale

/**
 * Übersetzungen. Alle Texte stehen im Code auf Deutsch und laufen durch [t].
 * Die Übersetzungen liegen in assets/i18n: source.json (deutsche Texte) und pro Sprache
 * eine Datei mit den Übersetzungen in derselben Reihenfolge. Fehlt eine Übersetzung,
 * erscheint der deutsche Text.
 */
object I18n {
    data class Lang(val code: String, val name: String, val flag: String)

    val LANGS = listOf(
        Lang("de", "German", "🇩🇪"),
        Lang("en", "English", "🇬🇧"),
        Lang("es", "Spanish", "🇪🇸"),
        Lang("fr", "French", "🇫🇷"),
        Lang("it", "Italian", "🇮🇹"),
        Lang("pt", "Portuguese", "🇧🇷"),
        Lang("nl", "Dutch", "🇳🇱"),
        Lang("pl", "Polish", "🇵🇱"),
        Lang("tr", "Turkish", "🇹🇷"),
        Lang("ru", "Russian", "🇷🇺"),
        Lang("uk", "Ukrainian", "🇺🇦"),
        Lang("ar", "Arabic", "🇸🇦"),
        Lang("ja", "Japanese", "🇯🇵"),
        Lang("ko", "Korean", "🇰🇷"),
        Lang("zh", "Chinese", "🇨🇳"),
        Lang("hi", "Hindi", "🇮🇳"),
        Lang("id", "Indonesian", "🇮🇩"),
    )

    private const val PREFS = "settings"
    private const val KEY = "lang"

    /** Aktuelle Sprache – Compose zeichnet neu, wenn sie sich ändert */
    var lang by mutableStateOf("de")
        private set

    /** Wurde schon eine Sprache ausgewählt? (sonst zeigt die App zuerst die Auswahl) */
    var chosen by mutableStateOf(true)
        private set

    private var appContext: Context? = null
    private var sources: List<String> = emptyList()
    private val tables = HashMap<String, Map<String, String>>()

    fun init(context: Context) {
        appContext = context.applicationContext
        val saved = savedLang(context)
        chosen = saved != null
        lang = saved ?: deviceLang()
    }

    /** Gespeicherte Sprache (oder null) – auch für attachBaseContext, bevor init läuft */
    fun savedLang(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.takeIf { c -> LANGS.any { it.code == c } }

    /** Gerätesprache, falls wir sie haben, sonst Englisch */
    fun deviceLang(): String {
        val dev = Locale.getDefault().language.let { if (it == "in") "id" else it }
        return LANGS.firstOrNull { it.code == dev }?.code ?: "en"
    }

    fun set(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).commit()
        lang = code
        chosen = true
    }

    fun locale(code: String = lang): Locale = Locale.forLanguageTag(code)

    /** Context mit der gewählten Sprache (Datumsauswahl, Schreibrichtung usw.) */
    fun wrap(base: Context): Context {
        val code = savedLang(base) ?: return base
        // Land vom Handy behalten (für den Länder-Vorschlag), nur die Sprache tauschen
        val country = base.resources.configuration.locales[0].country
        val loc = Locale.Builder().setLanguage(code).setRegion(country.takeIf { it.length == 2 } ?: "").build()
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        return base.createConfigurationContext(cfg)
    }

    private fun table(code: String): Map<String, String> {
        if (code == "de") return emptyMap()
        tables[code]?.let { return it }
        val ctx = appContext ?: return emptyMap()
        val map = runCatching {
            if (sources.isEmpty()) sources = readArray(ctx, "i18n/source.json")
            val tr = readArray(ctx, "i18n/$code.json")
            val m = HashMap<String, String>(sources.size * 2)
            for (i in sources.indices) {
                val v = tr.getOrNull(i)
                if (!v.isNullOrBlank()) m[sources[i]] = v
            }
            m
        }.getOrDefault(emptyMap())
        tables[code] = map
        return map
    }

    private fun readArray(ctx: Context, path: String): List<String> {
        val text = ctx.assets.open(path).bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        return List(arr.length()) { arr.optString(it) }
    }

    /** Meldung vom Server übersetzen – auch solche mit Zahlen darin („… höchstens 6 Fotos …“) */
    fun tServer(msg: String): String {
        val table = table(lang)
        table[msg]?.let { return it }
        for (src in sources) {
            if (!src.contains("{0}")) continue
            val pattern = Regex.escape(src).replace(Regex("\\{(\\d)\\}"), "\\\\E(.+?)\\\\Q")
            val m = Regex("^$pattern$").find(msg) ?: continue
            val tr = table[src] ?: return msg
            var out = tr
            m.groupValues.drop(1).forEachIndexed { i, v -> out = out.replace("{$i}", v) }
            return out
        }
        return msg
    }

    /** Übersetzt einen deutschen Text; {0}, {1} … werden durch [args] ersetzt */
    fun t(src: String, vararg args: Any?): String = tIn(lang, src, *args)

    fun tIn(code: String, src: String, vararg args: Any?): String {
        var out = table(code)[src] ?: src
        args.forEachIndexed { i, a -> out = out.replace("{$i}", a.toString()) }
        return out
    }
}

/** Kurzform für Übersetzungen */
fun t(src: String, vararg args: Any?): String = I18n.t(src, *args)
