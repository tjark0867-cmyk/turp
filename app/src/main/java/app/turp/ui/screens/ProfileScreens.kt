package app.turp.ui.screens

import app.turp.data.t

import app.turp.ui.components.InterestIcon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import app.turp.ui.components.SectionHeading
import app.turp.data.INVITE_BONUS
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import app.turp.ui.components.Sticker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
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
import androidx.compose.material.icons.rounded.Language
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
import app.turp.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import app.turp.ui.components.Surface
import androidx.compose.material3.Switch
import androidx.compose.material.icons.rounded.Palette
import app.turp.data.findActivity
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
import app.turp.data.requestCost
import app.turp.data.SafetyRepo
import app.turp.data.SessionState
import app.turp.data.SessionViewModel
import app.turp.data.Socials
import app.turp.data.UserProfile
import app.turp.ui.components.InterestCategory
import app.turp.ui.components.InterestCategoryChips
import app.turp.ui.components.InterestGrid
import app.turp.ui.components.InterestSearchField
import app.turp.ui.components.findInterests
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
import app.turp.ui.theme.PageBg
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

    var uploading by mutableStateOf(false)
        private set

    /** Das gerade ausgewählte Foto – wird schon gezeigt, während der Server prüft */
    var pendingPhoto by mutableStateOf<Uri?>(null)
        private set

    fun addPhoto(appContext: Context, uri: Uri) {
        if (uploading) return
        uploading = true
        pendingPhoto = uri
        launchSafe(showBusy = false) {
            try {
                ProfileRepo.uploadPhoto(appContext, uri)
                toast(t("Foto geprüft und hinzugefügt ✨"))
            } finally {
                uploading = false
                pendingPhoto = null
            }
        }
    }

    fun checkIn() = launchSafe {
        val (reward, streak, letters) = ProfileRepo.checkIn()
        toast(t("+{0} 💎 · {1} {2} Serie 🔥", reward, streak, if (streak == 1L) t("Tag") else t("Tage")) + if (letters > 0) "  " + t("+{0} Brief ✉️", letters) else "")
    }

    fun claimAdmin(onDone: () -> Unit) = launchSafe {
        val user = Fb.auth.currentUser ?: return@launchSafe
        user.reload().await()
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            toast(t("Bitte bestätige zuerst deine E-Mail – wir haben dir einen Link geschickt. Tippe danach erneut."))
            return@launchSafe
        }
        user.getIdToken(true).await()
        ProfileRepo.claimAdmin()
        toast(t("Admin-Zugang aktiviert 🛡️"))
        onDone()
    }

    fun deleteAccount() = launchSafe {
        ProfileRepo.deleteAccount()
    }
}

internal fun shareInvite(context: Context, code: String) {
    val text = t("Komm zu kumi und finde neue Freunde! 🌍 Gib bei der Anmeldung meinen Code {0} ein, dann bekommst du 10 Bonus-Gems 💎", code)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, t("kumi teilen")))
}

@Composable
fun ProfileScreen(
    session: SessionState.Active,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val profile by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState()
    val account = session.account
    val p = profile
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addPhoto(context.applicationContext, uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Zurück | Profil | Einstellungen
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = t("Zurück"), tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text(
                t("Profil"),
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Rounded.Settings, contentDescription = t("Einstellungen"), tint = TextHigh, modifier = Modifier.size(26.dp))
            }
        }

        if (p == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) { LoadingBox() }
        } else {
            // Bild, Name, Bearbeiten / Vorschau
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(p.photos.firstOrNull().orEmpty(), p.name, p.color, 74.dp, online = true)
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, color = TextHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        if (account.plusActive) {
                            Spacer(Modifier.width(6.dp))
                            Text("kumi+", color = Sun, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(Icons.Rounded.Edit, t("Bearbeiten"), onEdit)
                        PillButton(Icons.Rounded.Visibility, t("Vorschau"), onPreview)
                    }
                }
            }

            // Medien
            SectionHeading(
                t("{0} Medien", p.photos.size + (if (p.videoUrl.isNotBlank()) 1 else 0)),
                Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (p.photos.size < MAX_PHOTOS) {
                    item {
                        Surface(
                            onClick = {
                                ProfileRepo.warmUpMedia()
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !vm.uploading,
                            shape = RoundedCornerShape(16.dp),
                            color = InkElevated,
                            modifier = Modifier.size(width = 104.dp, height = 138.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (vm.uploading) {
                                    UploadPreview(vm.pendingPhoto)
                                } else {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .background(Teal, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = t("Foto hinzufügen"), tint = Ink)
                                    }
                                }
                            }
                        }
                    }
                }
                items(p.photos) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 104.dp, height = 138.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(InkHigh)
                            .clickable(onClick = onEdit)
                    )
                }
            }
        }

        // Upgrade
        SectionHeading(t("Upgrade ⭐"), Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp))
        Surface(
            onClick = onShop,
            shape = RoundedCornerShape(18.dp),
            color = Teal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GemIcon(30.dp, spin = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("Deine Gems: {0}", account.gems), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(t("Nutze sie, um noch mehr Freunde zu finden."), color = Ink.copy(alpha = 0.75f), fontSize = 13.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        PlusBanner(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = onShop,
            buttonLabel = t("Mehr erfahren")
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Kleiner Knopf wie "Bearbeiten" / "Vorschau" */
@Composable
private fun PillButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Teal) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---- Einstellungen --------------------------------------------------------

@Composable
fun SettingsScreen(
    session: SessionState.Active,
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShop: () -> Unit,
    onAdmin: () -> Unit,
    onCheckUpdate: () -> Unit,
    onBlocked: () -> Unit,
    onCredits: () -> Unit,
    onLanguage: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    var confirmDelete by remember { mutableStateOf(false) }
    val account = session.account

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = t("Zurück"), tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text(
                t("Einstellungen"),
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                MenuRow(Icons.Rounded.Edit, t("Profil bearbeiten"), onClick = onEdit)
                MenuRow(Icons.Rounded.AutoAwesome, t("Shop & kumi+"), onClick = onShop)
                MenuRow(Icons.Rounded.PersonAdd, t("Freunde einladen (+{0} 💎)", INVITE_BONUS), onClick = {
                    if (account.inviteCode.isNotBlank()) shareInvite(context, account.inviteCode)
                })
                if (session.isAdmin) MenuRow(Icons.Rounded.AdminPanelSettings, t("Admin-Bereich"), onClick = onAdmin)
                MenuRow(Icons.Rounded.SystemUpdate, t("Nach Updates suchen"), onClick = onCheckUpdate)
            }
            SectionCard {
                MenuRow(Icons.Rounded.Language, t("Sprache · {0}", app.turp.data.I18n.LANGS.firstOrNull { it.code == app.turp.data.I18n.lang }?.name.orEmpty()), onClick = onLanguage)
                // Pappe-Modus: ganze App im Cardboard-Look (wird gespeichert, Ansicht baut sich neu auf)
                MenuSwitchRow(
                    icon = Icons.Rounded.Palette,
                    label = t("Pappe-Modus"),
                    checked = app.turp.ui.theme.Look.cardboard,
                    onChange = { on ->
                        app.turp.ui.theme.Look.set(context, on)
                        context.findActivity()?.recreate()
                    }
                )
                MenuRow(Icons.Rounded.Block, t("Blockierte Nutzer"), onClick = onBlocked)
            }
            SectionCard {
                MenuRow(Icons.AutoMirrored.Rounded.Logout, t("Abmelden"), onClick = { sessionVm.signOut() })
                MenuRow(Icons.Rounded.DeleteForever, t("Konto löschen"), tint = Coral, onClick = { confirmDelete = true })
            }
            Text(
                t("kumi {0} · Dein Einladungscode: {1}\nGoldene Regel: Sei immer freundlich. 💛\n\nNamen von Spielen, Serien und Künstlern gehören ihren Inhabern; kumi steht in keiner Verbindung zu ihnen.", BuildConfig.VERSION_NAME, account.inviteCode.ifBlank { "–" }),
                style = MaterialTheme.typography.bodySmall,
                color = TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = onCredits,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(t("Lizenzen"), color = TextLow, style = MaterialTheme.typography.labelSmall) }
            if (!session.isAdmin) {
                TextButton(
                    onClick = { vm.claimAdmin(onDone = { sessionVm.refreshClaims() }) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(t("Admin? Zugang prüfen"), color = TextLow, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = t("Konto löschen?"),
            text = t("Dein Profil, deine Fotos, Chats und Freundschaften werden endgültig gelöscht. Das kann nicht rückgängig gemacht werden."),
            confirmLabel = t("Endgültig löschen"),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteAccount()
            }
        )
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

@Composable
private fun MenuSwitchRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextHigh)
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextHigh, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Ink)
        )
    }
}

// ---- Profil bearbeiten ----------------------------------------------------

class EditProfileViewModel : BaseViewModel() {
    private val uid = Fb.uid
    val profile = ProfileRepo.profile(uid)
    val socials = ProfileRepo.socials(uid).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** "photo" oder "video", solange ein Upload läuft */
    var uploading by mutableStateOf<String?>(null)
        private set

    /** Das gerade ausgewählte Foto – wird schon gezeigt, während der Server prüft */
    var pendingPhoto by mutableStateOf<Uri?>(null)
        private set

    fun addPhoto(appContext: Context, uri: Uri) {
        if (uploading != null) return
        uploading = "photo"
        pendingPhoto = uri
        launchSafe(showBusy = false) {
            try {
                ProfileRepo.uploadPhoto(appContext, uri)
                toast(t("Foto geprüft und hinzugefügt ✨"))
            } finally {
                uploading = null
                pendingPhoto = null
            }
        }
    }

    fun addVideo(appContext: Context, uri: Uri) {
        if (uploading != null) return
        uploading = "video"
        launchSafe(showBusy = false) {
            try {
                ProfileRepo.uploadVideo(appContext, uri)
                toast(t("Video geprüft und hinzugefügt 🎬"))
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

    fun save(name: String, bio: String, interests: List<String>, color: String, gender: String, socials: Socials, onDone: () -> Unit) =
        launchSafe {
            if (name.trim().length < 2) {
                toast(t("Der Name braucht mindestens 2 Zeichen."))
                return@launchSafe
            }
            ProfileRepo.updateProfile(
                mapOf(
                    "name" to name.trim(),
                    "bio" to bio.trim(),
                    "interests" to interests,
                    "color" to color,
                ) + (if (gender.isNotEmpty()) mapOf("gender" to gender) else emptyMap())
            )
            ProfileRepo.saveSocials(socials)
            toast(t("Gespeichert ✅"))
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
    var gender by remember { mutableStateOf(p.gender) }
    val socialsLoaded = socials != null
    var instagram by remember(socialsLoaded) { mutableStateOf(socials?.instagram.orEmpty()) }
    var snapchat by remember(socialsLoaded) { mutableStateOf(socials?.snapchat.orEmpty()) }
    var tiktok by remember(socialsLoaded) { mutableStateOf(socials?.tiktok.orEmpty()) }
    var visible by remember(socialsLoaded) { mutableStateOf(socials?.visible ?: false) }
    var showColors by remember { mutableStateOf(false) }
    var showInterests by remember { mutableStateOf(false) }
    val isTeen = p.ageGroup == "teen"

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addPhoto(context.applicationContext, uri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addVideo(context.applicationContext, uri)
    }

    fun doSave() {
        vm.save(name, bio, interests, color, gender, Socials(instagram, snapchat, tiktok, visible), onDone = onBack)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
    ) {
        // Zurück | Profil bearbeiten | Speichern
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = t("Zurück"), tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text(
                t("Profil bearbeiten"),
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
            Surface(
                onClick = { doSave() },
                enabled = !vm.busy,
                shape = CircleShape,
                color = InkHigh,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Text(
                    t("Speichern"),
                    color = Teal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Angaben wie in einer Liste
            GroupCard {
                FieldRow(t("Name")) {
                    InlineField(name, { name = it.take(30) }, t("Dein Name"))
                }
                RowDivider()
                FieldRow(t("Alter")) { Text("${p.age}", color = TextLow, fontSize = 15.sp) }
                RowDivider()
                FieldRow(t("Profilfarbe"), onClick = { showColors = true }) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(parseColor(color), CircleShape)
                    )
                }
                RowDivider()
                FieldRow(t("Ich bin")) {
                    val adult = p.ageGroup == "adult"
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "male" to if (adult) t("Mann") else t("Junge"),
                            "female" to if (adult) t("Frau") else t("Mädchen"),
                            "other" to t("Keine Angabe"),
                        ).forEach { (value, label) ->
                            val sel = gender == value
                            Surface(
                                onClick = { gender = value },
                                shape = CircleShape,
                                color = if (sel) Teal else InkHigh,
                            ) {
                                Text(
                                    label,
                                    color = if (sel) Ink else TextLow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                RowDivider()
                FieldRow(t("Land")) { Text("${Countries.flag(p.country)} ${Countries.name(p.country)}", color = TextLow, fontSize = 15.sp) }
            }

            FieldLabel(t("Bio"))
            GroupCard {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    BasicTextField(
                        value = bio,
                        onValueChange = { bio = it.take(500) },
                        textStyle = TextStyle(color = TextHigh, fontSize = 15.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(Teal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                    )
                    if (bio.isEmpty()) Text(t("Erzähl etwas über dich …"), color = TextLow, fontSize = 15.sp)
                }
                Text(
                    "${bio.length}/500",
                    color = TextLow,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 14.dp, bottom = 8.dp)
                )
            }

            GroupCard {
                FieldRow(t("Meine Interessen"), onClick = { showInterests = true }) {
                    if (interests.isEmpty()) {
                        Text(t("Auswählen"), color = TextLow, fontSize = 15.sp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            interests.take(5).forEach { InterestIcon(it, 28.dp) }
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextLow)
                }
            }

            FieldLabel(t("Medien"))
            // null = Feld zum Hinzufügen
            val slots: List<String?> = buildList {
                addAll(p.photos)
                if (p.photos.size < MAX_PHOTOS) add(null)
            }
            slots.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { url ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.78f)
                        ) {
                            if (url == null) {
                                AddPhotoTile(
                                    uploading = vm.uploading == "photo",
                                    preview = vm.pendingPhoto
                                ) {
                                    ProfileRepo.warmUpMedia()
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
                t("Fotos und Videos werden automatisch geprüft. Tippe auf ⭐, um ein Foto zum Hauptfoto zu machen."),
                fontSize = 12.sp,
                color = TextLow
            )
            GroupCard {
                if (p.videoUrl.isNotBlank()) {
                    FieldRow(t("Profilvideo"), onClick = { vm.removeMedia(p.videoUrl) }) {
                        Text(t("Entfernen"), color = Coral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    FieldRow(
                        t("Profilvideo (max. 30 s)"),
                        onClick = {
                            if (vm.uploading == null) {
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        }
                    ) {
                        if (vm.uploading == "video") {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Teal)
                        } else {
                            Icon(Icons.Rounded.Videocam, contentDescription = null, tint = Teal)
                        }
                    }
                }
            }

            FieldLabel(t("Socials"))
            SocialInputRow("👻", Color(0xFFFFFC00), t("Snapchat hinzufügen"), snapchat) { snapchat = it }
            SocialInputRow("📷", Color(0xFFDD2A7B), t("Instagram hinzufügen"), instagram) { instagram = it }
            SocialInputRow("🎵", Color(0xFF111114), t("TikTok hinzufügen"), tiktok) { tiktok = it }
            GroupCard {
                FieldRow(t("Für Freunde sichtbar")) {
                    Switch(
                        checked = visible,
                        onCheckedChange = { visible = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Ink)
                    )
                }
            }
            Text(
                if (isTeen) {
                    t("Nur deine Freunde können deine Socials sehen – und nur, wenn du sie freigibst. Überleg gut, mit wem du sie teilst.")
                } else {
                    t("Nur deine Freunde auf kumi können deine Socials sehen – und nur, wenn du sie freigibst.")
                },
                fontSize = 12.sp,
                color = TextLow
            )
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = t("Speichern"), onClick = { doSave() }, loading = vm.busy)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showColors) {
        Dialog(onDismissRequest = { showColors = false }) {
            Surface(shape = RoundedCornerShape(26.dp), color = InkElevated) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            t("Profilfarbe 🎨"),
                            color = TextHigh,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(onClick = { showColors = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Icon(Icons.Rounded.Close, contentDescription = t("Schließen"), tint = TextHigh)
                        }
                    }
                    Text(
                        t("Wähle eine Farbe für dein Namensschild"),
                        color = TextLow,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Sticker(name.ifBlank { p.name }, parseColor(color), fontSize = 26.sp)
                    Spacer(Modifier.height(16.dp))
                    ColorPicker(selected = color, onSelect = { color = it })
                }
            }
        }
    }

    if (showInterests) {
        ModalBottomSheet(
            onDismissRequest = { showInterests = false },
            containerColor = InkElevated,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            var query by remember { mutableStateOf("") }
            var category by remember { mutableStateOf<InterestCategory?>(null) }
            val items = remember(query, category) { findInterests(query, category) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t("Meine Interessen"),
                        color = TextHigh,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${interests.size}/${MAX_INTERESTS}",
                        color = if (interests.size >= MAX_INTERESTS) Teal else TextLow,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    t("Tippe an, was du magst – so finden dich Leute mit denselben Interessen."),
                    color = TextLow,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                InterestSearchField(query = query, onQuery = { query = it }, placeholder = t("Suchen, z. B. Minecraft"))
                Spacer(Modifier.height(10.dp))
                InterestCategoryChips(selected = category, onSelect = { category = it })
                InterestGrid(
                    items = items,
                    selected = interests,
                    onToggle = { def ->
                        val selected = def.value in interests
                        interests = when {
                            selected -> interests - def.value
                            interests.size >= MAX_INTERESTS -> {
                                context.toast(t("Höchstens {0} Interessen.", MAX_INTERESTS))
                                interests
                            }
                            else -> interests + def.value
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = t("Fertig"), onClick = { showInterests = false })
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Kleine farbige Überschrift wie "Bio" oder "Socials" */
@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
}

/** Dunkle, abgerundete Gruppe von Zeilen */
@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = InkLine, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
}

/** Zeile: Bezeichnung links, Wert rechts */
@Composable
private fun FieldRow(label: String, onClick: (() -> Unit)? = null, value: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, color = TextHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        value()
    }
}

/** Eingabe rechtsbündig in einer Zeile */
@Composable
private fun InlineField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Box(Modifier.widthIn(min = 80.dp, max = 200.dp), contentAlignment = Alignment.CenterEnd) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = TextLow, fontSize = 15.sp, textAlign = TextAlign.End),
            cursorBrush = SolidColor(Teal),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty()) Text(placeholder, color = TextLow.copy(alpha = 0.6f), fontSize = 15.sp)
    }
}

/** Socials-Zeile mit farbigem Symbol und Eingabe */
@Composable
private fun SocialInputRow(emoji: String, color: Color, placeholder: String, value: String, onChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(color, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = { onChange(ProfileRepo.cleanHandle(it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextHigh, fontSize = 15.sp),
                    cursorBrush = SolidColor(Teal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (value.isEmpty()) Text(placeholder, color = TextLow, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AddPhotoTile(uploading: Boolean, preview: Uri? = null, onClick: () -> Unit) {
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
                UploadPreview(preview)
            } else {
                Icon(Icons.Rounded.AddAPhoto, contentDescription = t("Foto hinzufügen"), tint = Teal)
            }
        }
    }
}

/** Ausgewähltes Foto schon zeigen (abgedunkelt), während der Server es prüft */
@Composable
private fun UploadPreview(preview: Uri?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (preview != null) {
            AsyncImage(
                model = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(26.dp), color = Teal, strokeWidth = 2.5.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                t("Wird geprüft…"),
                style = MaterialTheme.typography.labelSmall,
                color = if (preview != null) Color.White else TextLow
            )
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
                Icon(Icons.Rounded.Close, contentDescription = t("Entfernen"), tint = Color.White, modifier = Modifier.size(16.dp))
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
                    contentDescription = if (isMain) t("Hauptfoto") else t("Als Hauptfoto"),
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
        toast(if (matched) t("🎉 Ihr seid jetzt Freunde!") else t("👋 Anfrage gesendet"))
    }

    fun report(reason: String, details: String) = launchSafe {
        SafetyRepo.report(uid, reason, details, t("Profil"))
        toast(t("Danke! Unser Team schaut sich das an. 💛"))
    }

    val blocked = SafetyRepo.isBlocked(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun block(onDone: () -> Unit) = launchSafe {
        SafetyRepo.block(uid)
        toast(t("Blockiert. Du kannst das in den Einstellungen jederzeit rückgängig machen."))
        onDone()
    }

    fun unblock() = launchSafe {
        SafetyRepo.unblock(uid)
        toast(t("Nicht mehr blockiert."))
    }
}

@Composable
fun UserProfileScreen(
    uid: String,
    session: SessionState.Active,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val vm: UserProfileViewModel = viewModel(key = "user_${uid}") { UserProfileViewModel(uid) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val profile by vm.profile.collectAsState()
    val chatId by vm.friendChatId.collectAsState()
    val socials by vm.socials.collectAsState()
    val isMe = uid == session.uid
    var menuOpen by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var showVideo by remember { mutableStateOf(false) }
    val p = profile
    val friendChat = chatId
    val isBlocked by vm.blocked.collectAsState()
    val me by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState()
    val shared = if (isMe || p == null) emptySet() else p.interests.filter { it in me?.interests.orEmpty() }.toSet()

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Oben: Zurück | Menü
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = t("Zurück"),
                    tint = TextHigh,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isMe && p != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = t("Mehr"), tint = TextHigh, modifier = Modifier.size(26.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(t("Melden")) },
                            leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showReport = true
                            }
                        )
                        if (isBlocked) {
                            DropdownMenuItem(
                                text = { Text(t("Entblocken"), color = Teal) },
                                leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, tint = Teal) },
                                onClick = {
                                    menuOpen = false
                                    vm.unblock()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(t("Blockieren"), color = Coral) },
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
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(InkHigh)
        ) {
            when {
                p == null && vm.unavailable -> EmptyState(
                    emoji = "🙈",
                    title = t("Profil nicht verfügbar"),
                    text = t("Diese Person ist gerade nicht erreichbar."),
                    modifier = Modifier.align(Alignment.Center)
                )
                p == null -> LoadingBox()
                else -> {
                    // Seite 1 Foto, Seite 2 Bio + Interessen (+ Socials, Video), dann weitere Fotos
                    ProfileStory(
                        profile = p,
                        bottomSpace = if (isMe) 16.dp else 86.dp,
                        highlight = shared,
                        modifier = Modifier.fillMaxSize(),
                        header = { ProfileNameStickers(p, modifier = Modifier.padding(horizontal = 16.dp)) },
                        infoExtra = {
                            ProfileExtras(
                                accent = parseColor(p.color),
                                showSocials = !isMe,
                                isFriend = friendChat != null,
                                socials = socials,
                                hasVideo = p.videoUrl.isNotBlank(),
                                onVideo = { showVideo = true },
                            )
                        },
                    )
                    // Unten in der Karte: blockiert → entblocken, sonst Chat öffnen oder Anfrage senden
                    if (!isMe && isBlocked) {
                        Surface(
                            onClick = { vm.unblock() },
                            enabled = !vm.busy,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.5.dp, Teal),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(14.dp)
                                .height(56.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (vm.busy) {
                                    CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(Icons.Rounded.Block, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(t("Blockiert · Entblocken"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else if (!isMe) {
                        val requested = vm.requested
                        Surface(
                            onClick = {
                                if (friendChat != null) onOpenChat(friendChat) else if (!requested) vm.sendRequest()
                            },
                            enabled = friendChat != null || (!requested && !vm.busy),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(14.dp)
                                .height(56.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when {
                                    friendChat != null -> {
                                        Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(t("Chat öffnen"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    requested -> Text(t("Anfrage gesendet ✓"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    vm.busy -> CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                                    else -> {
                                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(t("Anfrage senden"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(10.dp))
                                        GemIcon(15.dp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("${requestCost(session.account.plusActive)}", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVideo && p != null && p.videoUrl.isNotBlank()) {
        Dialog(onDismissRequest = { showVideo = false }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ink)
            ) {
                VideoPlayer(
                    url = p.videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                )
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
            text = t("Ihr könnt euch dann nicht mehr sehen oder schreiben. Entblocken kannst du jederzeit in den Einstellungen unter „Blockierte Nutzer“."),
            confirmLabel = t("Blockieren"),
            onDismiss = { confirmBlock = false },
            onConfirm = {
                confirmBlock = false
                vm.block { onBack() }
            }
        )
    }
}

/** Zusatz auf Seite 2 des Profils: Socials (nur für Freunde) und Video */
@Composable
private fun ProfileExtras(
    accent: Color,
    showSocials: Boolean,
    isFriend: Boolean,
    socials: Socials?,
    hasVideo: Boolean,
    onVideo: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    fun open(url: String) {
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            context.toast(t("Link konnte nicht geöffnet werden."))
        }
    }
    val shadow = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 10f))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasVideo) {
            OverlayChip(t("▶  Video ansehen"), onVideo)
        }
        if (showSocials) {
            Sticker(t("Socials"), accent, rotation = -3f, fontSize = 20.sp)
            val s = socials
            when {
                !isFriend -> Text(t("🔒 Sichtbar, sobald ihr Freunde seid."), color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, style = shadow)
                s == null || (s.instagram.isBlank() && s.snapchat.isBlank() && s.tiktok.isBlank()) ->
                    Text(t("Keine Socials freigegeben."), color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, style = shadow)
                else -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (s.instagram.isNotBlank()) OverlayChip("📷 @${s.instagram}") { open("https://instagram.com/${s.instagram}") }
                    if (s.snapchat.isNotBlank()) OverlayChip("👻 @${s.snapchat}") { open("https://www.snapchat.com/add/${s.snapchat}") }
                    if (s.tiktok.isNotBlank()) OverlayChip("🎵 @${s.tiktok}") { open("https://www.tiktok.com/@${s.tiktok}") }
                }
            }
        }
    }
}

/** Halbtransparenter Knopf auf dem Foto */
@Composable
private fun OverlayChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
