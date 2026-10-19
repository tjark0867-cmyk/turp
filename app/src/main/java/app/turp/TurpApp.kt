package app.turp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.FirebaseApp

class TurpApp : Application() {

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

    companion object {
        const val CHANNEL_ID = "turp_default"

        var firebaseReady: Boolean = false
            private set
    }
}
