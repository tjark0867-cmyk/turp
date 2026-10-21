package app.turp.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.turp.TurpApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** In welchem Zustand ist der Nutzer gerade? */
sealed interface SessionState {
    data object Loading : SessionState
    data object NoFirebase : SessionState
    data object SignedOut : SessionState
    data object NeedsProfile : SessionState
    data class PendingConsent(val account: Account) : SessionState
    data object Banned : SessionState
    data class Active(
        val uid: String,
        val account: Account,
        val isAdmin: Boolean,
    ) : SessionState
}

class SessionViewModel : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state

    private var accountJob: Job? = null
    private var tokenSaved = false

    private val authListener = FirebaseAuth.AuthStateListener { auth -> onUserChanged(auth.currentUser) }

    init {
        if (!TurpApp.firebaseReady) {
            _state.value = SessionState.NoFirebase
        } else {
            Fb.auth.addAuthStateListener(authListener)
        }
    }

    private fun onUserChanged(user: FirebaseUser?) {
        accountJob?.cancel()
        tokenSaved = false
        if (user == null) {
            _state.value = SessionState.SignedOut
            return
        }
        _state.value = SessionState.Loading
        accountJob = viewModelScope.launch {
            var lastKey: String? = null
            var serverChecked = false
            val ref = Fb.db.collection("accounts").document(user.uid)
            // Mit Metadaten: so kommt auch die Bestätigung des Servers an, wenn sich
            // am Dokument selbst nichts ändert (z. B. "gibt es noch nicht").
            ref.asFlow(includeMetadata = true).collect { snap ->
                if (!snap.exists()) {
                    if (!snap.metadata.isFromCache) {
                        _state.value = SessionState.NeedsProfile
                    } else if (!serverChecked) {
                        // Nur aus dem Offline-Speicher: einmal direkt beim Server nachfragen
                        serverChecked = true
                        launch {
                            try {
                                val fresh = ref.get(Source.SERVER).await()
                                if (!fresh.exists() && _state.value == SessionState.Loading) {
                                    _state.value = SessionState.NeedsProfile
                                }
                            } catch (e: Exception) {
                                Log.w("turp", "Konto konnte nicht vom Server geladen werden", e)
                            }
                        }
                    }
                    return@collect
                }
                val account = snap.toAccount()
                // Ändern sich Status oder Altersgruppe, muss der Login-Token neu geladen werden,
                // damit die neuen Rechte (Custom Claims) gelten.
                val key = "${account.status}|${account.ageGroup}"
                val forceRefresh = key != lastKey
                lastKey = key
                val claims = try {
                    user.getIdToken(forceRefresh).await().claims
                } catch (e: Exception) {
                    Log.w("turp", "Token konnte nicht geladen werden", e)
                    emptyMap()
                }
                val isAdmin = claims["admin"] == true

                _state.value = when (account.status) {
                    "pending_consent" -> SessionState.PendingConsent(account)
                    "banned" -> SessionState.Banned
                    "active" -> SessionState.Active(user.uid, account, isAdmin)
                    else -> SessionState.Loading
                }

                if (account.status == "active" && !tokenSaved) {
                    tokenSaved = true
                    saveFcmToken(account.fcmToken)
                }
                if (account.status == "active" && account.lang != I18n.lang) {
                    viewModelScope.launch { runCatching { ProfileRepo.saveLang(I18n.lang) } }
                }
            }
        }
    }

    private fun saveFcmToken(current: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (token != current) ProfileRepo.saveFcmToken(token)
            } catch (e: Exception) {
                Log.w("turp", "Push-Token konnte nicht gespeichert werden", e)
            }
        }
    }

    /** Lädt den Anmeldestatus neu (falls das Laden hängt). */
    fun retry() {
        if (TurpApp.firebaseReady) onUserChanged(Fb.auth.currentUser)
    }

    /** Lädt Rechte neu, z. B. nachdem jemand Admin geworden ist. */
    fun refreshClaims() {
        viewModelScope.launch {
            val user = Fb.auth.currentUser ?: return@launch
            try {
                val claims = user.getIdToken(true).await().claims
                val s = _state.value
                if (s is SessionState.Active) _state.value = s.copy(isAdmin = claims["admin"] == true)
            } catch (e: Exception) {
                Log.w("turp", "Rechte konnten nicht neu geladen werden", e)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            if (_state.value is SessionState.Active) {
                // Keine Push-Nachrichten mehr an dieses Gerät
                try {
                    ProfileRepo.saveFcmToken("")
                } catch (_: Exception) {
                }
            }
            Fb.auth.signOut()
        }
    }

    override fun onCleared() {
        if (TurpApp.firebaseReady) Fb.auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
