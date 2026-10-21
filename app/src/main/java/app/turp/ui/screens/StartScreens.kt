package app.turp.ui.screens

import androidx.compose.foundation.BorderStroke
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import app.turp.R
import app.turp.data.findActivity
import app.turp.ui.components.Sticker
import app.turp.ui.theme.Sky
import app.turp.ui.theme.Sun
import app.turp.ui.theme.TextHigh
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
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
        text = "Diese kumi-Version ist noch nicht mit deinem Firebase-Projekt verbunden. " +
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
            "Wenn du denkst, dass das ein Fehler ist, melde dich beim kumi-Team.",
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

/** Web-Client-ID für „Mit Google“ – steht in google-services.json, sobald Google-Login in Firebase an ist */
private fun googleWebClientId(context: Context): String? {
    @SuppressLint("DiscouragedApi")
    val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    return if (id != 0) context.getString(id).takeIf { it.isNotBlank() } else null
}

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

    /** Google-Konto auswählen lassen und damit bei Firebase anmelden */
    fun signInWithGoogle(activity: Activity) = launchSafe {
        val clientId = googleWebClientId(activity)
        if (clientId == null) {
            toast("Die Google-Anmeldung wird gerade eingerichtet – nutze bitte so lange deine E-Mail.")
            return@launchSafe
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val result = try {
            CredentialManager.create(activity).getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            return@launchSafe
        } catch (e: NoCredentialException) {
            toast("Auf diesem Handy ist kein Google-Konto eingerichtet.")
            return@launchSafe
        } catch (e: GetCredentialException) {
            Log.w("kumi", "Google-Anmeldung", e)
            toast("Google-Anmeldung hat nicht geklappt – bitte versuch es noch einmal.")
            return@launchSafe
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            toast("Google-Anmeldung hat nicht geklappt.")
            return@launchSafe
        }
        val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Fb.auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }
}

/** Interessen-Sticker, die auf dem Startbildschirm schweben (x/y als Anteil der Fläche) */
private data class FloatingSticker(val res: Int, val x: Float, val y: Float, val size: Int, val rotation: Float)

private val StartStickers = listOf(
    FloatingSticker(R.drawable.interest_gaming, 0.05f, 0.05f, 96, -10f),
    FloatingSticker(R.drawable.interest_kpop, 0.68f, 0.03f, 92, 9f),
    FloatingSticker(R.drawable.interest_kaffee, 0.38f, 0.13f, 84, -4f),
    FloatingSticker(R.drawable.interest_tiere, 0.72f, 0.21f, 98, 11f),
    FloatingSticker(R.drawable.interest_reisen, 0.02f, 0.25f, 92, 7f),
    FloatingSticker(R.drawable.interest_digitalart, 0.40f, 0.31f, 84, -9f),
    FloatingSticker(R.drawable.interest_basketball, 0.12f, 0.43f, 70, 14f),
    FloatingSticker(R.drawable.interest_musik, 0.76f, 0.40f, 76, -12f),
)

@Composable
fun AuthScreen(vm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    var step by rememberSaveable { mutableStateOf("start") }
    BackHandler(enabled = step == "email") { step = "start" }

    if (step == "email") {
        EmailAuth(vm, onBack = { step = "start" })
    } else {
        WelcomeAuth(
            busy = vm.busy,
            onGoogle = { context.findActivity()?.let { vm.signInWithGoogle(it) } },
            onEmail = { step = "email" }
        )
    }
}

@Composable
private fun WelcomeAuth(busy: Boolean, onGoogle: () -> Unit, onEmail: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val w = maxWidth
        val h = maxHeight
        val float = rememberInfiniteTransition(label = "schweben")
        StartStickers.forEachIndexed { i, s ->
            val dy by float.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2400 + i * 260, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "sticker$i"
            )
            Image(
                painter = painterResource(s.res),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = w * s.x, y = h * s.y + (dy * 7).dp)
                    .size(s.size.dp)
                    .graphicsLayer { rotationZ = s.rotation + dy * 2f }
            )
        }
        // Verlauf nach unten, damit die Buttons ruhig stehen
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 0.62f to Night.copy(alpha = 0.92f), 1f to Night))
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            TurpLogo(fontSize = 76.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Finde Freunde, die dieselben Sachen lieben wie du.",
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
            Surface(
                onClick = onGoogle,
                enabled = !busy,
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Ink, strokeWidth = 2.5.dp)
                    } else {
                        Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Mit Google fortfahren", color = Color(0xFF1F1F1F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onEmail,
                shape = CircleShape,
                color = InkHigh,
                border = BorderStroke(1.dp, InkLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = Teal, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Mit E-Mail fortfahren", color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Ab 13 Jahren · Goldene Regel: Sei immer freundlich. 💛",
                color = TextLow,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun kumiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal,
    unfocusedBorderColor = InkLine,
    focusedLabelColor = Teal,
    cursorColor = Teal,
    focusedContainerColor = InkElevated,
    unfocusedContainerColor = InkElevated,
)

@Composable
private fun EmailAuth(vm: AuthViewModel, onBack: () -> Unit) {
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
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück", tint = TextHigh)
            }
        }
        Spacer(Modifier.height(12.dp))
        TurpLogo(fontSize = 56.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (register) "Erstell dein Konto mit E-Mail" else "Willkommen zurück!",
            style = MaterialTheme.typography.titleMedium,
            color = TextLow,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        // Umschalter Registrieren / Einloggen
        Surface(shape = CircleShape, color = InkElevated, border = BorderStroke(1.dp, InkLine), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                listOf(true to "Neu hier", false to "Einloggen").forEach { (isRegister, label) ->
                    val selected = register == isRegister
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (selected) Teal else Color.Transparent, CircleShape)
                            .clickable { register = isRegister }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = if (selected) Ink else TextLow)
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
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = TextLow) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(18.dp),
            colors = kumiFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (register) "Passwort (mind. 6 Zeichen)" else "Passwort") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextLow) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(18.dp),
            colors = kumiFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = if (register) "Konto erstellen" else "Einloggen",
            onClick = { vm.submit(register, email, password) },
            enabled = email.contains("@") && password.length >= 6,
            loading = vm.busy
        )
        if (!register) {
            TextButton(onClick = { vm.resetPassword(email) }) { Text("Passwort vergessen?", color = Teal) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---- Profil anlegen -------------------------------------------------------

class OnboardingViewModel : BaseViewModel() {
    fun submit(name: String, birthDate: LocalDate, country: String, parentEmail: String, invite: String, gender: String) = launchSafe {
        ProfileRepo.createProfile(
            name = name.trim(),
            birthDate = birthDate.toString(),
            country = country,
            parentEmail = parentEmail.trim(),
            inviteCode = invite.trim().uppercase(),
            gender = gender,
        )
        // Danach erscheint das Konto automatisch und die App wechselt weiter.
    }
}

private val GenderPink = Color(0xFFF06BAA)

/** Pappkarte mit Figur zum Auswählen des Geschlechts */
@Composable
private fun GenderCard(res: Int, label: String, selected: Boolean, dim: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.95f, label = "groesse")
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (selected) 0f else if (accent == Sky) -1.5f else 1.5f
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(480f / 560f)
                .clip(shape)
                .border(if (selected) 4.dp else 1.dp, if (selected) accent else InkLine, shape)
        ) {
            Image(
                painter = painterResource(res),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (dim) 0.45f else 1f }
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (selected) accent else TextHigh)
    }
}

@Composable
private fun OnboardingCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = InkHigh.copy(alpha = 0.55f), border = BorderStroke(1.dp, InkLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
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
    var gender by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showCountry by remember { mutableStateOf(false) }

    val birthDate = birthMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val age = birthDate?.let { Period.between(it, LocalDate.now(ZoneId.of("Europe/Berlin"))).years }
    val tooYoung = age != null && age < 13
    val needsParent = age != null && age in 13..15
    val isTeen = age != null && age in 13..17
    val adult = age != null && age >= 18
    val canSubmit = name.trim().length >= 2 &&
        birthDate != null && !tooYoung &&
        gender.isNotEmpty() &&
        (!needsParent || parentEmail.contains("@")) &&
        accepted

    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TurpLogo(fontSize = 34.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSignOut) { Text("Abmelden", color = TextLow) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Erzähl uns kurz von dir 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Dein Alter bestimmt, wen du auf kumi triffst – es lässt sich später nicht mehr ändern.",
                color = TextLow,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Sticker("Ich bin …", Teal, rotation = -2f, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GenderCard(
                R.drawable.gender_male, if (adult) "Mann" else "Junge", selected = gender == "male",
                dim = gender.isNotEmpty() && gender != "male", accent = Sky, modifier = Modifier.weight(1f)
            ) { gender = "male" }
            GenderCard(
                R.drawable.gender_female, if (adult) "Frau" else "Mädchen", selected = gender == "female",
                dim = gender.isNotEmpty() && gender != "female", accent = GenderPink, modifier = Modifier.weight(1f)
            ) { gender = "female" }
        }
        Text(
            if (gender == "other") "✓ Keine Angabe" else "Lieber keine Angabe",
            color = if (gender == "other") Teal else TextLow,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .clickable { gender = "other" }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Sticker("Über dich", Sun, rotation = 2f, fontSize = 18.sp)
        OnboardingCard {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Vorname oder Spitzname") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = kumiFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            PickerField(
                label = "🎂 Geburtsdatum",
                value = birthDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Tippen zum Auswählen",
                onClick = { showDate = true }
            )
            if (tooYoung) {
                Text("kumi ist erst ab 13 Jahren erlaubt.", color = Coral, fontWeight = FontWeight.Bold)
            }
            PickerField(
                label = "🌍 Land",
                value = "${Countries.flag(country)} ${Countries.name(country)}",
                onClick = { showCountry = true }
            )
        }

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
                colors = kumiFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = invite,
            onValueChange = { invite = it.take(12) },
            label = { Text("🎁 Einladungscode (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(16.dp),
            colors = kumiFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            onClick = { accepted = !accepted },
            shape = RoundedCornerShape(18.dp),
            color = if (accepted) Teal.copy(alpha = 0.10f) else InkElevated,
            border = BorderStroke(1.dp, if (accepted) Teal else InkLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = { accepted = it },
                    colors = CheckboxDefaults.colors(checkedColor = Teal, checkmarkColor = Ink)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Ich akzeptiere die Nutzungsbedingungen und die Datenschutzerklärung und halte mich an die " +
                        "goldene Regel: Sei immer freundlich. 💛",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        PrimaryButton(
            text = "Los geht's",
            onClick = { if (birthDate != null) vm.submit(name, birthDate, country, parentEmail, invite, gender) },
            enabled = canSubmit,
            loading = vm.busy
        )
        if (!canSubmit && gender.isEmpty() && name.isNotBlank() && birthDate != null) {
            Text("Wähl noch aus, wer du bist. ☝️", color = TextLow, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.height(12.dp))
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
