package app.turp.ui.screens

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import app.turp.ui.components.ProfileNameStickers
import app.turp.ui.components.ProfileStory
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import app.turp.BuildConfig
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.BaseViewModel
import app.turp.data.DiscoverRepo
import app.turp.data.Fb
import app.turp.data.MAX_INTERESTS
import app.turp.data.MAX_PHOTOS
import app.turp.data.ProfileRepo
import app.turp.data.REQUEST_COST
import app.turp.data.SafetyRepo
import app.turp.data.SessionState
import app.turp.data.SessionViewModel
import app.turp.data.Socials
import app.turp.data.UserProfile
import app.turp.ui.components.ALL_INTERESTS
import app.turp.ui.components.Avatar
import app.turp.ui.components.CandyButton
import app.turp.ui.components.ColorPicker
import app.turp.ui.components.ConfirmDialog
import app.turp.ui.components.GemIcon
import app.turp.ui.components.Countries
import app.turp.ui.components.EmptyState
import app.turp.ui.components.InterestSticker
import app.turp.ui.components.LoadingBox
import app.turp.ui.components.PrimaryButton
import app.turp.ui.components.ReportDialog
import app.turp.ui.components.ScreenHeader
import app.turp.ui.components.SectionCard
import app.turp.ui.components.SectionTitle
import app.turp.ui.components.VideoPlayer
import app.turp.ui.components.parseColor
import app.turp.ui.components.toast
import app.turp.ui.components.todayBerlin
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ---- Eigenes Profil -------------------------------------------------------

class ProfileViewModel : BaseViewModel() {

    fun checkIn() = launchSafe {
        val (reward, streak) = ProfileRepo.checkIn()
        toast("+$reward 💎 · $streak ${if (streak == 1L) "Tag" else "Tage"} Serie 🔥")
    }

    fun claimAdmin(onDone: () -> Unit) = launchSafe {
        val user = Fb.auth.currentUser ?: return@launchSafe
        user.reload().await()
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            toast("Bitte bestätige zuerst deine E-Mail – wir haben dir einen Link geschickt. Tippe danach erneut.")
            return@launchSafe
        }
        user.getIdToken(true).await()
        ProfileRepo.claimAdmin()
        toast("Admin-Zugang aktiviert 🛡️")
        onDone()
    }

    fun deleteAccount() = launchSafe {
        ProfileRepo.deleteAccount()
    }
}

private fun shareInvite(context: Context, code: String) {
    val text = "Komm zu turp und finde neue Freunde! 🌍 Gib bei der Anmeldung meinen Code $code ein, " +
        "dann bekommst du 10 Bonus-Gems 💎"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "turp teilen"))
}

@Composable
fun ProfileScreen(
    session: SessionState.Active,
    sessionVm: SessionViewModel,
    onEdit: () -> Unit,
    onPremium: () -> Unit,
    onAdmin: () -> Unit,
    onCheckUpdate: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val profile by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState(initial = null)
    val account = session.account
    val checkedInToday = account.lastCheckIn == todayBerlin()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Profil") {
            if (account.plus) Text("turp+ ✨", color = Sun, fontWeight = FontWeight.Bold)
        }

        ProfileHero(profile, onEdit)

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GemIcon(40.dp, spin = true)
                Spacer(Modifier.width(10.dp))
                Text("${account.gems} Gems", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Eine Freundschaftsanfrage kostet $REQUEST_COST Gems. Neue gibt's beim täglichen Check-in, " +
                    "für neue Freundschaften und Einladungen.",
                color = TextLow,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🔥 ${account.streak} ${if (account.streak == 1L) "Tag" else "Tage"} Serie",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                CandyButton(
                    text = if (checkedInToday) "Erledigt ✓" else "Check-in",
                    onClick = { vm.checkIn() },
                    enabled = !checkedInToday,
                    loading = vm.busy,
                    color = Teal,
                    height = 46.dp,
                    fontSize = 17.sp
                )
            }
        }

        SectionCard {
            Text("Freunde einladen", style = MaterialTheme.typography.titleMedium)
            Text(
                "Für jede Person, die sich mit deinem Code anmeldet, bekommst du 25 💎 – sie selbst bekommt 10 💎.",
                color = TextLow,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = InkHigh, modifier = Modifier.weight(1f)) {
                    Text(
                        account.inviteCode.ifBlank { "–" },
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.width(10.dp))
                CandyButton(
                    text = "Teilen",
                    onClick = { shareInvite(context, account.inviteCode) },
                    enabled = account.inviteCode.isNotBlank(),
                    color = InkHigh,
                    height = 46.dp,
                    fontSize = 17.sp
                )
            }
        }

        SectionCard {
            MenuRow(Icons.Rounded.Edit, "Profil bearbeiten", onClick = onEdit)
            MenuRow(Icons.Rounded.AutoAwesome, "turp+ & Gem-Shop", onClick = onPremium)
            if (session.isAdmin) MenuRow(Icons.Rounded.AdminPanelSettings, "Admin-Bereich", onClick = onAdmin)
            MenuRow(Icons.Rounded.SystemUpdate, "Nach Updates suchen", onClick = onCheckUpdate)
            MenuRow(Icons.AutoMirrored.Rounded.Logout, "Abmelden", onClick = { sessionVm.signOut() })
            MenuRow(Icons.Rounded.DeleteForever, "Konto löschen", tint = Coral, onClick = { confirmDelete = true })
        }

        Text(
            "turp ${BuildConfig.VERSION_NAME} · Goldene Regel: Sei immer freundlich. 💛",
            style = MaterialTheme.typography.bodySmall,
            color = TextLow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (!session.isAdmin) {
            TextButton(
                onClick = { vm.claimAdmin(onDone = { sessionVm.refreshClaims() }) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Admin? Zugang prüfen", color = TextLow, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Konto löschen?",
            text = "Dein Profil, deine Fotos, Chats und Freundschaften werden endgültig gelöscht. " +
                "Das kann nicht rückgängig gemacht werden.",
            confirmLabel = "Endgültig löschen",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteAccount()
            }
        )
    }
}

@Composable
private fun ProfileHero(profile: UserProfile?, onEdit: () -> Unit) {
    if (profile == null) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) { LoadingBox() }
        return
    }
    val accent = parseColor(profile.color)
    Surface(shape = RoundedCornerShape(28.dp), color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f), InkElevated)))
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 50.dp, bottom = 18.dp)
            ) {
                Avatar(
                    profile.photos.firstOrNull().orEmpty(),
                    profile.name,
                    profile.color,
                    92.dp,
                    Modifier.border(4.dp, InkElevated, CircleShape)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(8.dp))
                    Text("${profile.age}", style = MaterialTheme.typography.titleLarge, color = TextLow)
                }
                Text("${Countries.flag(profile.country)} ${Countries.name(profile.country)}", color = TextLow)
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(profile.bio)
                }
                if (profile.interests.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) { profile.interests.forEach { InterestSticker(it, accent) } }
                }
                if (profile.photos.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onEdit) { Text("📸 Erstes Foto hinzufügen") }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, tint: Color = TextHigh, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---- Profil bearbeiten ----------------------------------------------------

class EditProfileViewModel : BaseViewModel() {
    private val uid = Fb.uid
    val profile = ProfileRepo.profile(uid).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val socials = ProfileRepo.socials(uid).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** "photo" oder "video", solange ein Upload läuft */
    var uploading by mutableStateOf<String?>(null)
        private set

    fun addPhoto(appContext: Context, uri: Uri) {
        if (uploading != null) return
        uploading = "photo"
        launchSafe(showBusy = false) {
            try {
                ProfileRepo.uploadPhoto(appContext, uri)
                toast("Foto geprüft und hinzugefügt ✨")
            } finally {
                uploading = null
            }
        }
    }

    fun addVideo(appContext: Context, uri: Uri) {
        if (uploading != null) return
        uploading = "video"
        launchSafe(showBusy = false) {
            try {
                ProfileRepo.uploadVideo(appContext, uri)
                toast("Video geprüft und hinzugefügt 🎬")
            } finally {
                uploading = null
            }
        }
    }

    fun removeMedia(url: String) = launchSafe { ProfileRepo.removeMedia(url) }

    fun makeMainPhoto(url: String) {
        val p = profile.value ?: return
        launchSafe { ProfileRepo.updateProfile(mapOf("photos" to listOf(url) + (p.photos - url))) }
    }

    fun save(name: String, bio: String, interests: List<String>, color: String, socials: Socials, onDone: () -> Unit) =
        launchSafe {
            if (name.trim().length < 2) {
                toast("Der Name braucht mindestens 2 Zeichen.")
                return@launchSafe
            }
            ProfileRepo.updateProfile(
                mapOf(
                    "name" to name.trim(),
                    "bio" to bio.trim(),
                    "interests" to interests,
                    "color" to color,
                )
            )
            ProfileRepo.saveSocials(socials)
            toast("Gespeichert ✅")
            onDone()
        }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, vm: EditProfileViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val profile by vm.profile.collectAsState()
    val socials by vm.socials.collectAsState()
    val p = profile
    if (p == null) {
        LoadingBox()
    } else {
        EditProfileContent(p, socials, vm, onBack)
    }
}

@Composable
private fun EditProfileContent(p: UserProfile, socials: Socials?, vm: EditProfileViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(p.name) }
    var bio by remember { mutableStateOf(p.bio) }
    var interests by remember { mutableStateOf(p.interests) }
    var color by remember { mutableStateOf(p.color) }
    val socialsLoaded = socials != null
    var instagram by remember(socialsLoaded) { mutableStateOf(socials?.instagram.orEmpty()) }
    var snapchat by remember(socialsLoaded) { mutableStateOf(socials?.snapchat.orEmpty()) }
    var tiktok by remember(socialsLoaded) { mutableStateOf(socials?.tiktok.orEmpty()) }
    var visible by remember(socialsLoaded) { mutableStateOf(socials?.visible ?: false) }
    val isTeen = p.ageGroup == "teen"

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addPhoto(context.applicationContext, uri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addVideo(context.applicationContext, uri)
    }

    fun doSave() {
        vm.save(name, bio, interests, color, Socials(instagram, snapchat, tiktok, visible), onDone = onBack)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profil bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = { doSave() }, enabled = !vm.busy) {
                        Text("Speichern", color = Teal, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("Fotos (bis zu $MAX_PHOTOS)")
            // null = Feld zum Hinzufügen
            val slots: List<String?> = buildList {
                addAll(p.photos)
                if (p.photos.size < MAX_PHOTOS) add(null)
            }
            slots.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { url ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.8f)
                        ) {
                            if (url == null) {
                                AddPhotoTile(uploading = vm.uploading == "photo") {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            } else {
                                PhotoTile(
                                    url = url,
                                    isMain = url == p.photos.firstOrNull(),
                                    onRemove = { vm.removeMedia(url) },
                                    onMakeMain = { vm.makeMainPhoto(url) }
                                )
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                "Alle Fotos und Videos werden automatisch geprüft. Nacktbilder und Gewalt werden blockiert. " +
                    "Tippe auf ⭐, um ein Foto zum Hauptfoto zu machen.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )

            SectionTitle("Profilvideo (höchstens 30 Sekunden)")
            if (p.videoUrl.isNotBlank()) {
                VideoPlayer(
                    url = p.videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                OutlinedButton(onClick = { vm.removeMedia(p.videoUrl) }) { Text("Video entfernen") }
            } else {
                OutlinedButton(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    enabled = vm.uploading == null
                ) {
                    if (vm.uploading == "video") {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Teal)
                        Spacer(Modifier.width(8.dp))
                        Text("Wird geprüft…")
                    } else {
                        Icon(Icons.Rounded.Videocam, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Video hinzufügen")
                    }
                }
            }

            SectionTitle("Über dich")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(300) },
                label = { Text("Bio") },
                supportingText = { Text("${bio.length}/300") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Interessen (bis zu $MAX_INTERESTS)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ALL_INTERESTS.forEach { interest ->
                    val selected = interest in interests
                    FilterChip(
                        selected = selected,
                        onClick = {
                            interests = when {
                                selected -> interests - interest
                                interests.size >= MAX_INTERESTS -> {
                                    context.toast("Höchstens $MAX_INTERESTS Interessen.")
                                    interests
                                }
                                else -> interests + interest
                            }
                        },
                        label = { Text(interest) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = parseColor(color).copy(alpha = 0.3f),
                            selectedLabelColor = TextHigh
                        )
                    )
                }
            }

            SectionTitle("Profilfarbe")
            ColorPicker(selected = color, onSelect = { color = it })
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(parseColor(color), InkElevated)))
            )

            SectionTitle("Socials")
            Text(
                if (isTeen) {
                    "Nur deine Freunde können deine Socials sehen – und nur, wenn du sie unten freigibst. " +
                        "Überleg gut, mit wem du sie teilst."
                } else {
                    "Nur deine Freunde auf turp können deine Socials sehen – und nur, wenn du sie freigibst."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
            SocialField("Instagram", instagram) { instagram = it }
            SocialField("Snapchat", snapchat) { snapchat = it }
            SocialField("TikTok", tiktok) { tiktok = it }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Für Freunde sichtbar", modifier = Modifier.weight(1f))
                Switch(
                    checked = visible,
                    onCheckedChange = { visible = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Ink)
                )
            }

            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Speichern", onClick = { doSave() }, loading = vm.busy)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SocialField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(ProfileRepo.cleanHandle(it)) },
        label = { Text(label) },
        prefix = { Text("@") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AddPhotoTile(uploading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !uploading,
        shape = RoundedCornerShape(18.dp),
        color = InkElevated,
        border = BorderStroke(1.5.dp, InkLine),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (uploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = Teal, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(6.dp))
                    Text("Wird geprüft…", style = MaterialTheme.typography.labelSmall, color = TextLow)
                }
            } else {
                Icon(Icons.Rounded.AddAPhoto, contentDescription = "Foto hinzufügen", tint = Teal)
            }
        }
    }
}

@Composable
private fun PhotoTile(url: String, isMain: Boolean, onRemove: () -> Unit, onMakeMain: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(InkHigh)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            onClick = onRemove,
            shape = CircleShape,
            color = Ink.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, contentDescription = "Entfernen", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Surface(
            onClick = onMakeMain,
            enabled = !isMain,
            shape = CircleShape,
            color = if (isMain) Sun else Ink.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = if (isMain) "Hauptfoto" else "Als Hauptfoto",
                    tint = if (isMain) Ink else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---- Profil einer anderen Person ------------------------------------------

class UserProfileViewModel(private val uid: String) : BaseViewModel() {
    val profile = ProfileRepo.profile(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val friendChatId = ProfileRepo.friendChatId(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val socials = friendChatId
        .flatMapLatest { chatId -> if (chatId != null) ProfileRepo.socials(uid) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var unavailable by mutableStateOf(false)
        private set
    var requested by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                val doc = Fb.db.collection("users").document(uid).get().await()
                if (!doc.exists()) unavailable = true
            } catch (e: Exception) {
                unavailable = true
            }
        }
    }

    fun sendRequest() = launchSafe {
        val matched = DiscoverRepo.sendRequest(uid)
        requested = true
        toast(if (matched) "🎉 Ihr seid jetzt Freunde!" else "👋 Anfrage gesendet")
    }

    fun report(reason: String, details: String) = launchSafe {
        SafetyRepo.report(uid, reason, details, "Profil")
        toast("Danke! Unser Team schaut sich das an. 💛")
    }

    fun block(onDone: () -> Unit) = launchSafe {
        SafetyRepo.block(uid)
        toast("Blockiert.")
        onDone()
    }
}

@Composable
fun UserProfileScreen(
    uid: String,
    session: SessionState.Active,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val vm: UserProfileViewModel = viewModel(key = "user_$uid") { UserProfileViewModel(uid) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val profile by vm.profile.collectAsState()
    val chatId by vm.friendChatId.collectAsState()
    val socials by vm.socials.collectAsState()
    val isMe = uid == session.uid
    var menuOpen by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    val p = profile
    val friendChat = chatId

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            p == null && vm.unavailable -> EmptyState(
                emoji = "🙈",
                title = "Profil nicht verfügbar",
                text = "Diese Person ist gerade nicht erreichbar.",
                modifier = Modifier.align(Alignment.Center)
            )
            p == null -> LoadingBox()
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Seite 1 Foto, Seite 2 Bio + Interessen, dann weitere Fotos
                ProfileStory(
                    profile = p,
                    topSpace = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.66f),
                    header = { ProfileNameStickers(p, modifier = Modifier.padding(horizontal = 16.dp)) }
                )
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (p.videoUrl.isNotBlank()) {
                        SectionTitle("Video")
                        VideoPlayer(
                            url = p.videoUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                    if (!isMe) SocialsCard(isFriend = friendChat != null, socials = socials)
                    Spacer(Modifier.height(96.dp))
                }
            }
        }

        // Obere Leiste: Zurück und Menü
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", onBack)
            Spacer(Modifier.weight(1f))
            if (!isMe && p != null) {
                Box {
                    RoundIcon(Icons.Rounded.MoreVert, "Mehr") { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Melden") },
                            leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showReport = true
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
        }

        // Untere Aktion: Anfrage senden oder Chat öffnen
        if (p != null && !isMe) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Ink)))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                if (friendChat != null) {
                    PrimaryButton(text = "💬 Chat öffnen", onClick = { onOpenChat(friendChat) })
                } else {
                    PrimaryButton(
                        text = if (vm.requested) "Anfrage gesendet ✓" else "👋 Anfrage senden · $REQUEST_COST 💎",
                        onClick = { vm.sendRequest() },
                        enabled = !vm.requested,
                        loading = vm.busy
                    )
                }
            }
        }
    }

    if (showReport && p != null) {
        ReportDialog(
            name = p.name,
            onDismiss = { showReport = false },
            onSubmit = { reason, details ->
                vm.report(reason, details)
                showReport = false
            }
        )
    }
    if (confirmBlock && p != null) {
        ConfirmDialog(
            title = "${p.name} blockieren?",
            text = "Ihr könnt euch dann nicht mehr sehen oder schreiben.",
            confirmLabel = "Blockieren",
            onDismiss = { confirmBlock = false },
            onConfirm = {
                confirmBlock = false
                vm.block { onBack() }
            }
        )
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Ink.copy(alpha = 0.6f), modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
    }
}

@Composable
private fun SocialsCard(isFriend: Boolean, socials: Socials?) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    fun open(url: String) {
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            context.toast("Link konnte nicht geöffnet werden.")
        }
    }
    SectionCard {
        Text("Socials", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        val s = socials
        when {
            !isFriend -> Text("🔒 Sichtbar, sobald ihr Freunde seid.", color = TextLow)
            s == null || (s.instagram.isBlank() && s.snapchat.isBlank() && s.tiktok.isBlank()) ->
                Text("Keine Socials freigegeben.", color = TextLow)
            else -> {
                if (s.instagram.isNotBlank()) SocialRow("📷 Instagram", s.instagram) { open("https://instagram.com/${s.instagram}") }
                if (s.snapchat.isNotBlank()) SocialRow("👻 Snapchat", s.snapchat) { open("https://www.snapchat.com/add/${s.snapchat}") }
                if (s.tiktok.isNotBlank()) SocialRow("🎵 TikTok", s.tiktok) { open("https://www.tiktok.com/@${s.tiktok}") }
            }
        }
    }
}

@Composable
private fun SocialRow(label: String, handle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text("@$handle", color = Teal, fontWeight = FontWeight.Bold)
    }
}
