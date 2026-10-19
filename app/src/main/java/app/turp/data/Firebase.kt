package app.turp.data

import android.util.Log
import app.turp.BuildConfig
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Zentrale Zugriffe auf Firebase */
object Fb {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION) }

    /** UID des angemeldeten Nutzers oder leerer Text */
    val uid: String get() = auth.currentUser?.uid ?: ""
}

/** Ruft eine Cloud Function auf und liefert ihre Antwort als Map. */
suspend fun callFunction(
    name: String,
    data: Map<String, Any?> = emptyMap(),
    timeoutSeconds: Long = 70,
): Map<String, Any?> {
    val callable = Fb.functions.getHttpsCallable(name)
    callable.setTimeout(timeoutSeconds, TimeUnit.SECONDS)
    val result = callable.call(data).await()
    @Suppress("UNCHECKED_CAST")
    return (result.data as? Map<String, Any?>) ?: emptyMap()
}

/** Live-Updates eines Dokuments als Flow. Fehler werden nur protokolliert. */
fun DocumentReference.asFlow(): Flow<DocumentSnapshot> {
    val ref = this
    return callbackFlow {
        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("turp", "Lesen von ${ref.path} fehlgeschlagen: ${error.code}")
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }
}

/** Live-Updates einer Abfrage als Flow. Fehler werden nur protokolliert. */
fun Query.asFlow(): Flow<List<DocumentSnapshot>> {
    val query = this
    return callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("turp", "Abfrage fehlgeschlagen: ${error.code}", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.documents)
        }
        awaitClose { registration.remove() }
    }
}

/** Verständliche Fehlermeldung auf Deutsch */
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseFunctionsException ->
        if (code == FirebaseFunctionsException.Code.INTERNAL || code == FirebaseFunctionsException.Code.UNKNOWN) {
            "Serverfehler – bitte versuch es gleich noch einmal."
        } else {
            message ?: "Etwas ist schiefgelaufen."
        }
    is FirebaseAuthWeakPasswordException -> "Das Passwort ist zu schwach (mindestens 6 Zeichen)."
    is FirebaseAuthUserCollisionException -> "Diese E-Mail-Adresse ist schon registriert. Log dich ein."
    is FirebaseAuthInvalidUserException -> "Kein aktives Konto mit dieser E-Mail gefunden."
    is FirebaseAuthInvalidCredentialsException -> "E-Mail oder Passwort ist falsch."
    is FirebaseNetworkException -> "Keine Internetverbindung."
    is FirebaseFirestoreException ->
        if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) "Dafür fehlt dir die Berechtigung."
        else "Speichern fehlgeschlagen – bitte versuch es noch einmal."
    is StorageException -> "Upload fehlgeschlagen – bitte versuch es noch einmal."
    is IllegalArgumentException -> message ?: "Ungültige Eingabe."
    else -> message ?: "Etwas ist schiefgelaufen."
}
