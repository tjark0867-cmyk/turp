package app.turp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

const val DEFAULT_COLOR = "#2DE1C2"
const val REQUEST_COST = 3
const val MAX_PHOTOS = 6
const val MAX_INTERESTS = 8
const val FRIEND_BONUS = 5          // muss zu functions/index.js passen
const val INVITE_BONUS = 25         // Gems pro eingeladener Person

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
)

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
)

data class MemberInfo(val name: String, val photo: String, val color: String)

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
) {
    fun otherUid(me: String): String = members.firstOrNull { it != me } ?: ""

    fun other(me: String): MemberInfo =
        memberInfo[otherUid(me)] ?: MemberInfo("Unbekannt", "", DEFAULT_COLOR)

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
    val gifId: String = "",
    val gifW: Int = 0,
    val gifH: Int = 0,
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
    gifId = s("gifId"),
    gifW = n("w").toInt(),
    gifH = n("h").toInt(),
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
