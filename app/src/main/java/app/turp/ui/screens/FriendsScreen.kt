package app.turp.ui.screens

import app.turp.ui.components.InterestIcon
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import app.turp.ui.components.TabHeader
import app.turp.data.Fb
import app.turp.data.ChatRepo
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.foundation.layout.statusBarsPadding
import app.turp.ui.theme.Online
import app.turp.ui.components.rememberIsOnline
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.R
import app.turp.data.BaseViewModel
import app.turp.data.Chat
import app.turp.data.DEFAULT_COLOR
import app.turp.data.FRIEND_BONUS
import app.turp.data.FriendRequest
import app.turp.data.INVITE_BONUS
import app.turp.data.ProfileRepo
import app.turp.data.RequestRepo
import app.turp.data.SessionState
import app.turp.data.UserProfile
import app.turp.ui.components.Avatar
import app.turp.ui.components.Countries
import app.turp.ui.components.EmptyState
import app.turp.ui.components.GemPill
import app.turp.ui.components.darken
import app.turp.ui.components.parseColor
import app.turp.ui.components.readableOn
import app.turp.ui.components.timeAgo
import app.turp.ui.components.toast
import app.turp.ui.components.todayBerlin
import app.turp.ui.theme.Display
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Sky
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

// ---- Freunde & Anfragen ------------------------------------------------------

/** Schillernde Farben für den "Top Match" */
private val Holo = listOf(Teal, Sky, Lilac, Color(0xFFFF7AB6), Sun, Teal)

class RequestsViewModel : BaseViewModel() {
    var processing by mutableStateOf(setOf<String>())
        private set

    /** true = nach gemeinsamen Interessen sortieren, false = neueste zuerst */
    var sortByInterest by mutableStateOf(true)

    /** Profile der Absender (für Interessen) – jedes wird nur einmal geladen */
    var senders by mutableStateOf<Map<String, UserProfile>>(emptyMap())
        private set
    private val requested = mutableSetOf<String>()

    fun loadSenders(uids: Collection<String>) {
        val missing = uids.filter { requested.add(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val loaded = missing.map { uid ->
                async {
                    uid to try {
                        ProfileRepo.get(uid)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll()
            senders = senders + loaded.mapNotNull { (uid, p) -> p?.let { uid to it } }
        }
    }

    /** Annehmen oder ablehnen – ohne Seitenwechsel, damit man mehrere nacheinander abarbeiten kann */
    fun respond(request: FriendRequest, accept: Boolean) {
        if (request.id in processing) return
        processing = processing + request.id
        launchSafe(showBusy = false) {
            try {
                RequestRepo.respond(request.id, accept)
                if (accept) toast("🎉 Du und ${request.fromName} seid jetzt Freunde! +$FRIEND_BONUS 💎")
            } finally {
                processing = processing - request.id
            }
        }
    }
}

@Composable
fun FriendsScreen(
    session: SessionState.Active,
    incoming: List<FriendRequest>,
    chats: List<Chat>,
    onOpenUser: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onDiscover: () -> Unit,
    onAllFriends: () -> Unit,
    onOpenShop: () -> Unit,
    vm: RequestsViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    LaunchedEffect(incoming) { vm.loadSenders(incoming.map { it.from }) }

    val me by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState()
    val prefs = remember { context.getSharedPreferences("turp", Context.MODE_PRIVATE) }
    var inviteHidden by remember { mutableStateOf(prefs.getBoolean("inviteRowHidden", false)) }

    // Gemeinsame Interessen mit jeder Person, die angefragt hat
    val myInterests = me?.interests.orEmpty().toSet()
    val senders = vm.senders
    val common: Map<String, Set<String>> = incoming.associate { r ->
        r.id to senders[r.from]?.interests.orEmpty().filter { it in myInterests }.toSet()
    }
    val sorted = if (vm.sortByInterest) {
        incoming.sortedWith(
            compareByDescending<FriendRequest> { common[it.id]?.size ?: 0 }.thenByDescending { it.createdAt }
        )
    } else {
        incoming
    }
    val bestId = incoming
        .maxByOrNull { common[it.id]?.size ?: 0 }
        ?.takeIf { (common[it.id]?.size ?: 0) > 0 }
        ?.id

    // Neue Freunde: neueste Freundschaft zuerst
    val friends = chats.filter { !it.closed }.sortedByDescending { it.createdAt ?: it.lastAt }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            TabHeader(
                title = "Freunde",
                me = me,
                gems = session.account.gems,
                showDot = session.account.lastCheckIn != todayBerlin(),
                onOpenProfile = onOpenProfile,
                onOpenShop = onOpenShop,
            ) {
                IconButton(onClick = onAllFriends) {
                    Icon(Icons.Rounded.Group, contentDescription = "Alle Freunde", tint = TextHigh, modifier = Modifier.size(25.dp))
                }
            }
        }

        item { Heading("Neue Freunde", Modifier.padding(start = 16.dp, top = 6.dp, bottom = 10.dp)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (friends.isEmpty()) item { FindFriendsCard(onDiscover) }
                items(friends, key = { it.id }) { chat ->
                    NewFriendCard(chat = chat, me = session.uid, onClick = { onOpenChat(chat.id) })
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Heading("Anfragen", Modifier.weight(1f))
                SortChip("Neueste", selected = !vm.sortByInterest) { vm.sortByInterest = false }
                Spacer(Modifier.width(6.dp))
                SortChip("Interessen", selected = vm.sortByInterest) { vm.sortByInterest = true }
            }
        }

        val code = session.account.inviteCode
        if (!inviteHidden && code.isNotBlank()) {
            item(key = "invite") {
                TeamInviteRow(
                    onShare = { shareInvite(context, code) },
                    onHide = {
                        inviteHidden = true
                        prefs.edit().putBoolean("inviteRowHidden", true).apply()
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        if (incoming.isEmpty()) {
            item {
                EmptyState(
                    emoji = "👋",
                    title = "Noch keine Anfragen",
                    text = "Wenn dich jemand hinzufügen möchte, siehst du es hier.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(sorted, key = { it.id }) { r ->
            RequestRow(
                request = r,
                interests = senders[r.from]?.interests.orEmpty(),
                shared = common[r.id].orEmpty(),
                best = r.id == bestId,
                busy = r.id in vm.processing,
                onOpen = { onOpenUser(r.from) },
                onAccept = { vm.respond(r, true) },
                onDecline = { vm.respond(r, false) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TextHigh, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier)
}

/** Hochkant-Karte mit Foto und Name – tippen öffnet den Chat */
@Composable
private fun NewFriendCard(chat: Chat, me: String, onClick: () -> Unit) {
    val info = chat.other(me)
    val accent = parseColor(info.color)
    val shape = RoundedCornerShape(18.dp)
    val online = rememberIsOnline(chat.otherUid(me))
    Box(
        Modifier
            .size(width = 104.dp, height = 142.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent, darken(accent, 0.45f))))
            .clickable(onClick = onClick)
    ) {
        if (info.photo.isNotBlank()) {
            AsyncImage(
                model = info.photo,
                contentDescription = info.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                info.name.take(1).uppercase(),
                style = TextStyle(fontFamily = Display, fontSize = 46.sp),
                color = readableOn(accent),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)))
        )
        if (online) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(12.dp)
                    .background(Online, CircleShape)
                    .border(2.dp, Ink, CircleShape)
            )
        }
        Text(
            info.name,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 9.dp, vertical = 8.dp)
        )
        if (chat.isUnread(me)) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(11.dp)
                    .background(Teal, CircleShape)
                    .border(2.dp, Ink, CircleShape)
            )
        }
    }
}

/** Platzhalter, solange es noch keine Freunde gibt */
@Composable
private fun FindFriendsCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .size(width = 104.dp, height = 142.dp)
            .clip(shape)
            .background(InkElevated)
            .border(1.5.dp, InkLine, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(Teal.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Teal)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Finde neue Freunde",
            color = TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (selected) Teal else InkHigh) {
        Text(
            text,
            color = if (selected) Ink else TextHigh,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

/** Angepinnte Zeile vom kumi-Team: Freunde einladen und Gems bekommen */
@Composable
private fun TeamInviteRow(onShare: () -> Unit, onHide: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onShare)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.width(40.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("kumi Team", color = TextHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Lade Freunde ein: +$INVITE_BONUS 💎 pro Person", color = TextLow, fontSize = 13.sp)
        }
        IconButton(onClick = onHide) {
            Icon(Icons.Rounded.Close, contentDescription = "Ausblenden", tint = TextLow, modifier = Modifier.size(21.dp))
        }
        Surface(onClick = onShare, shape = CircleShape, color = Teal) {
            Text(
                "Teilen",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** Eine Anfrage: Foto, Name, Alter + Land, wie lange her, Interessen, ablehnen / annehmen */
@Composable
private fun RequestRow(
    request: FriendRequest,
    interests: List<String>,
    shared: Set<String>,
    best: Boolean,
    busy: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = if (best) 10.dp else 0.dp, bottom = if (best) 4.dp else 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (best) {
                        Modifier
                            .background(InkElevated, shape)
                            .background(Brush.linearGradient(Holo.map { it.copy(alpha = 0.22f) }), shape)
                            .border(2.dp, Brush.linearGradient(Holo), shape)
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(request.fromPhoto, request.fromName, request.fromColor, 48.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        request.fromName,
                        color = TextHigh,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${request.fromAge}, ${Countries.flag(request.fromCountry)} ${request.fromCountry}",
                        color = TextHigh,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier
                            .background(InkHigh, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gemeinsame Interessen zuerst und hervorgehoben
                    val ordered = interests.sortedByDescending { it in shared }.take(5)
                    ordered.forEach { interest ->
                        InterestEmoji(interest, shared = interest in shared)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        timeAgo(request.createdAt),
                        color = TextLow,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = if (ordered.isEmpty()) 0.dp else 3.dp)
                    )
                }
            }
            IconButton(onClick = onDecline, enabled = !busy) {
                Icon(Icons.Rounded.Close, contentDescription = "Ablehnen", tint = TextHigh, modifier = Modifier.size(25.dp))
            }
            IconButton(onClick = onAccept, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = "Annehmen", tint = Teal, modifier = Modifier.size(27.dp))
                }
            }
        }
        if (best) {
            TopMatchBadge(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = (-10).dp)
            )
        }
    }
}

@Composable
private fun InterestEmoji(interest: String, shared: Boolean) {
    Box(
        Modifier
            .size(28.dp)
            .background(if (shared) Teal.copy(alpha = 0.28f) else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        InterestIcon(interest, 26.dp)
    }
}

/** Sticker für die Anfrage mit den meisten gemeinsamen Interessen */
@Composable
private fun TopMatchBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        "Top Match",
        style = TextStyle(fontFamily = Display, fontSize = 12.sp),
        color = Ink,
        modifier = modifier
            .graphicsLayer { rotationZ = 7f }
            .shadow(6.dp, shape)
            .background(Color.White, shape)
            .border(1.5.dp, Brush.linearGradient(Holo), shape)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    )
}

// ---- Alle Freunde ----------------------------------------------------------

class AllFriendsViewModel : BaseViewModel() {
    val chats = ChatRepo.chats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Profile der Freunde (für Alter und Land) – jedes wird nur einmal geladen */
    var profiles by mutableStateOf<Map<String, UserProfile>>(emptyMap())
        private set
    private val requested = mutableSetOf<String>()

    fun load(uids: Collection<String>) {
        val missing = uids.filter { requested.add(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val loaded = missing.map { uid ->
                async {
                    uid to try {
                        ProfileRepo.get(uid)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll()
            profiles = profiles + loaded.mapNotNull { (uid, p) -> p?.let { uid to it } }
        }
    }
}

@Composable
fun AllFriendsScreen(
    onClose: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    vm: AllFriendsViewModel = viewModel(),
) {
    val me = Fb.uid
    val chats by vm.chats.collectAsState()
    val friends = chats.filter { !it.closed }.sortedBy { it.other(me).name.lowercase() }
    LaunchedEffect(friends.size) { vm.load(friends.map { it.otherUid(me) }) }
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) friends else friends.filter { it.other(me).name.contains(query.trim(), ignoreCase = true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Rounded.Close, contentDescription = "Schließen", tint = TextHigh)
            }
            Text(
                "Alle Freunde 👯",
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        TextField(
            value = query,
            onValueChange = { query = it.take(30) },
            placeholder = { Text("Nach Namen suchen") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = InkHigh,
                unfocusedContainerColor = InkHigh,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "👯",
                        title = if (friends.isEmpty()) "Noch keine Freunde" else "Niemand gefunden",
                        text = if (friends.isEmpty()) "Nimm Anfragen an oder schick selbst welche beim Entdecken." else "Probier einen anderen Namen.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            items(shown, key = { it.id }) { chat ->
                val uid = chat.otherUid(me)
                val info = chat.other(me)
                val profile = vm.profiles[uid]
                val online = rememberIsOnline(uid)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUser(uid) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(info.photo, info.name, info.color, 46.dp, online = online)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            info.name,
                            color = TextHigh,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile != null) {
                            Text(
                                if (profile.bot) "🤖 Testbot" else "${profile.age}, ${Countries.flag(profile.country)} ${profile.country}",
                                color = TextLow,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Surface(onClick = { onOpenChat(chat.id) }, shape = CircleShape, color = Teal) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Chat", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
