package app.turp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.turp.ui.TurpRoot
import app.turp.ui.theme.TurpTheme

class MainActivity : ComponentActivity() {

    /** Chat, der über eine Push-Benachrichtigung geöffnet werden soll */
    private val pendingChatId = mutableStateOf<String?>(null)

    // Gewählte Sprache für die ganze Oberfläche (auch Datumsauswahl und Schreibrichtung)
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(app.turp.data.I18n.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pappe-Modus ist hell: dunkle Symbole in Status- und Navigationsleiste
        val light = app.turp.ui.theme.Look.cardboard
        enableEdgeToEdge(
            statusBarStyle = if (light) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = if (light) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT) else SystemBarStyle.dark(Color.TRANSPARENT)
        )
        pendingChatId.value = intent?.getStringExtra("chatId")
        setContent {
            TurpTheme {
                TurpRoot(
                    pendingChatId = pendingChatId.value,
                    onChatHandled = { pendingChatId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("chatId")?.let { pendingChatId.value = it }
    }
}
