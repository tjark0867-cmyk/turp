package app.turp.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Gemeinsame Basis: Lade-Anzeige und kurze Meldungen (Toasts) für die Oberfläche */
abstract class BaseViewModel : ViewModel() {

    val notices = MutableSharedFlow<String>(extraBufferCapacity = 8)

    var busy by mutableStateOf(false)
        protected set

    protected fun toast(text: String) {
        notices.tryEmit(text)
    }

    /** Startet eine Aktion; Fehler werden als verständliche Meldung angezeigt. */
    protected fun launchSafe(showBusy: Boolean = true, block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            if (showBusy) busy = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("turp", "Aktion fehlgeschlagen", e)
                toast(e.toUserMessage())
            } finally {
                if (showBusy) busy = false
            }
        }
}
