package app.turp.data

import app.turp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Ein GIF aus der Suche: ID, kleines Vorschaubild und Seitenverhältnis */
data class Gif(val id: String, val previewUrl: String, val width: Int, val height: Int)

/**
 * GIFs von GIPHY – immer mit Altersfreigabe "G" (für alle geeignet).
 * Braucht einen kostenlosen GIPHY-Schlüssel (GitHub-Secret GIPHY_API_KEY).
 * Im Chat wird nur die GIF-ID gespeichert, angezeigt wird immer von media.giphy.com.
 */
object Gifs {

    private const val API = "https://api.giphy.com/v1/gifs/"
    private val validId = Regex("^[A-Za-z0-9]{5,64}$")

    val enabled: Boolean get() = BuildConfig.GIPHY_KEY.isNotBlank()

    /** GIF in Chat-Größe (200 px breit) */
    fun url(id: String): String = "https://media.giphy.com/media/$id/200w.gif"

    suspend fun trending(): List<Gif> = load("trending", "")

    suspend fun search(query: String): List<Gif> = load("search", query)

    private suspend fun load(kind: String, query: String): List<Gif> = withContext(Dispatchers.IO) {
        val params = StringBuilder("api_key=").append(enc(BuildConfig.GIPHY_KEY)).append("&limit=30&rating=g")
        if (query.isNotBlank()) params.append("&q=").append(enc(query.trim().take(50))).append("&lang=de")
        val connection = URL("$API$kind?$params").openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        try {
            if (connection.responseCode != 200) {
                throw IllegalStateException("GIFs konnten nicht geladen werden (Fehler ${connection.responseCode}).")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val data = json.optJSONArray("data") ?: return@withContext emptyList()
            (0 until data.length()).mapNotNull { i ->
                val item = data.optJSONObject(i) ?: return@mapNotNull null
                val id = item.optString("id")
                if (!validId.matches(id)) return@mapNotNull null
                val images = item.optJSONObject("images") ?: return@mapNotNull null
                val fixed = images.optJSONObject("fixed_width") ?: return@mapNotNull null
                val preview = images.optJSONObject("fixed_width_downsampled") ?: fixed
                Gif(
                    id = id,
                    previewUrl = preview.optString("url").ifBlank { url(id) },
                    width = fixed.optString("width").toIntOrNull()?.coerceIn(1, 1000) ?: 200,
                    height = fixed.optString("height").toIntOrNull()?.coerceIn(1, 2000) ?: 200,
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
