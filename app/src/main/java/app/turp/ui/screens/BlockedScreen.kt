package app.turp.ui.screens

import app.turp.data.t

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import app.turp.ui.components.Icon
import androidx.compose.material3.IconButton
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.BaseViewModel
import app.turp.data.BlockedUser
import app.turp.data.ProfileRepo
import app.turp.data.SafetyRepo
import app.turp.data.UserProfile
import app.turp.ui.components.Avatar
import app.turp.ui.components.ConfirmDialog
import app.turp.ui.components.EmptyState
import app.turp.ui.components.LoadingBox
import app.turp.ui.components.timeAgo
import app.turp.ui.components.toast
import app.turp.ui.theme.Ink
import app.turp.ui.theme.PageBg
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BlockedViewModel : BaseViewModel() {
    /** null = lädt noch */
    val blocked = SafetyRepo.blocked().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Für ältere Einträge ohne gespeicherten Namen: aktuelles Profil (falls sichtbar) */
    val profiles = mutableStateMapOf<String, UserProfile?>()

    /** Gerade in Arbeit (Knopf zeigt Ladekreis) */
    val working = mutableStateListOf<String>()

    fun loadMissing(list: List<BlockedUser>) {
        list.filter { it.name.isBlank() && it.uid !in profiles }.forEach { b ->
            profiles[b.uid] = null
            launchSafe(showBusy = false) {
                profiles[b.uid] = runCatching { ProfileRepo.get(b.uid) }.getOrNull()
            }
        }
    }

    fun unblock(user: BlockedUser, name: String) {
        if (user.uid in working) return
        working.add(user.uid)
        launchSafe(showBusy = false) {
            try {
                SafetyRepo.unblock(user.uid)
                toast(t("{0} ist nicht mehr blockiert.", name))
            } finally {
                working.remove(user.uid)
            }
        }
    }
}

@Composable
fun BlockedScreen(
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    vm: BlockedViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val list by vm.blocked.collectAsState()
    LaunchedEffect(list) { list?.let(vm::loadMissing) }
    var confirm by remember { mutableStateOf<Pair<BlockedUser, String>?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = t("Zurück"),
                    tint = TextHigh,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                t("Blockierte Nutzer"),
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val entries = list
        when {
            entries == null -> LoadingBox()
            entries.isEmpty() -> EmptyState(
                emoji = "🕊️",
                title = t("Niemand blockiert"),
                text = t("Wenn du jemanden blockierst, erscheint die Person hier – und du kannst sie jederzeit wieder entblocken."),
                modifier = Modifier.padding(top = 40.dp)
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        t("Blockierte Personen sehen dich nicht und können dir nicht schreiben. Entblockst du jemanden, kann er dich wieder finden und dir eine Anfrage schicken."),
                        color = TextLow,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp, end = 4.dp)
                    )
                }
                items(entries, key = { it.uid }) { b ->
                    val fallback = vm.profiles[b.uid]
                    val name = b.name.ifBlank { fallback?.name.orEmpty() }.ifBlank { t("Unbekannt") }
                    val photo = b.photo.ifBlank { fallback?.photos?.firstOrNull().orEmpty() }
                    BlockedRow(
                        name = name,
                        photo = photo,
                        color = b.color,
                        since = timeAgo(b.at),
                        working = b.uid in vm.working,
                        onOpen = { onOpenUser(b.uid) },
                        onUnblock = { confirm = b to name }
                    )
                }
            }
        }
    }

    val c = confirm
    if (c != null) {
        ConfirmDialog(
            title = "${c.second} entblocken?",
            text = t("{0} kann dich dann wieder im Entdecken sehen und dir eine Anfrage schicken. Eine frühere Freundschaft kommt nicht automatisch zurück.", c.second),
            confirmLabel = t("Entblocken"),
            onDismiss = { confirm = null },
            onConfirm = {
                confirm = null
                vm.unblock(c.first, c.second)
            }
        )
    }
}

@Composable
private fun BlockedRow(
    name: String,
    photo: String,
    color: String,
    since: String,
    working: Boolean,
    onOpen: () -> Unit,
    onUnblock: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(url = photo, name = name, color = color, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = TextHigh,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (since.isNotBlank()) {
                        Text(t("Blockiert · {0}", since), color = TextLow, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onUnblock,
                enabled = !working,
                shape = CircleShape,
                color = Teal.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, Teal)
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    if (working) {
                        CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(t("Entblocken"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
