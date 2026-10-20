package app.turp.data

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.util.Collections

/** Hilfen, damit Bilder sofort erscheinen statt erst zu laden */
object Images {
    private val requested: MutableSet<String> = Collections.synchronizedSet(HashSet())

    /**
     * Lädt Bilder schon im Hintergrund in den Speicher (z. B. Profilbilder von Freunden,
     * während man noch im Entdecken ist). Später werden sie dann ohne Wartezeit angezeigt.
     */
    fun prefetch(context: Context, urls: Collection<String>, limit: Int = 60) {
        val loader = context.imageLoader
        urls.asSequence()
            .filter { it.isNotBlank() }
            .filter { requested.add(it) }
            .take(limit)
            .forEach { url ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(256)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .listener(onError = { _, _ -> requested.remove(url) })
                        .build()
                )
            }
    }

    /**
     * Gerade selbst hochgeladene Bilddaten direkt unter ihrer Adresse ablegen,
     * damit das eigene Foto nicht noch einmal heruntergeladen werden muss.
     * Nicht im Haupt-Thread aufrufen (schreibt auf den Speicher).
     */
    @OptIn(ExperimentalCoilApi::class)
    fun seed(context: Context, url: String, bytes: ByteArray) {
        if (url.isBlank() || bytes.isEmpty()) return
        val cache = context.imageLoader.diskCache ?: return
        val editor = cache.openEditor(url) ?: return
        try {
            cache.fileSystem.write(editor.data) { write(bytes) }
            // Leere Zusatzdaten = "selbst abgelegt", Coil nimmt den Eintrag dann direkt
            cache.fileSystem.write(editor.metadata) { }
            editor.commit()
            requested.add(url)
        } catch (e: Exception) {
            runCatching { editor.abort() }
        }
    }
}
