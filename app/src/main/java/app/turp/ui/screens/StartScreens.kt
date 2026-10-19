package app.turp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.data.Account
import app.turp.data.BaseViewModel
import app.turp.data.Fb
import app.turp.data.ProfileRepo
import app.turp.ui.components.CountryPickerDialog
import app.turp.ui.components.Countries
import app.turp.ui.components.PrimaryButton
import app.turp.ui.components.TurpLogo
import app.turp.ui.components.toast
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Night
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val backgroundGradient = Brush.verticalGradient(listOf(Ink, Night))

// ---- Ladebildschirm & Hinweise --------------------------------------------

@Composable
fun SplashScreen(onRetry: (() -> Unit)? = null, onSignOut: (() -> Unit)? = null) {
    // Dauert das Laden ungewöhnlich lange, gibt es einen Ausweg statt endlos zu warten
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        slow = true
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TurpLogo()
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Teal)
            if (slow && (onRetry != null || onSignOut != null)) {
                Spacer(Modifier.height(28.dp))
                Text("Das dauert länger als sonst.", color = TextLow)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRetry != null) {
                        TextButton(onClick = { slow = false; onRetry() }) { Text("Erneut versuchen", color = Teal) }
                    }
                    if (onSignOut != null) {
                        TextButton(onClick = onSignOut) { Text("Abmelden", color = TextLow) }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupMissingScreen() {
    MessageScreen(
        emoji = "🔧",
        title = "Fast geschafft!",
        text = "Diese turp-Version ist noch nicht mit deinem Firebase-Projekt verbunden. " +
            "Lege die Datei google-services.json in den Ordner app/ und baue die App neu. " +
            "Die einzelnen Schritte stehen in der README."
    )
}

@Composable
fun BannedScreen(onSignOut: () -> Unit) {
    MessageScreen(
        emoji = "⛔",
        title = "Konto gesperrt",
        text = "Dein Konto wurde gesperrt, weil es gegen unsere Regeln verstoßen hat. " +
            "Wenn du denkst, dass das ein Fehler ist, melde dich beim turp-Team.",
        actionLabel = "Abmelden",
        onAction = onSignOut
    )
}

@Composable
private fun MessageScreen(
    emoji: String,
    title: String,
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, color = TextLow, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun InfoBox(text: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = InkHigh, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PickerField(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = InkElevated,
        border = BorderStroke(1.dp, InkLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextLow)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ---- Login & Registrierung ------------------------------------------------

class AuthViewModel : BaseViewModel() {

    fun submit(register: Boolean, email: String, password: String) = launchSafe {
        val mail = email.trim()
        if (register) {
            val result = Fb.auth.createUserWithEmailAndPassword(mail, password).await()
            try {
                result.user?.sendEmailVerification()?.await()
            } catch (_: Exception) {
            }
        } else {
            Fb.auth.signInWithEmailAndPassword(mail, password).await()
        }
    }

    fun resetPassword(email: String) = launchSafe {
        if (email.isBlank()) {
            toast("Gib zuerst deine E-Mail-Adresse ein.")
            return@launchSafe
        }
        Fb.auth.sendPasswordResetEmail(email.trim()).await()
        toast("Wir haben dir eine E-Mail zum Zurücksetzen geschickt.")
    }
}

@Composable
fun AuthScreen(vm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    var register by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        TurpLogo(fontSize = 72.sp)
        Text(
            "Finde Freunde auf der ganzen Welt.",
            style = MaterialTheme.typography.titleMedium,
            color = TextLow,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        // Umschalter Registrieren / Einloggen
        Surface(shape = CircleShape, color = InkElevated, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                listOf(true to "Neu hier", false to "Einloggen").forEach { (isRegister, label) ->
                    val selected = register == isRegister
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (selected) Teal else InkElevated, CircleShape)
                            .clickable { register = isRegister }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Ink else TextLow
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (register) "Passwort (mind. 6 Zeichen)" else "Passwort") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = if (register) "Konto erstellen" else "Einloggen",
            onClick = { vm.submit(register, email, password) },
            enabled = email.contains("@") && password.length >= 6,
            loading = vm.busy
        )
        if (!register) {
            TextButton(onClick = { vm.resetPassword(email) }) { Text("Passwort vergessen?") }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Goldene Regel auf turp: Sei immer freundlich. 💛",
            color = TextLow,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---- Profil anlegen -------------------------------------------------------

class OnboardingViewModel : BaseViewModel() {
    fun submit(name: String, birthDate: LocalDate, country: String, parentEmail: String, invite: String) = launchSafe {
        ProfileRepo.createProfile(
            name = name.trim(),
            birthDate = birthDate.toString(),
            country = country,
            parentEmail = parentEmail.trim(),
            inviteCode = invite.trim().uppercase(),
        )
        // Danach erscheint das Konto automatisch und die App wechselt weiter.
    }
}

@Composable
fun OnboardingScreen(onSignOut: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    var name by rememberSaveable { mutableStateOf("") }
    var birthMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var country by rememberSaveable { mutableStateOf(Countries.deviceDefault()) }
    var parentEmail by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showCountry by remember { mutableStateOf(false) }

    val birthDate = birthMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val age = birthDate?.let { Period.between(it, LocalDate.now(ZoneId.of("Europe/Berlin"))).years }
    val tooYoung = age != null && age < 13
    val needsParent = age != null && age in 13..15
    val isTeen = age != null && age in 13..17
    val canSubmit = name.trim().length >= 2 &&
        birthDate != null && !tooYoung &&
        (!needsParent || parentEmail.contains("@")) &&
        accepted

    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TurpLogo(fontSize = 40.sp)
        Text("Erzähl uns kurz von dir", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Dein Alter bestimmt, wen du auf turp triffst. Es lässt sich später nicht mehr ändern.",
            color = TextLow
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            label = { Text("Vorname oder Spitzname") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        PickerField(
            label = "Geburtsdatum",
            value = birthDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Tippen zum Auswählen",
            onClick = { showDate = true }
        )
        if (tooYoung) {
            Text("turp ist erst ab 13 Jahren erlaubt.", color = Coral, fontWeight = FontWeight.Bold)
        }

        PickerField(
            label = "Land",
            value = "${Countries.flag(country)} ${Countries.name(country)}",
            onClick = { showCountry = true }
        )

        if (isTeen) {
            InfoBox(
                "🛡️ Du kommst in die Jugend-Community (13–17). Dort triffst du nur Leute in deinem Alter – " +
                    "Erwachsene können dich nicht sehen und dir nicht schreiben."
            )
        }
        if (needsParent) {
            InfoBox(
                "👨‍👩‍👧 Weil du unter 16 bist, muss ein Elternteil zustimmen. " +
                    "Wir schicken eine E-Mail an diese Adresse."
            )
            OutlinedTextField(
                value = parentEmail,
                onValueChange = { parentEmail = it.trim() },
                label = { Text("E-Mail eines Elternteils") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = invite,
            onValueChange = { invite = it.take(12) },
            label = { Text("Einladungscode (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { accepted = !accepted },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(checkedColor = Teal, checkmarkColor = Ink)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Ich akzeptiere die Nutzungsbedingungen und die Datenschutzerklärung und halte mich an die " +
                    "goldene Regel: Sei immer freundlich.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        PrimaryButton(
            text = "Los geht's",
            onClick = { if (birthDate != null) vm.submit(name, birthDate, country, parentEmail, invite) },
            enabled = canSubmit,
            loading = vm.busy
        )
        TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Abmelden", color = TextLow)
        }
    }

    if (showDate) {
        BirthDateDialog(
            initial = birthMillis,
            onDismiss = { showDate = false },
            onPick = {
                birthMillis = it
                showDate = false
            }
        )
    }
    if (showCountry) {
        CountryPickerDialog(
            allowAll = false,
            onDismiss = { showCountry = false },
            onPick = { code ->
                if (code != null) country = code
                showCountry = false
            }
        )
    }
}

@Composable
private fun BirthDateDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val thisYear = LocalDate.now().year
    val suggestion = LocalDate.of(thisYear - 15, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        initialDisplayedMonthMillis = initial ?: suggestion,
        yearRange = (thisYear - 100)..thisYear,
        initialDisplayMode = DisplayMode.Input,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onPick) },
                enabled = state.selectedDateMillis != null
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    ) {
        DatePicker(
            state = state,
            title = { Text("Dein Geburtsdatum", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
        )
    }
}

// ---- Warten auf die Eltern ------------------------------------------------

class ConsentViewModel : BaseViewModel() {
    fun resend(email: String) = launchSafe {
        ProfileRepo.resendConsent(email.trim())
        toast("E-Mail wurde gesendet ✉️")
    }
}

private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return email
    return "${email.first()}•••${email.substring(at)}"
}

@Composable
fun ConsentPendingScreen(account: Account, onSignOut: () -> Unit, vm: ConsentViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    var newEmail by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("⏳", fontSize = 64.sp)
        Text("Fast geschafft!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Wir haben eine E-Mail an ${maskEmail(account.parentEmail)} geschickt. " +
                "Sobald ein Elternteil zustimmt, geht's los – die App aktualisiert sich dann von selbst.",
            color = TextLow,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it.trim() },
            label = { Text("Andere Eltern-E-Mail (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = "E-Mail erneut senden",
            onClick = { vm.resend(newEmail) },
            loading = vm.busy
        )
        TextButton(onClick = onSignOut) { Text("Abmelden", color = TextLow) }
    }
}
