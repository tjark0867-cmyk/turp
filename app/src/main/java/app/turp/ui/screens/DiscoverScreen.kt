package app.turp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.automirrored.rounded.Send
import app.turp.ui.components.GemIcon
import app.turp.ui.components.readableOn
import app.turp.ui.theme.InkElevated
import app.turp.data.SafetyRepo
import app.turp.ui.components.Avatar
import app.turp.ui.components.GemPill
import app.turp.ui.components.ReportDialog
import app.turp.ui.components.RoundCandyButton
import app.turp.ui.components.Sticker
import app.turp.ui.components.TurpLogo
import app.turp.ui.components.todayBerlin
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Sun
import app.turp.ui.theme.TextHigh
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.BaseViewModel
import app.turp.data.DiscoverRepo
import app.turp.data.ProfileRepo
import app.turp.data.REQUEST_COST
import app.turp.data.SessionState
import app.turp.data.UserProfile
import app.turp.data.toUserMessage
import app.turp.ui.components.Countries
import app.turp.ui.components.CountryPickerDialog
import app.turp.ui.components.EmptyState
import app.turp.ui.components.GemChip
import app.turp.ui.components.InterestSticker
import app.turp.ui.components.LoadingBox
import app.turp.ui.components.ScreenHeader
import app.turp.ui.components.parseColor
import app.turp.ui.components.toast
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class DiscoverViewModel : BaseViewModel() {
    val cards = MutableStateFlow<List<UserProfile>>(emptyList())
    var loaded by mutableStateOf(false)
        private set
    var country by mutableStateOf<String?>(null)
        private set
    var minAge by mutableIntStateOf(13)
        private set
    var maxAge by mutableIntStateOf(17)
        private set

    private var group = ""

    val ageBounds: IntRange get() = if (group == "teen") 13..17 else 18..80

    fun init(ageGroup: String) {
        if (group == ageGroup) return
        group = ageGroup
        minAge = ageBounds.first
        maxAge = ageBounds.last
        reload()
    }

    fun setFilter(newCountry: String?, min: Int, max: Int) {
        country = newCountry
        minAge = min
        maxAge = max
        reload()
    }

    fun reload() = launchSafe(showBusy = false) {
        loaded = false
        try {
            // "80" heißt beim oberen Regler: 80 und älter
            val upper = if (maxAge >= 80) 200 else maxAge
            cards.value = DiscoverRepo.candidates(group, country, minAge, upper)
        } finally {
            loaded = true
        }
    }

    fun reshuffle() = launchSafe(showBusy = false) {
        DiscoverRepo.resetSkipped()
        reload()
    }

    fun skip(profile: UserProfile) {
        cards.value = cards.value - profile
        launchSafe(showBusy = false) { DiscoverRepo.skip(profile.uid) }
    }

    fun report(profile: UserProfile, reason: String, details: String) {
        cards.value = cards.value - profile
        launchSafe {
            SafetyRepo.report(profile.uid, reason, details, "Entdecken")
            DiscoverRepo.skip(profile.uid)
            toast("Danke! Unser Team schaut sich das an. 💛")
        }
    }

    fun like(profile: UserProfile) {
        cards.value = cards.value - profile
        viewModelScope.launch {
            try {
                val matched = DiscoverRepo.sendRequest(profile.uid)
                toast(
                    if (matched) "🎉 Ihr seid jetzt Freunde – ${profile.name} hatte dich auch angefragt!"
                    else "👋 Anfrage an ${profile.name} gesendet"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.toUserMessage())
                // Karte zurücklegen, z. B. wenn die Gems nicht gereicht haben
                cards.value = listOf(profile) + cards.value
            }
        }
    }
}

@Composable
fun DiscoverScreen(
    session: SessionState.Active,
    onOpenUser: (String) -> Unit,
    onEditProfile: () -> Unit,
    onOpenProfile: () -> Unit,
    vm: DiscoverViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(session.account.ageGroup) { vm.init(session.account.ageGroup) }
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val cards by vm.cards.collectAsState()
    val me by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState(initial = null)
    var showFilter by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<UserProfile?>(null) }
    val myProfile = me
    val checkInOpen = session.account.lastCheckIn != todayBerlin()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Kopfzeile: eigenes Profil + Filter | Logo | Gems
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 8.dp)
        ) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    url = myProfile?.photos?.firstOrNull().orEmpty(),
                    name = myProfile?.name.orEmpty(),
                    color = myProfile?.color ?: "#2DE1C2",
                    size = 42.dp,
                    ring = Color.White,
                    online = true,
                    modifier = Modifier.clickable(onClick = onOpenProfile)
                )
                IconButton(onClick = { showFilter = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = TextHigh, modifier = Modifier.size(28.dp))
                }
            }
            TurpLogo(Modifier.align(Alignment.Center), fontSize = 34.sp)
            GemPill(
                gems = session.account.gems,
                showDot = checkInOpen,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onOpenProfile)
            )
        }

        if (myProfile != null && myProfile.photos.isEmpty()) {
            Surface(
                onClick = onEditProfile,
                shape = RoundedCornerShape(16.dp),
                color = InkElevated,
                border = BorderStroke(1.dp, Teal.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Lade ein Foto hoch, damit andere dich sehen können.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Bearbeiten", color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            when {
                !vm.loaded -> LoadingBox()
                cards.isEmpty() -> EmptyState(
                    emoji = "🌍",
                    title = "Gerade keine neuen Leute",
                    text = "Schau später wieder vorbei oder ändere deine Filter.",
                    modifier = Modifier.align(Alignment.Center),
                    actionLabel = "Übersprungene nochmal zeigen",
                    onAction = { vm.reshuffle() }
                )
                else -> CardStack(
                    cards = cards,
                    onSkip = vm::skip,
                    onLike = vm::like,
                    onOpen = onOpenUser,
                    onReport = { reporting = it }
                )
            }
        }
    }

    if (showFilter) {
        FilterDialog(
            country = vm.country,
            minAge = vm.minAge,
            maxAge = vm.maxAge,
            bounds = vm.ageBounds,
            onDismiss = { showFilter = false },
            onApply = { c, min, max ->
                showFilter = false
                vm.setFilter(c, min, max)
            }
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
private fun CardStack(
    cards: List<UserProfile>,
    onSkip: (UserProfile) -> Unit,
    onLike: (UserProfile) -> Unit,
    onOpen: (String) -> Unit,
    onReport: (UserProfile) -> Unit,
) {
    val top = cards.first()
    val scope = rememberCoroutineScope()
    val offsetX = remember(top.uid) { Animatable(0f) }
    val offsetY = remember(top.uid) { Animatable(0f) }
    var flinging by remember(top.uid) { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1000f) }

    fun fling(right: Boolean) {
        if (flinging) return
        flinging = true
        scope.launch {
            offsetX.animateTo(if (right) widthPx * 1.6f else -widthPx * 1.6f, tween(260))
            if (right) onLike(top) else onSkip(top)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        if (cards.size > 1) {
            val next = cards[1]
            key(next.uid) {
                ProfileCard(
                    profile = next,
                    interactive = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = (abs(offsetX.value) / widthPx).coerceIn(0f, 1f)
                            scaleX = 0.94f + 0.06f * p
                            scaleY = 0.94f + 0.06f * p
                            alpha = 0.5f + 0.5f * p
                        }
                )
            }
        }
        key(top.uid) {
            ProfileCard(
                profile = top,
                interactive = true,
                onOpen = { onOpen(top.uid) },
                onLike = { fling(true) },
                onSkip = { fling(false) },
                onReport = { onReport(top) },
                likeProgress = { (offsetX.value / widthPx * 3f).coerceIn(-1f, 1f) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        rotationZ = offsetX.value / 45f
                    }
                    .pointerInput(top.uid) {
                        detectDragGestures(
                            onDragEnd = {
                                val threshold = size.width * 0.28f
                                when {
                                    offsetX.value > threshold -> fling(true)
                                    offsetX.value < -threshold -> fling(false)
                                    else -> scope.launch {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        offsetY.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    launch { offsetX.animateTo(0f, spring()) }
                                    offsetY.animateTo(0f, spring())
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + amount.x)
                                    offsetY.snapTo(offsetY.value + amount.y)
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
    onLike: () -> Unit = {},
    onSkip: () -> Unit = {},
    onReport: () -> Unit = {},
    likeProgress: () -> Float = { 0f },
) {
    var photoIndex by remember(profile.uid) { mutableIntStateOf(0) }
    val accent = parseColor(profile.color)
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier
            .clip(shape)
            .background(InkHigh)
    ) {
        AsyncImage(
            model = profile.photos.getOrNull(photoIndex),
            contentDescription = profile.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.35f),
                        0.2f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.55f)
                    )
                )
        )

        if (interactive) {
            // Links/rechts tippen = Fotos blättern, unten tippen = Profil öffnen
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(profile.uid, profile.photos.size) {
                        detectTapGestures { pos ->
                            if (pos.y > size.height * 0.6f) {
                                onOpen()
                            } else if (pos.x < size.width / 2f) {
                                photoIndex = (photoIndex - 1).coerceAtLeast(0)
                            } else {
                                photoIndex = (photoIndex + 1).coerceAtMost((profile.photos.size - 1).coerceAtLeast(0))
                            }
                        }
                    }
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp)
        ) {
            // Foto-Fortschritt wie bei Storys
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val count = profile.photos.size.coerceAtLeast(1)
                repeat(count) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (i == photoIndex) Color.White else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Sticker(profile.name, accent, rotation = -3f, fontSize = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Sticker(
                        "${Countries.flag(profile.country)} ${profile.country}, ${profile.age}",
                        accent,
                        rotation = -3f,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (interactive) {
                    IconButton(onClick = onReport) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = "Melden",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        if (interactive) {
            SwipeBadge(
                icon = Icons.AutoMirrored.Rounded.Send,
                background = Teal,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = likeProgress().coerceIn(0f, 1f)
                        alpha = p
                        scaleX = 0.7f + 0.3f * p
                        scaleY = 0.7f + 0.3f * p
                    }
            )
            SwipeBadge(
                icon = Icons.Rounded.Close,
                background = InkHigh,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = (-likeProgress()).coerceIn(0f, 1f)
                        alpha = p
                        scaleX = 0.7f + 0.3f * p
                        scaleY = 0.7f + 0.3f * p
                    }
            )
        }

        if (interactive) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onLike,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            tint = Teal,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Anfrage senden",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        GemIcon(15.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("$REQUEST_COST", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                RoundCandyButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Weiter",
                    color = Color.Black.copy(alpha = 0.45f),
                    borderColor = Color.White.copy(alpha = 0.7f),
                    iconColor = Color.White,
                    size = 56.dp,
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun SwipeBadge(icon: ImageVector, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(96.dp)
            .background(background.copy(alpha = 0.92f), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = readableOn(background), modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun Stamp(text: String, color: Color, rotation: Float, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .background(Ink.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .border(4.dp, color, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun FilterDialog(
    country: String?,
    minAge: Int,
    maxAge: Int,
    bounds: IntRange,
    onDismiss: () -> Unit,
    onApply: (String?, Int, Int) -> Unit,
) {
    var selectedCountry by remember { mutableStateOf(country) }
    var range by remember { mutableStateOf(minAge.toFloat()..maxAge.toFloat()) }
    var pickCountry by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter") },
        text = {
            Column {
                Text("Land", style = MaterialTheme.typography.labelMedium, color = TextLow)
                OutlinedButton(onClick = { pickCountry = true }, modifier = Modifier.fillMaxWidth()) {
                    val c = selectedCountry
                    Text(if (c == null) "🌍 Alle Länder" else "${Countries.flag(c)} ${Countries.name(c)}")
                }
                Spacer(Modifier.height(16.dp))
                val upperLabel = if (range.endInclusive.roundToInt() >= 80) "80+" else "${range.endInclusive.roundToInt()}"
                Text(
                    "Alter: ${range.start.roundToInt()} – $upperLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextLow
                )
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = bounds.first.toFloat()..bounds.last.toFloat(),
                    steps = (bounds.last - bounds.first - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(selectedCountry, range.start.roundToInt(), range.endInclusive.roundToInt())
            }) { Text("Anwenden") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )

    if (pickCountry) {
        CountryPickerDialog(
            allowAll = true,
            onDismiss = { pickCountry = false },
            onPick = {
                selectedCountry = it
                pickCountry = false
            }
        )
    }
}
