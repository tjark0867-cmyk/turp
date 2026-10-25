package app.turp

import app.turp.data.t

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp

class TurpApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        app.turp.data.I18n.init(this)
        app.turp.ui.theme.Look.init(this)
        // Ohne google-services.json gibt es keine Firebase-Konfiguration.
        // Die App zeigt dann einen Hinweis statt abzustürzen.
        firebaseReady = FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null

        val channel = NotificationChannel(
            CHANNEL_ID,
            t("Nachrichten & Anfragen"),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = t("Neue Nachrichten und Freundschaftsanfragen") }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * Bilder-Lader für die ganze App. Jedes hochgeladene Bild bekommt eine neue Adresse,
     * ein Bild ändert sich also nie – darum wird es aus dem Speicher gezeigt, ohne erst
     * beim Server nachzufragen (die Server-Header würden sonst jedes Mal eine Anfrage erzwingen).
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .crossfade(120)
            .build()

    companion object {
        const val CHANNEL_ID = "turp_default"

        var firebaseReady: Boolean = false
            private set
    }
}
