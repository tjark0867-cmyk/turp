package app.turp.ui.screens

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
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(other.photo, other.name, other.color, 54.dp)
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

    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var reportContext by remember { mutableStateOf<String?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.sendImage(context.applicationContext, uri)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Index 0 ist der Sicherheitshinweis, danach folgen die Nachrichten
            listState.animateScrollToItem(messages.size)
            vm.markRead()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        Modifier.clickable(enabled = otherUid.isNotEmpty()) { onOpenUser(otherUid) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(other?.photo.orEmpty(), otherName, other?.color ?: DEFAULT_COLOR, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(otherName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }, enabled = otherUid.isNotEmpty()) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Mehr")
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InkElevated)
            )
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
            } else {
                MessageInput(
                    onSend = vm::send,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                    enabled = current != null,
                    onPickImage = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    imageBusy = vm.sendingImage
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    "🛡️ Teile keine Adresse, Passwörter oder Nacktbilder und triff dich nie allein mit Leuten aus dem Internet. " +
                        "Halte eine Nachricht gedrückt, um sie zu melden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            items(messages, key = { it.id }) { m ->
                val mine = m.senderId == me
                MessageBubble(
                    message = m,
                    mine = mine,
                    onLongPress = {
                        if (!mine) reportContext = if (m.type == "text") m.text else "[Foto]"
                    }
                )
            }
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

@Composable
private fun MessageBubble(message: Message, mine: Boolean, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            val shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (mine) 20.dp else 6.dp,
                bottomEnd = if (mine) 6.dp else 20.dp
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(if (mine) Teal else InkHigh)
                    .combinedClickable(onClick = {}, onLongClick = onLongPress)
            ) {
                when {
                    message.type == "image" && message.imageStatus == "ok" -> AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(220.dp)
                    )
                    message.type == "image" && message.imageStatus == "blocked" ->
                        BubbleText("🚫 Foto wurde blockiert", mine, italic = true)
                    message.type == "image" -> BubbleText("📷 Foto wird geprüft…", mine, italic = true)
                    else -> BubbleText(message.text, mine)
                }
            }
            Text(
                timeOfDay(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun BubbleText(text: String, mine: Boolean, italic: Boolean = false) {
    Text(
        text,
        color = if (mine) Ink else TextHigh,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    )
}
