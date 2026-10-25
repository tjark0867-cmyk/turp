package app.turp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

const val DEFAULT_COLOR = "#2DE1C2"
// Spielregeln – müssen zu functions/index.js passen
const val REQUEST_COST = 10
const val PLUS_REQUEST_COST = 5     // mit kumi+
const val MAX_PHOTOS = 6
const val MAX_INTERESTS = 8
const val FRIEND_BONUS = 5
const val INVITE_BONUS = 50         // Gems pro eingeladener Person

/** Was eine Freundschaftsanfrage kostet */
fun requestCost(plus: Boolean): Int = if (plus) PLUS_REQUEST_COST else REQUEST_COST

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val age: Int = 0,
    val country: String = "",
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val videoUrl: String = "",
    val color: String = DEFAULT_COLOR,
    val ageGroup: String = "",
    val bot: Boolean = false,
    /** hat kumi+ (für das goldene Plus am Namen) */
    val plus: Boolean = false,
    /** "male", "female", "other" oder leer (ältere Konten) */
    val gender: String = "",
)

data class Account(
    val status: String = "",
    val ageGroup: String = "",
    val gems: Long = 0,
    val streak: Long = 0,
    val lastCheckIn: String = "",
    val inviteCode: String = "",
    val parentEmail: String = "",
    val plus: Boolean = false,
    val fcmToken: String = "",
    val plusUntil: Timestamp? = null,
    /** Sprache für Push-Benachrichtigungen */
    val lang: String = "",
    /** Briefe: jemandem schreiben, bevor er angenommen hat */
    val letters: Long = 0,
    /** Start-Geschenk (5 Briefe) schon abgeholt */
    val letterGift: Boolean = false,
) {
    /** kumi+ gerade aktiv (auch wenn der Tagesjob ein abgelaufenes Abo noch nicht beendet hat) */
    val plusActive: Boolean
        get() = plus && (plusUntil == null || plusUntil.toDate().time > System.currentTimeMillis())
}

data class Socials(
    val instagram: String = "",
    val snapchat: String = "",
    val tiktok: String = "",
    val visible: Boolean = false,
)

data class FriendRequest(
    val id: String,
    val from: String,
    val to: String,
    val status: String,
    val fromName: String,
    val fromPhoto: String,
    val fromColor: String,
    val fromAge: Int,
    val fromCountry: String,
    val toName: String,
    val toPhoto: String,
    val toColor: String,
    val createdAt: Timestamp?,
    /** Anfrage als Brief (mit Chat und erster Nachricht) */
    val letter: Boolean = false,
)

data class MemberInfo(val name: String, val photo: String, val color: String)

/** Eintrag in der Liste "Blockierte Nutzer" (Name/Bild vom Zeitpunkt des Blockierens) */
data class BlockedUser(
    val uid: String,
    val name: String,
    val photo: String,
    val color: String,
    val at: Timestamp?,
)

data class Chat(
    val id: String,
    val members: List<String>,
    val closed: Boolean,
    val lastMessage: String,
    val lastAt: Timestamp?,
    val lastSender: String,
    val readAt: Map<String, Timestamp>,
    val memberInfo: Map<String, MemberInfo>,
    val createdAt: Timestamp? = null,
    /** Brief-Chat: noch nicht angenommen – [pendingFrom] hat geschrieben */
    val pending: Boolean = false,
    val pendingFrom: String = "",
) {
    /** Ein offener Brief, den ich bekommen habe und noch annehmen/ablehnen muss */
    fun letterFor(me: String): Boolean = pending && pendingFrom != me && pendingFrom.isNotEmpty()

    fun otherUid(me: String): String = members.firstOrNull { it != me } ?: ""

    fun other(me: String): MemberInfo =
        memberInfo[otherUid(me)] ?: MemberInfo(t("Unbekannt"), "", DEFAULT_COLOR)

    fun isUnread(me: String): Boolean {
        val last = lastAt ?: return false
        if (lastSender.isEmpty() || lastSender == me) return false
        val read = readAt[me] ?: return true
        return last > read
    }
}

data class Message(
    val id: String,
    val senderId: String,
    val type: String,
    val text: String,
    val imageUrl: String,
    val imageStatus: String,
    val createdAt: Timestamp?,
    val audioUrl: String = "",
    val audioStatus: String = "",
    val durationMs: Long = 0,
    val imagePath: String = "",
)

data class RoomMessage(
    val id: String,
    val senderId: String,
    val name: String,
    val photo: String,
    val color: String,
    val text: String,
    val createdAt: Timestamp?,
)

data class Report(
    val id: String,
    val reporter: String,
    val target: String,
    val targetName: String,
    val reason: String,
    val details: String,
    val context: String,
    val ageGroup: String,
    val priority: Long,
    val createdAt: Timestamp?,
)

// ---- Umwandlung von Firestore-Dokumenten ----------------------------------

private fun DocumentSnapshot.s(field: String): String = get(field) as? String ?: ""
private fun DocumentSnapshot.n(field: String): Long = (get(field) as? Number)?.toLong() ?: 0L
private fun DocumentSnapshot.b(field: String): Boolean = get(field) as? Boolean ?: false
private fun DocumentSnapshot.list(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

private fun DocumentSnapshot.ts(field: String): Timestamp? =
    get(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) as? Timestamp

fun DocumentSnapshot.toProfile() = UserProfile(
    uid = id,
    name = s("name"),
    age = n("age").toInt(),
    country = s("country"),
    bio = s("bio"),
    interests = list("interests"),
    photos = list("photos"),
    videoUrl = s("videoUrl"),
    color = s("color").ifBlank { DEFAULT_COLOR },
    ageGroup = s("ageGroup"),
    bot = b("bot"),
    plus = b("plus"),
    gender = s("gender"),
)

fun DocumentSnapshot.toAccount() = Account(
    status = s("status"),
    ageGroup = s("ageGroup"),
    gems = n("gems"),
    streak = n("streak"),
    lastCheckIn = s("lastCheckIn"),
    inviteCode = s("inviteCode"),
    parentEmail = s("parentEmail"),
    plus = b("plus"),
    fcmToken = s("fcmToken"),
    plusUntil = ts("plusUntil"),
    lang = s("lang"),
    letters = n("letters"),
    letterGift = b("letterGift"),
)

fun DocumentSnapshot.toSocials() = Socials(
    instagram = s("instagram"),
    snapchat = s("snapchat"),
    tiktok = s("tiktok"),
    visible = b("visible"),
)

fun DocumentSnapshot.toRequest() = FriendRequest(
    id = id,
    from = s("from"),
    to = s("to"),
    status = s("status"),
    fromName = s("fromName"),
    fromPhoto = s("fromPhoto"),
    fromColor = s("fromColor").ifBlank { DEFAULT_COLOR },
    fromAge = n("fromAge").toInt(),
    fromCountry = s("fromCountry"),
    toName = s("toName"),
    toPhoto = s("toPhoto"),
    toColor = s("toColor").ifBlank { DEFAULT_COLOR },
    createdAt = ts("createdAt"),
    letter = b("letter"),
)

fun DocumentSnapshot.toChat(): Chat {
    val read = (get("readAt") as? Map<*, *>)
        ?.mapNotNull { (k, v) -> if (k is String && v is Timestamp) k to v else null }
        ?.toMap() ?: emptyMap()
    val info = (get("memberInfo") as? Map<*, *>)
        ?.mapNotNull { (k, v) ->
            val m = v as? Map<*, *>
            if (k is String && m != null) {
                k to MemberInfo(
                    name = m["name"] as? String ?: "",
                    photo = m["photo"] as? String ?: "",
                    color = (m["color"] as? String).orEmpty().ifBlank { DEFAULT_COLOR },
                )
            } else null
        }
        ?.toMap() ?: emptyMap()
    return Chat(
        id = id,
        members = list("members"),
        closed = b("closed"),
        lastMessage = s("lastMessage"),
        lastAt = ts("lastAt"),
        lastSender = s("lastSender"),
        readAt = read,
        memberInfo = info,
        createdAt = ts("createdAt"),
        pending = b("pending"),
        pendingFrom = s("pendingFrom"),
    )
}

fun DocumentSnapshot.toMessage() = Message(
    id = id,
    senderId = s("senderId"),
    type = s("type").ifBlank { "text" },
    text = s("text"),
    imageUrl = s("imageUrl"),
    imageStatus = s("imageStatus"),
    createdAt = ts("createdAt"),
    audioUrl = s("audioUrl"),
    audioStatus = s("audioStatus"),
    durationMs = n("durationMs"),
    imagePath = s("imagePath"),
)

fun DocumentSnapshot.toRoomMessage() = RoomMessage(
    id = id,
    senderId = s("senderId"),
    name = s("name"),
    photo = s("photo"),
    color = s("color").ifBlank { DEFAULT_COLOR },
    text = s("text"),
    createdAt = ts("createdAt"),
)

fun DocumentSnapshot.toReport() = Report(
    id = id,
    reporter = s("reporter"),
    target = s("target"),
    targetName = s("targetName"),
    reason = s("reason"),
    details = s("details"),
    context = s("context"),
    ageGroup = s("ageGroup"),
    priority = n("priority"),
    createdAt = ts("createdAt"),
)

fun DocumentSnapshot.toBlocked() = BlockedUser(
    uid = id,
    name = s("name"),
    photo = s("photo"),
    color = s("color").ifBlank { DEFAULT_COLOR },
    at = ts("at"),
)
