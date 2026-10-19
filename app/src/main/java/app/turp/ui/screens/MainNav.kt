package app.turp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.turp.BuildConfig
import app.turp.data.BaseViewModel
import app.turp.data.ChatRepo
import app.turp.ui.components.toast
import app.turp.data.RequestRepo
import app.turp.data.SessionState
import app.turp.data.SessionViewModel
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Lilac
import app.turp.ui.theme.Sky
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Gemeinsame Live-Daten für die Reiter (Anfragen & Chats mit Zähler-Abzeichen) */
class MainViewModel : BaseViewModel() {
    val incoming = RequestRepo.incoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chats = ChatRepo.chats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Chat mit dem Test-Bot öffnen (nur in Test-Versionen sichtbar) */
    fun startTestChat(onOpen: (String) -> Unit) = launchSafe {
        onOpen(ChatRepo.startTestChat())
    }
}

private enum class Tab(val label: String, val icon: ImageVector, val iconOutlined: ImageVector) {
    Discover("Entdecken", Icons.Rounded.Style, Icons.Outlined.Style),
    Lounge("Lounge", Icons.Rounded.Public, Icons.Outlined.Public),
    Requests("Anfragen", Icons.Rounded.EmojiEmotions, Icons.Outlined.EmojiEmotions),
    Chats("Chats", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    Profile("Profil", Icons.Rounded.Person, Icons.Outlined.Person),
}

@Composable
fun MainNav(
    session: SessionState.Active,
    sessionVm: SessionViewModel,
    pendingChatId: String?,
    onChatHandled: () -> Unit,
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    // Über State weitergereicht, damit sich der Navigationsgraph nicht bei jeder Gem-Änderung neu aufbaut
    val sessionState = rememberUpdatedState(session)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pendingChatId) {
        val id = pendingChatId ?: return@LaunchedEffect
        nav.navigate("chat/$id")
        onChatHandled()
    }

    NavHost(
        navController = nav,
        startDestination = "tabs",
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("tabs") {
            MainTabs(sessionState, sessionVm, nav)
        }
        composable("chat/{chatId}") { entry ->
            ChatScreen(
                chatId = entry.arguments?.getString("chatId").orEmpty(),
                onBack = { nav.popBackStack() },
                onOpenUser = { nav.navigate("user/$it") },
            )
        }
        composable("user/{uid}") { entry ->
            UserProfileScreen(
                uid = entry.arguments?.getString("uid").orEmpty(),
                session = sessionState.value,
                onBack = { nav.popBackStack() },
                onOpenChat = { nav.navigate("chat/$it") },
            )
        }
        composable("editProfile") {
            EditProfileScreen(onBack = { nav.popBackStack() })
        }
        composable("premium") {
            PremiumScreen(onBack = { nav.popBackStack() })
        }
        composable("admin") {
            AdminScreen(
                onBack = { nav.popBackStack() },
                onOpenUser = { nav.navigate("user/$it") },
            )
        }
    }
}

@Composable
private fun MainTabs(
    sessionState: State<SessionState.Active>,
    sessionVm: SessionViewModel,
    nav: NavController,
    vm: MainViewModel = viewModel(),
) {
    val session = sessionState.value
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    var tab by rememberSaveable { mutableStateOf(Tab.Discover) }
    val incoming by vm.incoming.collectAsState()
    val chats by vm.chats.collectAsState()
    val unread = chats.count { it.isUnread(session.uid) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TurpBottomBar(
                selected = tab,
                onSelect = { tab = it },
                badges = mapOf(Tab.Requests to incoming.size, Tab.Chats to unread),
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            when (tab) {
                Tab.Discover -> DiscoverScreen(
                    session = session,
                    onOpenUser = { nav.navigate("user/$it") },
                    onEditProfile = { nav.navigate("editProfile") },
                    onOpenProfile = { tab = Tab.Profile },
                )
                Tab.Requests -> RequestsScreen(
                    incoming = incoming,
                    onOpenUser = { nav.navigate("user/$it") },
                    onOpenChat = { nav.navigate("chat/$it") },
                )
                Tab.Chats -> ChatsScreen(
                    chats = chats,
                    me = session.uid,
                    onOpenChat = { nav.navigate("chat/$it") },
                    onDiscover = { tab = Tab.Discover },
                    onStartTestChat = if (BuildConfig.DEBUG) {
                        { vm.startTestChat { chatId -> nav.navigate("chat/$chatId") } }
                    } else {
                        null
                    },
                    testBusy = vm.busy,
                )
                Tab.Lounge -> RoomScreen(
                    session = session,
                    onOpenUser = { nav.navigate("user/$it") },
                )
                Tab.Profile -> ProfileScreen(
                    session = session,
                    sessionVm = sessionVm,
                    onEdit = { nav.navigate("editProfile") },
                    onPremium = { nav.navigate("premium") },
                    onAdmin = { nav.navigate("admin") },
                )
            }
        }
    }
}

/** Untere Leiste: nur Symbole, jedes in seiner eigenen Farbe */
@Composable
private fun TurpBottomBar(selected: Tab, onSelect: (Tab) -> Unit, badges: Map<Tab, Int>) {
    Surface(color = Ink) {
        Column {
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(66.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tab.entries.forEach { t ->
                    val isSelected = t == selected
                    val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, label = "tabScale")
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable { onSelect(t) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isSelected) t.icon else t.iconOutlined,
                            contentDescription = t.label,
                            tint = if (isSelected) Teal else TextLow,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                        val count = badges[t] ?: 0
                        if (count > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 6.dp)
                                    .size(19.dp)
                                    .background(Coral, CircleShape)
                                    .border(2.dp, Ink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (count > 9) "9+" else "$count",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
