package app.turp.ui.screens

import app.turp.ui.components.VoicePlayer
import app.turp.ui.components.VoiceMessageContent
import app.turp.ui.components.RecordingBar
import app.turp.ui.components.InAppCamera
import app.turp.data.VoiceRecorder
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.DisposableEffect
import android.content.pm.PackageManager
import android.Manifest
import java.io.File
import app.turp.ui.components.rememberIsOnline
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import com.google.firebase.Timestamp
import app.turp.ui.components.sameDay
import app.turp.ui.components.lastOnline
import app.turp.ui.components.dayLabel
import app.turp.ui.components.ONLINE_WINDOW_MS
import app.turp.data.PresenceRepo
import app.turp.data.MemberInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.produceState
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.BaseViewModel
import app.turp.data.Chat
import app.turp.data.ChatRepo
import app.turp.data.DEFAULT_COLOR
import app.turp.data.Fb
import app.turp.data.FriendRequest
import app.turp.data.Message
import app.turp.data.RequestRepo
import app.turp.data.SafetyRepo
import app.turp.ui.components.Avatar
import app.turp.ui.components.CandyButton
import app.turp.ui.components.ConfirmDialog
import app.turp.ui.components.Countries
import app.turp.ui.components.EmptyState
import app.turp.ui.components.MessageInput
import app.turp.ui.components.ReportDialog
import app.turp.ui.components.ScreenHeader
import app.turp.ui.components.SectionTitle
import app.turp.ui.components.parseColor
import app.turp.ui.components.shortTime
import app.turp.ui.components.timeOfDay
import app.turp.ui.components.toast
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---- Chat-Liste -----------------------------------------------------------

@Composable
fun ChatsScreen(
    chats: List<Chat>,
    me: String,
    onOpenChat: (String) -> Unit,
    onDiscover: () -> Unit,
    onStartTestChat: (() -> Unit)? = null,
    testBusy: Boolean = false,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
        item { ScreenHeader("Chats") }
        if (onStartTestChat != null) {
            item {
                Surface(
                    onClick = onStartTestChat,
                    enabled = !testBusy,
                    shape = RoundedCornerShape(16.dp),
                    color = InkElevated,
                    border = BorderStroke(1.dp, Teal.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Test-Chat mit dem turp Bot", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Antwortet sofort – nur in der Test-Version sichtbar",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLow
                            )
                        }
                        if (testBusy) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Teal)
                        } else {
                            Text("Start", color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (chats.isEmpty()) {
            item {
                EmptyState(
                    emoji = "💬",
                    title = "Noch keine Chats",
                    text = "Sobald jemand deine Anfrage annimmt, könnt ihr hier schreiben.",
                    actionLabel = "Leute entdecken",
                    onAction = onDiscover
                )
            }
        }
        items(chats, key = { it.id }) { chat ->
            ChatRow(chat, me) { onOpenChat(chat.id) }
        }
    }
}

@Composable
private fun ChatRow(chat: Chat, me: String, onClick: () -> Unit) {
    val other = chat.other(me)
    val unread = chat.isUnread(me)
    val isOnline = rememberIsOnline(chat.otherUid(me))
    val online = !chat.closed && isOnline
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(other.photo, other.name, other.color, 54.dp, online = online)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(other.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (chat.closed) "Chat geschlossen" else chat.lastMessage,
                color = if (unread) TextHigh else TextLow,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(shortTime(chat.lastAt), style = MaterialTheme.typography.labelSmall, color = TextLow)
            if (unread) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(10.dp)
                        .background(Coral, CircleShape)
                )
            }
        }
    }
}

// ---- Einzelner Chat -------------------------------------------------------

class ChatViewModel(private val chatId: String) : BaseViewModel() {
    val chat = ChatRepo.chat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val messages = ChatRepo.messages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var sendingImage by mutableStateOf(false)
        private set

    fun send(text: String) {
        if (text.isBlank()) return
        launchSafe(showBusy = false) { ChatRepo.sendText(chatId, text.take(1000)) }
    }

    var sendingAudio by mutableStateOf(false)
        private set

    fun sendAudio(file: File, durationMs: Long) {
        sendingAudio = true
        launchSafe(showBusy = false) {
            try {
                ChatRepo.sendAudio(chatId, file, durationMs)
            } finally {
                sendingAudio = false
            }
        }
    }

    fun sendImage(appContext: Context, uri: Uri) {
        if (sendingImage) return
        sendingImage = true
        launchSafe(showBusy = false) {
            try {
                ChatRepo.sendImage(appContext, chatId, uri)
            } finally {
                sendingImage = false
            }
        }
    }

    fun markRead() {
        viewModelScope.launch {
            try {
                ChatRepo.markRead(chatId)
            } catch (_: Exception) {
            }
        }
    }

    fun report(targetUid: String, reason: String, details: String, context: String) = launchSafe {
        SafetyRepo.report(targetUid, reason, details, context)
        toast("Danke! Unser Team schaut sich das an. 💛")
    }

    fun block(targetUid: String, onDone: () -> Unit) = launchSafe {
        SafetyRepo.block(targetUid)
        toast("Blockiert. Ihr könnt euch nicht mehr schreiben.")
        onDone()
    }
}

@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit, onOpenUser: (String) -> Unit) {
    val vm: ChatViewModel = viewModel(key = "chat_$chatId") { ChatViewModel(chatId) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val chat by vm.chat.collectAsState()
    val messages by vm.messages.collectAsState()
    val me = Fb.uid
    val current = chat
    val otherUid = current?.otherUid(me).orEmpty()
    val other = current?.other(me)
    val otherName = other?.name.orEmpty()
    val isBot = otherUid.startsWith("turp-bot-")

    // Zuletzt online (nur für Freunde sichtbar) – jede Minute neu berechnen
    val lastActive by remember(otherUid) {
        if (otherUid.isEmpty() || isBot) flowOf(null) else PresenceRepo.lastActive(otherUid)
    }.collectAsState(initial = null)
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }
    val seen = lastActive
    val online = isBot || (seen != null && now - seen.toDate().time < ONLINE_WINDOW_MS)
    val status = when {
        online -> "Online"
        seen != null -> "Zuletzt online ${lastOnline(seen)}"
        else -> ""
    }

    // Lesebestätigung: kleines Profilbild an meiner letzten Nachricht, die gelesen wurde
    val otherRead = current?.readAt?.get(otherUid)
    val lastReadId = if (otherRead == null) {
        null
    } else {
        messages.lastOrNull { it.senderId == me && it.createdAt != null && it.createdAt <= otherRead }?.id
    }

    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var reportContext by remember { mutableStateOf<String?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.sendImage(context.applicationContext, uri)
    }

    // Eigene Kamera (Selfies werden nicht gespiegelt)
    var showCamera by remember { mutableStateOf(false) }

    // Sprachnachrichten: aufnehmen und abspielen
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    val voice = remember { VoicePlayer(context.applicationContext) }
    var recording by remember { mutableStateOf(false) }
    var recordMs by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            voice.release()
        }
    }
    fun finishRecording(send: Boolean) {
        if (!recording) return
        recording = false
        if (!send) {
            recorder.cancel()
            return
        }
        val result = recorder.stop()
        if (result == null) context.toast("Zu kurz – sprich etwas länger.") else vm.sendAudio(result.first, result.second)
    }
    fun startRecording() {
        if (recorder.start()) {
            recordMs = 0L
            recording = true
        } else {
            context.toast("Aufnahme nicht möglich.")
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startRecording() else context.toast("Ohne Mikrofon-Erlaubnis geht das leider nicht.")
    }
    LaunchedEffect(recording) {
        while (recording) {
            recordMs = recorder.elapsedMs
            if (recordMs >= VoiceRecorder.MAX_MS) finishRecording(send = true)
            delay(200)
        }
    }
    LaunchedEffect(voice.isPlaying) {
        while (voice.isPlaying) {
            voice.tick()
            delay(80)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Index 0 ist der Sicherheitshinweis, danach folgen die Nachrichten
            listState.animateScrollToItem(messages.size)
            vm.markRead()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Ink,
        topBar = {
            // Zurück | Profilbild + Name + zuletzt online | Menü
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Ink)
                    .statusBarsPadding()
                    .padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = "Zurück",
                        tint = TextHigh,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = otherUid.isNotEmpty()) { onOpenUser(otherUid) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(other?.photo.orEmpty(), otherName, other?.color ?: DEFAULT_COLOR, 44.dp, online = online)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            otherName,
                            color = TextHigh,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (status.isNotEmpty()) {
                            Text(
                                status,
                                color = if (online) Teal else TextLow,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, enabled = otherUid.isNotEmpty()) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Mehr", tint = TextHigh, modifier = Modifier.size(26.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Profil ansehen") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenUser(otherUid)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Melden") },
                            leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                reportContext = ""
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Blockieren", color = Coral) },
                            leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, tint = Coral) },
                            onClick = {
                                menuOpen = false
                                confirmBlock = true
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (current?.closed == true) {
                Surface(color = InkElevated, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Dieser Chat ist geschlossen.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(18.dp),
                        textAlign = TextAlign.Center,
                        color = TextLow
                    )
                }
            } else if (recording) {
                RecordingBar(
                    elapsedMs = recordMs,
                    onCancel = { finishRecording(send = false) },
                    onSend = { finishRecording(send = true) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                )
            } else {
                MessageInput(
                    onSend = vm::send,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                    enabled = current != null,
                    onPickImage = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    imageBusy = vm.sendingImage || vm.sendingAudio,
                    onCamera = { showCamera = true },
                    onMic = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
        ) {
            item {
                Text(
                    "🛡️ Teile keine Adresse, Passwörter oder Nacktbilder und triff dich nie allein mit Leuten aus dem Internet. " +
                        "Halte eine Nachricht gedrückt, um sie zu melden.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextLow,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(messages, key = { _, m -> m.id }) { i, m ->
                val prev = messages.getOrNull(i - 1)
                val next = messages.getOrNull(i + 1)
                val newDay = prev == null || !sameDay(prev.createdAt, m.createdAt)
                val firstInRun = newDay || prev?.senderId != m.senderId
                val lastInRun = next == null || next.senderId != m.senderId || !sameDay(m.createdAt, next.createdAt)
                val mine = m.senderId == me
                Column {
                    if (newDay) DateChip(m.createdAt)
                    MessageRow(
                        message = m,
                        mine = mine,
                        firstInRun = firstInRun,
                        lastInRun = lastInRun,
                        other = other,
                        showRead = m.id == lastReadId,
                        voice = voice,
                        onLongPress = {
                            if (!mine) {
                                reportContext = when (m.type) {
                                    "text" -> m.text
                                    "audio" -> "[Sprachnachricht]"
                                    else -> "[Foto]"
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCamera) {
        InAppCamera(
            onClose = { showCamera = false },
            onPhoto = { uri ->
                showCamera = false
                vm.sendImage(context.applicationContext, uri)
            }
        )
    }
    }

    val ctx = reportContext
    if (ctx != null && otherUid.isNotEmpty()) {
        ReportDialog(
            name = otherName,
            onDismiss = { reportContext = null },
            onSubmit = { reason, details ->
                vm.report(otherUid, reason, details, ctx)
                reportContext = null
            }
        )
    }
    if (confirmBlock) {
        ConfirmDialog(
            title = "$otherName blockieren?",
            text = "Ihr könnt euch dann nicht mehr sehen oder schreiben, und der Chat wird geschlossen.",
            confirmLabel = "Blockieren",
            onDismiss = { confirmBlock = false },
            onConfirm = {
                confirmBlock = false
                vm.block(otherUid) { onBack() }
            }
        )
    }
}

/** Datums-Trenner in der Mitte, z. B. "Heute" oder "03. Okt. 2026" */
@Composable
private fun DateChip(ts: Timestamp?) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            dayLabel(ts),
            color = TextLow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(InkHigh, CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/**
 * Eine Nachricht. Bei der anderen Person steht links ihr Profilbild (an der letzten
 * Nachricht einer Reihe), bei mir zeigt ein kleines Profilbild, bis wohin gelesen wurde.
 */
@Composable
private fun MessageRow(
    message: Message,
    mine: Boolean,
    firstInRun: Boolean,
    lastInRun: Boolean,
    other: MemberInfo?,
    showRead: Boolean,
    voice: VoicePlayer,
    onLongPress: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (firstInRun) 8.dp else 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mine) {
            if (lastInRun && other != null) {
                Avatar(other.photo, other.name, other.color, 32.dp)
            } else {
                Spacer(Modifier.width(32.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Box {
            Bubble(message, mine, lastInRun, voice, onLongPress)
            if (showRead && other != null) {
                Avatar(
                    other.photo,
                    other.name,
                    other.color,
                    16.dp,
                    ring = Ink,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 7.dp, y = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Bubble(message: Message, mine: Boolean, lastInRun: Boolean, voice: VoicePlayer, onLongPress: () -> Unit) {
    val round = 20.dp
    val tail = 6.dp
    val shape = RoundedCornerShape(
        topStart = round,
        topEnd = round,
        bottomStart = if (!mine && lastInRun) tail else round,
        bottomEnd = if (mine && lastInRun) tail else round
    )
    val fg = if (mine) Ink else TextHigh
    val timeColor = if (mine) Ink.copy(alpha = 0.55f) else TextLow
    val time = timeOfDay(message.createdAt)
    Box(
        Modifier
            .widthIn(max = 280.dp)
            .clip(shape)
            .background(if (mine) Teal else InkHigh)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    ) {
        when {
            message.type == "image" && message.imageStatus == "ok" -> Box {
                AsyncImage(
                    model = message.imageUrl,
                    contentDescription = "Foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(220.dp)
                )
                Text(
                    time,
                    color = Color.White,
                    fontSize = 11.sp,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1f), 4f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            message.type == "audio" -> VoiceMessageContent(
                id = message.id,
                status = when {
                    message.audioStatus == "ok" && message.audioUrl.isNotBlank() -> "ok"
                    message.audioStatus == "blocked" -> "blocked"
                    else -> "pending"
                },
                durationMs = message.durationMs,
                playing = voice.playingId == message.id && voice.isPlaying,
                progress = if (voice.playingId == message.id) voice.progress else 0f,
                mine = mine,
                time = time,
                onToggle = { voice.toggle(message.id, message.audioUrl) }
            )
            message.type == "image" && message.imageStatus == "blocked" ->
                BubbleText("🚫 Foto wurde blockiert", time, fg, timeColor, italic = true)
            message.type == "image" -> BubbleText("📷 Foto wird geprüft…", time, fg, timeColor, italic = true)
            else -> BubbleText(message.text, time, fg, timeColor)
        }
    }
}

/** Text mit der Uhrzeit klein unten rechts in der Blase */
@Composable
private fun BubbleText(text: String, time: String, color: Color, timeColor: Color, italic: Boolean = false) {
    Row(
        Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 7.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text,
            color = color,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(time, color = timeColor, fontSize = 11.sp, modifier = Modifier.padding(bottom = 1.dp))
    }
}
