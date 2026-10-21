package app.turp.ui.screens

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.exp
import kotlin.math.sign
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import app.turp.R
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
import app.turp.ui.components.ProfileNameStickers
import app.turp.ui.components.ProfileStory
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
import app.turp.data.DiscoverFilter
import app.turp.data.DiscoverPrefs
import app.turp.data.Images
import app.turp.ui.components.InterestIcon
import androidx.compose.foundation.layout.offset
import app.turp.data.DiscoverRepo
import app.turp.data.ProfileRepo
import app.turp.data.REQUEST_COST
import app.turp.data.requestCost
import app.turp.data.SessionState
import app.turp.data.UserProfile
import app.turp.data.toUserMessage
import app.turp.ui.components.EmptyState
import app.turp.ui.components.GemChip
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
    var filter by mutableStateOf(DiscoverFilter())
        private set

    private var group = ""
    private var myCountry = ""
    private var myInterests: Set<String> = emptySet()

    val ageBounds: IntRange get() = if (group == "teen") 13..17 else 18..80

    /** Eigenes Land und eigene Interessen (für "Mein Land" und "Gemeinsamkeiten zuerst") */
    fun setMe(profile: UserProfile) {
        myCountry = profile.country
        myInterests = profile.interests.toSet()
    }

    fun init(ageGroup: String, load: (IntRange) -> DiscoverFilter) {
        if (group == ageGroup) return
        group = ageGroup
        filter = load(ageBounds)
        reload()
    }

    fun apply(newFilter: DiscoverFilter) {
        filter = newFilter
        reload()
    }

    fun reload() = launchSafe(showBusy = false) {
        loaded = false
        try {
            val f = filter
            // "80" heißt beim oberen Regler: 80 und älter
            val upper = if (f.maxAge >= 80) 200 else f.maxAge
            val list = DiscoverRepo.candidates(group, f.countryCode(myCountry), f.minAge, upper, f.interests)
                .filter { f.gender.isEmpty() || it.bot || it.gender == f.gender }
            cards.value = rank(list, f)
        } finally {
            loaded = true
        }
    }

    /** Test-Bot vorn, dann passende Interessen, dann Gemeinsamkeiten mit dem eigenen Profil */
    private fun rank(list: List<UserProfile>, f: DiscoverFilter): List<UserProfile> {
        val wanted = f.interests.toSet()
        val mine = myInterests
        return list.sortedWith(
            compareByDescending<UserProfile> { it.bot }
                .thenByDescending { p -> p.interests.count { it in wanted } }
                .thenByDescending { p -> if (f.sharedFirst) p.interests.count { it in mine } else 0 }
        )
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
                    when {
                        profile.bot -> "🤖 ${profile.name} hat sofort angenommen – schau in deine Chats!"
                        matched -> "🎉 Ihr seid jetzt Freunde – ${profile.name} hatte dich auch angefragt!"
                        else -> "👋 Anfrage an ${profile.name} gesendet"
                    }
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
    onOpenShop: () -> Unit,
    vm: DiscoverViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    val cards by vm.cards.collectAsState()
    val me by remember(session.uid) { ProfileRepo.profile(session.uid) }.collectAsState()
    var showFilter by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<UserProfile?>(null) }
    val myProfile = me
    val checkInOpen = session.account.lastCheckIn != todayBerlin()
    val myInterests = myProfile?.interests.orEmpty().toSet()

    // Fotos der nächsten Karten schon vorladen, damit beim Wischen nichts nachlädt
    LaunchedEffect(cards) {
        Images.prefetch(
            context,
            cards.take(2).flatMap { it.photos } + cards.drop(2).take(8).mapNotNull { it.photos.firstOrNull() }
        )
    }

    // Erst laden, wenn das eigene Profil da ist (wegen "Mein Land")
    LaunchedEffect(myProfile) { myProfile?.let(vm::setMe) }
    LaunchedEffect(session.account.ageGroup, myProfile != null) {
        if (myProfile != null) {
            vm.init(session.account.ageGroup) { bounds -> DiscoverPrefs.load(context, session.uid, bounds) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
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
                IconButton(onClick = { showFilter = !showFilter }) {
                    Box {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Such-Einstellungen",
                            tint = if (showFilter) Teal else TextHigh,
                            modifier = Modifier.size(28.dp)
                        )
                        if (vm.filter.isActive(vm.ageBounds)) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(9.dp)
                                    .background(Teal, CircleShape)
                                    .border(1.5.dp, Ink, CircleShape)
                            )
                        }
                    }
                }
            }
            TurpLogo(Modifier.align(Alignment.Center), fontSize = 34.sp)
            GemPill(
                gems = session.account.gems,
                showDot = checkInOpen,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onOpenShop)
            )
        }

        if (showFilter) {
            SearchPreferences(
                initial = vm.filter,
                bounds = vm.ageBounds,
                myCountry = myProfile?.country.orEmpty(),
                onClose = { showFilter = false },
                onSave = { f ->
                    DiscoverPrefs.save(context, session.uid, vm.ageBounds, f)
                    showFilter = false
                    vm.apply(f)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 10.dp)
            )
        } else {

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
                    !vm.loaded -> Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GemIcon(110.dp, spin = true)
                        Spacer(Modifier.height(18.dp))
                        Text("Profile werden geladen", color = TextHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    cards.isEmpty() && vm.filter.isActive(vm.ageBounds) -> EmptyState(
                        emoji = "🔍",
                        title = "Niemand passt gerade zu deiner Suche",
                        text = "Probier andere Interessen, ein anderes Land oder ein größeres Alter.",
                        modifier = Modifier.align(Alignment.Center),
                        actionLabel = "Such-Einstellungen ändern",
                        onAction = { showFilter = true },
                        secondaryLabel = "Alle Filter zurücksetzen",
                        onSecondary = {
                            val d = DiscoverFilter.defaults(vm.ageBounds)
                            DiscoverPrefs.save(context, session.uid, vm.ageBounds, d)
                            vm.apply(d)
                        }
                    )
                    cards.isEmpty() -> EmptyState(
                        emoji = "🌍",
                        title = "Gerade keine neuen Leute",
                        text = "Schau später wieder vorbei oder ändere deine Such-Einstellungen.",
                        modifier = Modifier.align(Alignment.Center),
                        actionLabel = "Übersprungene nochmal zeigen",
                        onAction = { vm.reshuffle() }
                    )
                    else -> CardStack(
                        cards = cards,
                        myInterests = myInterests,
                        requestCost = requestCost(session.account.plusActive),
                        onSkip = vm::skip,
                        onLike = vm::like,
                        onOpen = onOpenUser,
                        onReport = { reporting = it }
                    )
                }
            }
        }
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
    myInterests: Set<String>,
    requestCost: Int,
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
    val density = LocalDensity.current
    // Nach oben/unten gibt die Karte nur wenig nach (Gummiband), höchstens ~56 dp
    val maxLift = with(density) { 56.dp.toPx() }
    fun rubber(raw: Float): Float = sign(raw) * maxLift * (1f - exp(-abs(raw) / (maxLift * 2.2f)))

    fun fling(right: Boolean, velocity: Float = 0f) {
        if (flinging) return
        flinging = true
        scope.launch {
            launch { offsetY.animateTo(offsetY.value * 1.4f, tween(240)) }
            offsetX.animateTo(
                if (right) widthPx * 1.5f else -widthPx * 1.5f,
                tween(if (abs(velocity) > 2500f) 170 else 240, easing = FastOutLinearInEasing)
            )
            if (right) onLike(top) else onSkip(top)
        }
    }

    fun settle() {
        scope.launch {
            val back = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)
            launch { offsetX.animateTo(0f, back) }
            offsetY.animateTo(0f, back)
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
                // Nächste Karte liegt voll sichtbar darunter und wächst beim Wischen ein kleines Stück
                ProfileCard(
                    profile = next,
                    myInterests = myInterests,
                    interactive = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = (abs(offsetX.value) / widthPx).coerceIn(0f, 1f)
                            scaleX = 0.965f + 0.035f * p
                            scaleY = 0.965f + 0.035f * p
                        }
                )
            }
        }
        key(top.uid) {
            var rawY by remember { mutableFloatStateOf(0f) }
            ProfileCard(
                profile = top,
                myInterests = myInterests,
                requestCost = requestCost,
                interactive = true,
                onOpen = { onOpen(top.uid) },
                onLike = { fling(true) },
                onSkip = { fling(false) },
                onReport = { onReport(top) },
                likeProgress = { (offsetX.value / widthPx * 2.6f).coerceIn(-1f, 1f) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        // kippt um einen Punkt unter der Karte – wie ein Stapel Karten in der Hand
                        transformOrigin = TransformOrigin(0.5f, 1.1f)
                        rotationZ = offsetX.value / widthPx * 34f
                    }
                    .pointerInput(top.uid) {
                        val tracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                rawY = offsetY.value
                            },
                            onDragEnd = {
                                val v = tracker.calculateVelocity().x
                                val threshold = size.width * 0.3f
                                when {
                                    v > 1400f && offsetX.value > -threshold / 2 -> fling(true, v)
                                    v < -1400f && offsetX.value < threshold / 2 -> fling(false, v)
                                    offsetX.value > threshold -> fling(true, v)
                                    offsetX.value < -threshold -> fling(false, v)
                                    else -> settle()
                                }
                            },
                            onDragCancel = { settle() },
                            onDrag = { change, amount ->
                                change.consume()
                                tracker.addPosition(change.uptimeMillis, change.position)
                                rawY += amount.y
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + amount.x)
                                    offsetY.snapTo(rubber(rawY))
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
    myInterests: Set<String>,
    requestCost: Int = REQUEST_COST,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
    onLike: () -> Unit = {},
    onSkip: () -> Unit = {},
    onReport: () -> Unit = {},
    likeProgress: () -> Float = { 0f },
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier
            .clip(shape)
            .background(InkHigh)
    ) {
        // Seite 1 Foto, Seite 2 Bio + Interessen, dann weitere Fotos.
        // Tippen irgendwo = weiter, linker Rand = zurück.
        val shared = remember(profile.interests, myInterests) { profile.interests.filter { it in myInterests }.toSet() }
        ProfileStory(
            profile = profile,
            interactive = interactive,
            bottomSpace = if (interactive) 72.dp else 16.dp,
            highlight = shared,
            modifier = Modifier.fillMaxSize(),
            header = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        ProfileNameStickers(profile = profile, onClick = if (interactive) onOpen else null)
                        if (shared.isNotEmpty() && !profile.bot) {
                            Spacer(Modifier.height(8.dp))
                            SharedInterestsSticker(shared.toList())
                        }
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
        )

        if (interactive) {
            // Beim Wischen erscheint ein Zeichen aus Pappe: Haken (Anfrage) bzw. X (weiter)
            Image(
                painter = painterResource(R.drawable.swipe_yes),
                contentDescription = null,
                // wie bei purp: erscheint auf der Seite, von der die Karte wegzieht (hier links)
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .size(124.dp)
                    .graphicsLayer {
                        val p = likeProgress().coerceIn(0f, 1f)
                        alpha = p
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        rotationZ = -14f + 8f * p
                    }
            )
            Image(
                painter = painterResource(R.drawable.swipe_no),
                contentDescription = null,
                // beim Wischen nach links rechts auf der Karte
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(124.dp)
                    .graphicsLayer {
                        val p = (-likeProgress()).coerceIn(0f, 1f)
                        alpha = p
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        rotationZ = 14f - 8f * p
                    }
            )
        }

        if (interactive) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onLike,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        GemIcon(15.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("$requestCost", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                RoundCandyButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Weiter",
                    color = Color.Black.copy(alpha = 0.45f),
                    borderColor = Color.White.copy(alpha = 0.7f),
                    iconColor = Color.White,
                    size = 48.dp,
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

/** Kleiner Hinweis auf der Karte: so viele Interessen habt ihr gemeinsam */
@Composable
private fun SharedInterestsSticker(shared: List<String>) {
    Row(
        Modifier
            .padding(start = 4.dp)
            .graphicsLayer { rotationZ = 2f }
            .background(Ink.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .border(1.5.dp, Teal, RoundedCornerShape(12.dp))
            .padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icons = shared.take(3)
        Box(
            Modifier
                .width((26 + 16 * (icons.size - 1)).dp)
                .height(26.dp)
        ) {
            icons.forEachIndexed { i, interest ->
                InterestIcon(interest, 26.dp, Modifier.offset(x = (16 * i).dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            if (shared.size == 1) "1 Gemeinsamkeit" else "${shared.size} Gemeinsamkeiten",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
