package app.turp

import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import coil.ImageLoaderFactory
import coil.ImageLoader
import android.os.Build
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.FirebaseApp

class TurpApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Ohne google-services.json gibt es keine Firebase-Konfiguration.
        // Die App zeigt dann einen Hinweis statt abzustürzen.
        firebaseReady = FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nachrichten & Anfragen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Neue Nachrichten und Freundschaftsanfragen" }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Bilder-Lader mit Unterstützung für animierte GIFs */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .crossfade(true)
            .build()

    companion object {
        const val CHANNEL_ID = "turp_default"

        var firebaseReady: Boolean = false
            private set
    }
}
