package app.turp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
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
import app.turp.data.ChatRepo
import app.turp.data.RequestRepo
import app.turp.data.SessionState
import app.turp.data.SessionViewModel
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Gemeinsame Live-Daten für die Reiter (Anfragen & Chats mit Zähler-Abzeichen) */
class MainViewModel : ViewModel() {
    val incoming = RequestRepo.incoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chats = ChatRepo.chats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Discover("Entdecken", Icons.Rounded.Explore),
    Requests("Anfragen", Icons.Rounded.PersonAdd),
    Chats("Chats", Icons.Rounded.ChatBubble),
    Lounge("Lounge", Icons.Rounded.Forum),
    Profile("Profil", Icons.Rounded.Person),
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
    var tab by rememberSaveable { mutableStateOf(Tab.Discover) }
    val incoming by vm.incoming.collectAsState()
    val chats by vm.chats.collectAsState()
    val unread = chats.count { it.isUnread(session.uid) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = InkElevated, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    val badge = when (t) {
                        Tab.Requests -> incoming.size
                        Tab.Chats -> unread
                        else -> 0
                    }
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            if (badge > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = Coral, contentColor = Color.White) {
                                        Text(if (badge > 9) "9+" else badge.toString())
                                    }
                                }) { Icon(t.icon, contentDescription = t.label) }
                            } else {
                                Icon(t.icon, contentDescription = t.label)
                            }
                        },
                        label = { Text(t.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Teal,
                            indicatorColor = Teal,
                            unselectedIconColor = TextLow,
                            unselectedTextColor = TextLow,
                        )
                    )
                }
            }
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
