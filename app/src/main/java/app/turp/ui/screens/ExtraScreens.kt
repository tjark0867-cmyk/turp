package app.turp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.AdminRepo
import app.turp.data.BaseViewModel
import app.turp.data.Report
import app.turp.ui.components.ConfirmDialog
import app.turp.ui.components.EmptyState
import app.turp.ui.components.LoadingBox
import app.turp.ui.components.SectionCard
import app.turp.ui.components.SectionTitle
import app.turp.ui.components.TurpLogo
import app.turp.ui.components.shortTime
import app.turp.ui.components.toast
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Night
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

// ---- turp+ (vorbereitet, Käufe folgen später) -----------------------------

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("turp+ & Gem-Shop", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .background(Brush.linearGradient(listOf(Night, Lilac.copy(alpha = 0.6f), Coral.copy(alpha = 0.6f))))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TurpLogo(fontSize = 48.sp)
                        Text("+", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Sun)
                    }
                    Text(
                        "Mehr Gems, mehr Möglichkeiten.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            SectionCard {
                Benefit("💎", "Doppelte Gems beim täglichen Check-in")
                Benefit("⭐", "Dein Profil wird beim Entdecken öfter gezeigt")
                Benefit("🎨", "Exklusive Profilfarben und Rahmen")
                Benefit("🔁", "Übersprungene Profile jederzeit zurückholen")
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = Ink)
                ) { Text("Bald verfügbar") }
            }

            SectionTitle("Gem-Pakete")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GemPack("50", "klein", Modifier.weight(1f))
                GemPack("150", "beliebt", Modifier.weight(1f))
                GemPack("400", "groß", Modifier.weight(1f))
            }
            Text(
                "Käufe kommen später über Google Play. Bei Minderjährigen können Eltern Käufe mit Google Family Link freigeben. " +
                    "Gems kannst du schon jetzt kostenlos sammeln: täglicher Check-in, Einladungen und neue Freundschaften.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Benefit(emoji: String, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun GemPack(amount: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = InkHigh, modifier = modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💎", fontSize = 28.sp)
            Text(amount, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextLow)
            Spacer(Modifier.height(6.dp))
            Text("bald", style = MaterialTheme.typography.labelMedium, color = Teal)
        }
    }
}

// ---- Admin: Meldungen bearbeiten -----------------------------------------

class AdminViewModel : BaseViewModel() {
    val reports = AdminRepo.openReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun act(reportId: String, action: String) = launchSafe {
        AdminRepo.act(reportId, action)
        toast(if (action == "ban") "Konto gesperrt." else "Meldung verworfen.")
    }
}

@Composable
fun AdminScreen(onBack: () -> Unit, onOpenUser: (String) -> Unit, vm: AdminViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val reports by vm.reports.collectAsState()
    var confirmBan by remember { mutableStateOf<Report?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("Meldungen", onBack) }
    ) { padding ->
        val list = reports
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                list == null -> LoadingBox()
                list.isEmpty() -> EmptyState(
                    emoji = "✅",
                    title = "Alles erledigt",
                    text = "Keine offenen Meldungen.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Meldungen aus der Jugend-Community stehen oben. Profile mit 3 oder mehr Meldern sind automatisch versteckt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLow
                        )
                    }
                    items(list, key = { it.id }) { r ->
                        ReportCard(
                            report = r,
                            busy = vm.busy,
                            onOpen = { onOpenUser(r.target) },
                            onDismiss = { vm.act(r.id, "dismiss") },
                            onBan = { confirmBan = r }
                        )
                    }
                }
            }
        }
    }

    val ban = confirmBan
    if (ban != null) {
        ConfirmDialog(
            title = "${ban.targetName} sperren?",
            text = "Das Konto wird sofort gesperrt, aus dem Entdecken entfernt und alle Chats werden geschlossen.",
            confirmLabel = "Sperren",
            onDismiss = { confirmBan = null },
            onConfirm = {
                confirmBan = null
                vm.act(ban.id, "ban")
            }
        )
    }
}

@Composable
private fun ReportCard(report: Report, busy: Boolean, onOpen: () -> Unit, onDismiss: () -> Unit, onBan: () -> Unit) {
    val teen = report.ageGroup == "teen"
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = (if (teen) Coral else Lilac).copy(alpha = 0.2f)) {
                Text(
                    if (teen) "Jugend 13–17" else "18+",
                    color = if (teen) Coral else Lilac,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(shortTime(report.createdAt), style = MaterialTheme.typography.labelSmall, color = TextLow)
        }
        Spacer(Modifier.height(8.dp))
        Text(report.reason, style = MaterialTheme.typography.titleMedium)
        Text("Gemeldet: ${report.targetName}", color = TextLow)
        if (report.details.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("„${report.details}“")
        }
        if (report.context.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = InkHigh, modifier = Modifier.fillMaxWidth()) {
                Text(report.context, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpen) { Text("Profil") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onDismiss, enabled = !busy) { Text("Verwerfen") }
            Button(
                onClick = onBan,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink)
            ) { Text("Sperren") }
        }
    }
}
