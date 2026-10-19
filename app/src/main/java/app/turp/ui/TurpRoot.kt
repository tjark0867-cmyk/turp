package app.turp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.SessionState
import app.turp.data.SessionViewModel
import app.turp.ui.screens.AuthScreen
import app.turp.ui.screens.BannedScreen
import app.turp.ui.screens.ConsentPendingScreen
import app.turp.ui.screens.MainNav
import app.turp.ui.screens.OnboardingScreen
import app.turp.ui.screens.SetupMissingScreen
import app.turp.ui.screens.SplashScreen

/** Wählt je nach Anmeldestatus den passenden Bildschirm */
@Composable
fun TurpRoot(pendingChatId: String?, onChatHandled: () -> Unit) {
    val session: SessionViewModel = viewModel()
    val state by session.state.collectAsState()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            SessionState.Loading -> SplashScreen(onRetry = session::retry, onSignOut = session::signOut)
            SessionState.NoFirebase -> SetupMissingScreen()
            SessionState.SignedOut -> AuthScreen()
            SessionState.NeedsProfile -> OnboardingScreen(onSignOut = session::signOut)
            is SessionState.PendingConsent -> ConsentPendingScreen(account = s.account, onSignOut = session::signOut)
            SessionState.Banned -> BannedScreen(onSignOut = session::signOut)
            is SessionState.Active -> MainNav(
                session = s,
                sessionVm = session,
                pendingChatId = pendingChatId,
                onChatHandled = onChatHandled,
            )
        }
    }
}
