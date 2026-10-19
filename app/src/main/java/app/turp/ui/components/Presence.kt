package app.turp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import app.turp.data.PresenceRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

/**
 * Ist diese Person gerade online? (Nur bei Freunden bekannt, der Test-Bot ist immer online.)
 * Wird jede Minute neu geprüft.
 */
@Composable
fun rememberIsOnline(uid: String): Boolean {
    val bot = uid.startsWith("turp-bot-")
    val last by remember(uid) {
        if (uid.isEmpty() || bot) flowOf(null) else PresenceRepo.lastActive(uid)
    }.collectAsState(initial = null)
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }
    val seen = last
    return bot || (seen != null && now - seen.toDate().time < ONLINE_WINDOW_MS)
}
