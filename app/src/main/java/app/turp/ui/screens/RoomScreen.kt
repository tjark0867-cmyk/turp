package app.turp.ui.screens

import app.turp.data.t

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.BaseViewModel
import app.turp.data.RoomMessage
import app.turp.data.RoomRepo
import app.turp.data.SafetyRepo
import app.turp.data.SessionState
import app.turp.ui.components.Avatar
import app.turp.ui.components.EmptyState
import app.turp.ui.components.MessageInput
import app.turp.ui.components.ReportDialog
import app.turp.ui.components.parseColor
import app.turp.ui.components.timeOfDay
import app.turp.ui.components.toast
import app.turp.ui.theme.Ink
import app.turp.ui.theme.cardboard
import app.turp.ui.theme.TornShape
import app.turp.ui.theme.PaperLight
import app.turp.ui.theme.Look
import app.turp.ui.theme.PageBg
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Night
import app.turp.ui.theme.Sun
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomViewModel : BaseViewModel() {
    private val group = MutableStateFlow("")

    val roomMessages = group
        .flatMapLatest { g -> if (g.isEmpty()) flowOf(emptyList()) else RoomRepo.messages(g) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var cooldown by mutableIntStateOf(0)
        private set

    fun setGroup(g: String) {
        group.value = g
    }

    fun post(text: String) {
        if (cooldown > 0 || text.isBlank()) return
        launchSafe(showBusy = false) {
            RoomRepo.post(text)
            startCooldown()
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            cooldown = 10
            while (cooldown > 0) {
                delay(1000)
                cooldown -= 1
            }
        }
    }

    fun report(message: RoomMessage, reason: String, details: String) = launchSafe {
        SafetyRepo.report(message.senderId, reason, details, t("Lounge: {0}", message.text))
        toast(t("Danke! Unser Team schaut sich das an. 💛"))
    }
}

@Composable
fun RoomScreen(
    session: SessionState.Active,
    onOpenUser: (String) -> Unit,
    vm: RoomViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(session.account.ageGroup) { vm.setGroup(session.account.ageGroup) }
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val messages by vm.roomMessages.collectAsState()
    val listState = rememberLazyListState()
    var reporting by remember { mutableStateOf<RoomMessage?>(null) }
    val isTeen = session.account.ageGroup == "teen"

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, PageBg)))
    ) {
        // Kopfbereich im gemütlichen Lounge-Look
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Lounge"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = Lilac.copy(alpha = 0.2f)) {
                    Text(
                        if (isTeen) "13–17" else "18+",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Lilac,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                t("Gruppenchat für deine Altersgruppe · Slow-Mode 10 s"),
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🛋️",
                        title = t("Noch ruhig hier"),
                        text = t("Sag Hallo und starte das Gespräch!")
                    )
                }
            }
            items(messages, key = { it.id }) { m ->
                RoomBubble(
                    message = m,
                    onOpenUser = { onOpenUser(m.senderId) },
                    onLongPress = { reporting = m }
                )
            }
        }

        MessageInput(
            onSend = vm::post,
            modifier = Modifier.imePadding(),
            placeholder = t("In die Lounge schreiben…"),
            sendHint = if (vm.cooldown > 0) "${vm.cooldown} s" else null
        )
    }

    val target = reporting
    if (target != null) {
        ReportDialog(
            name = target.name,
            onDismiss = { reporting = null },
            onSubmit = { reason, details ->
                vm.report(target, reason, details)
                reporting = null
            }
        )
    }
}

@Composable
private fun RoomBubble(message: RoomMessage, onOpenUser: () -> Unit, onLongPress: () -> Unit) {
    val accent = parseColor(message.color)
    Row(verticalAlignment = Alignment.Top) {
        Avatar(
            message.photo,
            message.name,
            message.color,
            34.dp,
            Modifier.clickable(onClick = onOpenUser)
        )
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier
                .then(
                    if (Look.cardboard) {
                        val paper = remember(message.id) { TornShape(message.id.hashCode(), jag = 1.5.dp, step = 6.dp) }
                        Modifier.cardboard(PaperLight, paper).clip(paper)
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                            .background(InkHigh.copy(alpha = 0.85f))
                    }
                )
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.name, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Text(timeOfDay(message.createdAt), color = TextLow, style = MaterialTheme.typography.labelSmall)
            }
            Text(message.text, color = if (message.text == "***") Sun else TextHigh)
        }
    }
}

