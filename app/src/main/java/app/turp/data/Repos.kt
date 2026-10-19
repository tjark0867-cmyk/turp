package app.turp.data

import com.google.firebase.Timestamp
import android.content.Context
import android.net.Uri
import app.turp.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

private fun users() = Fb.db.collection("users")
private fun userRef(uid: String) = users().document(uid)

// ---- Profil & Konto -------------------------------------------------------

object ProfileRepo {

    fun profile(uid: String): Flow<UserProfile?> =
        userRef(uid).asFlow().map { if (it.exists()) it.toProfile() else null }

    /** Profil einmalig laden (null, wenn es nicht existiert oder nicht sichtbar ist) */
    suspend fun get(uid: String): UserProfile? {
        val snap = userRef(uid).get().await()
        return if (snap.exists()) snap.toProfile() else null
    }

    fun account(uid: String): Flow<Account?> =
        Fb.db.collection("accounts").document(uid).asFlow().map { if (it.exists()) it.toAccount() else null }

    fun socials(uid: String): Flow<Socials?> =
        userRef(uid).collection("private").document("socials").asFlow()
            .map { if (it.exists()) it.toSocials() else null }

    /** Chat-ID, falls wir mit [uid] befreundet sind, sonst null */
    fun friendChatId(uid: String): Flow<String?> =
        userRef(Fb.uid).collection("friends").document(uid).asFlow()
            .map { if (it.exists()) it.getString("chatId") else null }

    suspend fun createProfile(
        name: String,
        birthDate: String,
        country: String,
        parentEmail: String,
        inviteCode: String,
    ) {
        callFunction(
            "createProfile",
            mapOf(
                "name" to name,
                "birthDate" to birthDate,
                "country" to country,
                "parentEmail" to parentEmail,
                "inviteCode" to inviteCode,
            )
        )
    }

    suspend fun updateProfile(fields: Map<String, Any>) {
        userRef(Fb.uid).update(fields).await()
    }

    suspend fun saveSocials(s: Socials) {
        userRef(Fb.uid).collection("private").document("socials").set(
            mapOf(
                "instagram" to cleanHandle(s.instagram),
                "snapchat" to cleanHandle(s.snapchat),
                "tiktok" to cleanHandle(s.tiktok),
                "visible" to s.visible,
            )
        ).await()
    }

    /** Erlaubt nur Buchstaben, Zahlen, Punkt, Unterstrich und Bindestrich */
    fun cleanHandle(raw: String): String =
        raw.trim().removePrefix("@").filter { it.isLetterOrDigit() && it.code < 128 || it in "._-" }.take(40)

    suspend fun uploadPhoto(context: Context, uri: Uri): String {
        val bytes = withContext(Dispatchers.IO) { MediaUtils.compressImage(context, uri) }
        val path = "uploads/${Fb.uid}/${UUID.randomUUID()}.jpg"
        val meta = StorageMetadata.Builder().setContentType("image/jpeg").build()
        Fb.storage.reference.child(path).putBytes(bytes, meta).await()
        val result = callFunction("addProfileMedia", mapOf("path" to path, "kind" to "photo"), timeoutSeconds = 120)
        return result["url"] as? String ?: ""
    }

    suspend fun uploadVideo(context: Context, uri: Uri): String {
        val info = withContext(Dispatchers.IO) { MediaUtils.videoInfo(context, uri) }
        if (info.durationMs > 30_000) throw IllegalArgumentException("Das Video darf höchstens 30 Sekunden lang sein.")
        if (info.sizeBytes > 40L * 1024 * 1024) throw IllegalArgumentException("Das Video ist zu groß (höchstens 40 MB).")
        val ext = when {
            info.mimeType.contains("quicktime") -> "mov"
            info.mimeType.contains("webm") -> "webm"
            info.mimeType.contains("3gp") -> "3gp"
            else -> "mp4"
        }
        val path = "uploads/${Fb.uid}/${UUID.randomUUID()}.$ext"
        val meta = StorageMetadata.Builder().setContentType(info.mimeType).build()
        Fb.storage.reference.child(path).putFile(uri, meta).await()
        val result = callFunction("addProfileMedia", mapOf("path" to path, "kind" to "video"), timeoutSeconds = 300)
        return result["url"] as? String ?: ""
    }

    suspend fun removeMedia(url: String) {
        callFunction("removeProfileMedia", mapOf("url" to url))
    }

    /** Täglicher Check-in. Liefert Belohnung und Serie. */
    suspend fun checkIn(): Pair<Long, Long> {
        val r = callFunction("checkIn")
        return ((r["reward"] as? Number)?.toLong() ?: 0L) to ((r["streak"] as? Number)?.toLong() ?: 0L)
    }

    suspend fun resendConsent(parentEmail: String) {
        callFunction("resendConsent", mapOf("parentEmail" to parentEmail))
    }

    suspend fun claimAdmin() {
        callFunction("claimAdmin")
    }

    suspend fun saveFcmToken(token: String) {
        Fb.db.collection("accounts").document(Fb.uid).update("fcmToken", token).await()
    }

    suspend fun deleteAccount() {
        callFunction("deleteAccount", timeoutSeconds = 300)
        Fb.auth.signOut()
    }
}

// ---- Entdecken ------------------------------------------------------------

object DiscoverRepo {

    suspend fun candidates(ageGroup: String, country: String?, minAge: Int, maxAge: Int): List<UserProfile> {
        val me = Fb.uid
        val meRef = userRef(me)
        val excluded = coroutineScope {
            val seen = async { meRef.collection("seen").get().await().documents.map { it.id } }
            val blocked = async { meRef.collection("blocked").get().await().documents.map { it.id } }
            val friends = async { meRef.collection("friends").get().await().documents.map { it.id } }
            (seen.await() + blocked.await() + friends.await()).toHashSet()
        }
        excluded.add(me)

        var base: Query = users()
            .whereEqualTo("ageGroup", ageGroup)
            .whereEqualTo("active", true)
        if (country != null) base = base.whereEqualTo("country", country)

        // Zufälliger Startpunkt, damit jeder andere Leute zuerst sieht
        val pivot = Math.random()
        val after = base.orderBy("random").startAt(pivot).limit(80).get().await().documents
        val before = if (after.size < 80) {
            base.orderBy("random").endBefore(pivot).limit(80).get().await().documents
        } else {
            emptyList()
        }

        return (after + before).asSequence()
            .map { it.toProfile() }
            .filter { it.uid !in excluded && it.photos.isNotEmpty() }
            // Der Test-Bot erscheint nur in Test-Versionen (Debug-Builds), dann ganz vorne
            .filter { if (it.bot) BuildConfig.DEBUG else it.age in minAge..maxAge }
            .distinctBy { it.uid }
            .sortedByDescending { it.bot }
            .toList()
    }

    suspend fun skip(uid: String) {
        userRef(Fb.uid).collection("seen").document(uid)
            .set(mapOf("at" to FieldValue.serverTimestamp(), "liked" to false)).await()
    }

    /** Übersprungene Profile wieder anzeigen lassen */
    suspend fun resetSkipped() {
        val docs = userRef(Fb.uid).collection("seen").whereEqualTo("liked", false).limit(450).get().await()
        if (docs.isEmpty) return
        val batch = Fb.db.batch()
        docs.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /** Sendet eine Freundschaftsanfrage. true = sofort befreundet (gegenseitig) */
    suspend fun sendRequest(uid: String): Boolean {
        val r = callFunction("sendRequest", mapOf("toUid" to uid))
        return r["matched"] == true
    }
}

// ---- Anfragen -------------------------------------------------------------

object RequestRepo {

    fun incoming(): Flow<List<FriendRequest>> =
        Fb.db.collection("requests")
            .whereEqualTo("to", Fb.uid)
            .whereEqualTo("status", "pending")
            .asFlow()
            .map { docs -> docs.map { it.toRequest() }.sortedByDescending { it.createdAt } }

    fun outgoing(): Flow<List<FriendRequest>> =
        Fb.db.collection("requests")
            .whereEqualTo("from", Fb.uid)
            .whereEqualTo("status", "pending")
            .asFlow()
            .map { docs -> docs.map { it.toRequest() }.sortedByDescending { it.createdAt } }

    /** Annehmen oder ablehnen. Liefert beim Annehmen die Chat-ID. */
    suspend fun respond(requestId: String, accept: Boolean): String? {
        val r = callFunction("respondRequest", mapOf("requestId" to requestId, "accept" to accept))
        return r["chatId"] as? String
    }
}

// ---- Chats ----------------------------------------------------------------

object ChatRepo {

    private fun chatRef(id: String) = Fb.db.collection("chats").document(id)

    fun chats(): Flow<List<Chat>> =
        Fb.db.collection("chats")
            .whereArrayContains("members", Fb.uid)
            .asFlow()
            .map { docs -> docs.map { it.toChat() }.sortedByDescending { it.lastAt } }

    fun chat(id: String): Flow<Chat?> =
        chatRef(id).asFlow().map { if (it.exists()) it.toChat() else null }

    fun messages(id: String): Flow<List<Message>> =
        chatRef(id).collection("messages")
            .orderBy("createdAt")
            .limitToLast(200)
            .asFlow()
            .map { docs -> docs.map { it.toMessage() } }

    suspend fun sendText(chatId: String, text: String) {
        chatRef(chatId).collection("messages").add(
            mapOf(
                "senderId" to Fb.uid,
                "type" to "text",
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun sendImage(context: Context, chatId: String, uri: Uri) {
        val bytes = withContext(Dispatchers.IO) { MediaUtils.compressImage(context, uri, maxSide = 1280, quality = 80) }
        val path = "chatUploads/${Fb.uid}/${UUID.randomUUID()}.jpg"
        val meta = StorageMetadata.Builder().setContentType("image/jpeg").build()
        Fb.storage.reference.child(path).putBytes(bytes, meta).await()
        chatRef(chatId).collection("messages").add(
            mapOf(
                "senderId" to Fb.uid,
                "type" to "image",
                "text" to "",
                "imagePath" to path,
                "imageStatus" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    /** Startet einen Chat mit dem Test-Bot und liefert die Chat-ID */
    suspend fun startTestChat(): String =
        callFunction("startTestChat")["chatId"] as? String
            ?: throw IllegalStateException("Der Test-Chat konnte nicht gestartet werden.")

    suspend fun markRead(chatId: String) {
        chatRef(chatId).update("readAt.${Fb.uid}", FieldValue.serverTimestamp()).await()
    }
}

// ---- Lounge ---------------------------------------------------------------

object RoomRepo {

    fun messages(group: String): Flow<List<RoomMessage>> =
        Fb.db.collection("rooms").document(group).collection("messages")
            .orderBy("createdAt")
            .limitToLast(100)
            .asFlow()
            .map { docs -> docs.map { it.toRoomMessage() } }

    suspend fun post(text: String) {
        callFunction("postRoomMessage", mapOf("text" to text))
    }
}

// ---- Sicherheit -----------------------------------------------------------

object SafetyRepo {

    suspend fun report(targetUid: String, reason: String, details: String, context: String) {
        callFunction(
            "report",
            mapOf("targetUid" to targetUid, "reason" to reason, "details" to details, "context" to context)
        )
    }

    suspend fun block(targetUid: String) {
        callFunction("blockUser", mapOf("targetUid" to targetUid))
    }
}

// ---- Admin ----------------------------------------------------------------

object AdminRepo {

    fun openReports(): Flow<List<Report>> =
        Fb.db.collection("reports")
            .whereEqualTo("status", "open")
            .asFlow()
            .map { docs ->
                docs.map { it.toReport() }
                    .sortedWith(compareBy<Report> { it.priority }.thenBy { it.createdAt })
            }

    /** action: "ban" oder "dismiss" */
    suspend fun act(reportId: String, action: String) {
        callFunction("adminAction", mapOf("reportId" to reportId, "action" to action))
    }
}

// ---- Online-Status ----------------------------------------------------------

/** Meldet "ich bin gerade in der App" (alle 2 Minuten); nur Freunde dürfen es lesen. */
object PresenceRepo {
    private fun ref(uid: String) =
        Fb.db.collection("users").document(uid).collection("private").document("presence")

    fun ping() {
        ref(Fb.uid).set(mapOf("lastActive" to FieldValue.serverTimestamp()))
            .addOnFailureListener { android.util.Log.w("turp", "Online-Status nicht gespeichert", it) }
    }

    fun lastActive(uid: String): Flow<Timestamp?> =
        ref(uid).asFlow().map { if (it.exists()) it.getTimestamp("lastActive") else null }
}
