package app.turp.data

import android.content.Context

/** Wo die Leute wohnen sollen */
enum class CountryMode { WORLD, MINE, OTHER }

/** Such-Einstellungen fürs Entdecken (werden auf dem Handy gespeichert) */
data class DiscoverFilter(
    val interests: List<String> = emptyList(),
    val countryMode: CountryMode = CountryMode.WORLD,
    val otherCountry: String = "",
    val minAge: Int = 13,
    val maxAge: Int = 17,
    /** Leute mit gleichen Interessen wie im eigenen Profil zuerst zeigen */
    val sharedFirst: Boolean = true,
    /** "" = alle, sonst nur "male" oder "female" */
    val gender: String = "",
) {
    /** Ländercode für die Abfrage, null = weltweit */
    fun countryCode(myCountry: String): String? = when (countryMode) {
        CountryMode.WORLD -> null
        CountryMode.MINE -> myCountry.ifBlank { null }
        CountryMode.OTHER -> otherCountry.ifBlank { null }
    }

    /** Weicht von den Standard-Einstellungen ab? (für den Punkt am Filter-Symbol) */
    fun isActive(bounds: IntRange): Boolean =
        interests.isNotEmpty() || countryMode != CountryMode.WORLD ||
            minAge > bounds.first || maxAge < bounds.last || gender.isNotEmpty()

    companion object {
        const val MAX_SEARCH_INTERESTS = 4

        fun defaults(bounds: IntRange) = DiscoverFilter(minAge = bounds.first, maxAge = bounds.last)
    }
}

object DiscoverPrefs {
    /** Pro Konto eigene Datei – sonst übernimmt ein neues Konto die Filter des alten */
    private fun file(uid: String) = "discover_prefs_$uid"

    fun load(context: Context, uid: String, bounds: IntRange): DiscoverFilter {
        val p = context.getSharedPreferences(file(uid), Context.MODE_PRIVATE)
        val group = "${bounds.first}-${bounds.last}"
        // Nach dem Wechsel der Altersgruppe (18. Geburtstag) passen die alten Werte nicht mehr
        val sameGroup = p.getString("group", null) == group
        val min = if (sameGroup) p.getInt("minAge", bounds.first) else bounds.first
        val max = if (sameGroup) p.getInt("maxAge", bounds.last) else bounds.last
        return DiscoverFilter(
            interests = p.getString("interests", "").orEmpty()
                .split('\n').filter { it.isNotBlank() }.take(DiscoverFilter.MAX_SEARCH_INTERESTS),
            countryMode = runCatching { CountryMode.valueOf(p.getString("countryMode", "WORLD")!!) }
                .getOrDefault(CountryMode.WORLD),
            otherCountry = p.getString("otherCountry", "").orEmpty(),
            minAge = min.coerceIn(bounds),
            maxAge = max.coerceIn(bounds).coerceAtLeast(min.coerceIn(bounds)),
            sharedFirst = p.getBoolean("sharedFirst", true),
            gender = p.getString("gender", "").orEmpty(),
        )
    }

    fun save(context: Context, uid: String, bounds: IntRange, f: DiscoverFilter) {
        context.getSharedPreferences(file(uid), Context.MODE_PRIVATE).edit()
            .putString("group", "${bounds.first}-${bounds.last}")
            .putString("interests", f.interests.joinToString("\n"))
            .putString("countryMode", f.countryMode.name)
            .putString("otherCountry", f.otherCountry)
            .putInt("minAge", f.minAge)
            .putInt("maxAge", f.maxAge)
            .putBoolean("sharedFirst", f.sharedFirst)
            .putString("gender", f.gender)
            .apply()
    }
}
