package app.turp.ui.screens

import app.turp.data.t

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import app.turp.ui.theme.Display
import app.turp.ui.theme.Marker
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
import app.turp.ui.components.Icon
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
import app.turp.data.callFunction
import app.turp.ui.theme.Sky
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.turp.ui.theme.PageBg
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Night
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

private val backgroundGradient: Brush get() = Brush.verticalGradient(listOf(PageBg, Night))

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
                Text(t("Das dauert länger als sonst."), color = TextLow)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRetry != null) {
                        TextButton(onClick = { slow = false; onRetry() }) { Text(t("Erneut versuchen"), color = Teal) }
                    }
                    if (onSignOut != null) {
                        TextButton(onClick = onSignOut) { Text(t("Abmelden"), color = TextLow) }
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
        title = t("Fast geschafft!"),
        text = t("Diese kumi-Version ist noch nicht mit deinem Firebase-Projekt verbunden. Lege die Datei google-services.json in den Ordner app/ und baue die App neu. Die einzelnen Schritte stehen in der README.")
    )
}

@Composable
fun BannedScreen(onSignOut: () -> Unit) {
    MessageScreen(
        emoji = "⛔",
        title = t("Konto gesperrt"),
        text = t("Dein Konto wurde gesperrt, weil es gegen unsere Regeln verstoßen hat. Wenn du denkst, dass das ein Fehler ist, melde dich beim kumi-Team."),
        actionLabel = t("Abmelden"),
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
            toast(t("Gib zuerst deine E-Mail-Adresse ein."))
            return@launchSafe
        }
        Fb.auth.sendPasswordResetEmail(email.trim()).await()
        toast(t("Wir haben dir eine E-Mail zum Zurücksetzen geschickt."))
    }

    /** Google-Konto auswählen lassen und damit bei Firebase anmelden */
    fun signInWithGoogle(activity: Activity) = launchSafe {
        val clientId = googleWebClientId(activity)
            ?: runCatching { callFunction("googleClientId")["clientId"] as? String }.getOrNull()?.takeIf { it.isNotBlank() }
        if (clientId == null) {
            toast(t("Die Google-Anmeldung wird gerade eingerichtet – nutze bitte so lange deine E-Mail."))
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
            toast(t("Auf diesem Handy ist kein Google-Konto eingerichtet."))
            return@launchSafe
        } catch (e: GetCredentialException) {
            Log.w("kumi", "Google-Anmeldung", e)
            toast(t("Google-Anmeldung hat nicht geklappt – bitte versuch es noch einmal."))
            return@launchSafe
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            toast(t("Google-Anmeldung hat nicht geklappt."))
            return@launchSafe
        }
        val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Fb.auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }
}

/** Interessen-Sticker, die auf dem Startbildschirm schweben (x/y als Anteil der Fläche) */
private data class FloatingSticker(val res: Int, val x: Float, val y: Float, val size: Int, val rotation: Float)

private val StartStickers = listOf(
    FloatingSticker(R.drawable.interest_lesen, 0.06f, 0.06f, 104, -8f),
    FloatingSticker(R.drawable.interest_singen, 0.66f, 0.04f, 100, 10f),
    FloatingSticker(R.drawable.interest_katzen, 0.37f, 0.15f, 112, -3f),
    FloatingSticker(R.drawable.interest_reisen, 0.04f, 0.28f, 96, 7f),
    FloatingSticker(R.drawable.interest_basketball, 0.72f, 0.26f, 86, -12f),
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
                label = "sticker${i}"
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
                .background(Brush.verticalGradient(0.34f to Color.Transparent, 0.5f to Night.copy(alpha = 0.9f), 1f to Night))
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // etwas unter der Mitte, aber nicht ganz unten
            Spacer(Modifier.weight(1f))
            TurpLogo(fontSize = 76.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                t("Finde Freunde, die dieselben Sachen lieben wie du."),
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
                    Text(t("Mit Google fortfahren"), color = Color(0xFF1F1F1F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    Text(t("Mit E-Mail fortfahren"), color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                t("Ab 13 Jahren · Goldene Regel: Sei immer freundlich. 💛"),
                color = TextLow,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(0.45f))
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = t("Zurück"), tint = TextHigh)
            }
        }
        Spacer(Modifier.height(12.dp))
        TurpLogo(fontSize = 56.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (register) t("Erstell dein Konto mit E-Mail") else t("Willkommen zurück!"),
            style = MaterialTheme.typography.titleMedium,
            color = TextLow,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        // Umschalter Registrieren / Einloggen
        Surface(shape = CircleShape, color = InkElevated, border = BorderStroke(1.dp, InkLine), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                listOf(true to t("Neu hier"), false to t("Einloggen")).forEach { (isRegister, label) ->
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
            label = { Text(t("E-Mail")) },
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
            label = { Text(if (register) t("Passwort (mind. 6 Zeichen)") else t("Passwort")) },
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
            text = if (register) t("Konto erstellen") else t("Einloggen"),
            onClick = { vm.submit(register, email, password) },
            enabled = email.contains("@") && password.length >= 6,
            loading = vm.busy
        )
        if (!register) {
            TextButton(onClick = { vm.resetPassword(email) }) { Text(t("Passwort vergessen?"), color = Teal) }
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

// ---- Profil anlegen: vier kurze Schritte ------------------------------------

private const val ONBOARDING_STEPS = 4
private val InkBlue = Color(0xFF1F2B5C)

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, fontFamily = Display, fontSize = 34.sp, lineHeight = 38.sp, color = TextHigh)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = TextLow, fontSize = 15.sp, lineHeight = 21.sp)
}

/** Eingabefeld für Tag/Monat/Jahr (nur Ziffern, springt weiter wenn voll) */
@Composable
private fun DateBox(
    label: String,
    value: String,
    digits: Int,
    focus: FocusRequester,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = TextLow, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(digits)) },
            singleLine = true,
            textStyle = TextStyle(color = TextHigh, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            cursorBrush = SolidColor(Teal),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused }
                .background(InkElevated, RoundedCornerShape(16.dp))
                .border(if (focused) 2.dp else 1.dp, if (focused) Teal else InkLine, RoundedCornerShape(16.dp))
                .padding(vertical = 14.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text("•".repeat(digits), color = InkLine, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
fun OnboardingScreen(onSignOut: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf(Countries.deviceDefault()) }
    var parentEmail by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var showInvite by rememberSaveable { mutableStateOf(false) }
    var gender by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var showCountry by remember { mutableStateOf(false) }

    val today = LocalDate.now(ZoneId.of("Europe/Berlin"))
    val birthDate = if (year.length == 4) {
        runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()?.takeIf { !it.isAfter(today) }
    } else null
    val dateTyped = day.isNotEmpty() && month.isNotEmpty() && year.length == 4
    val age = birthDate?.let { Period.between(it, today).years }
    val tooYoung = age != null && age < 13
    val needsParent = age != null && age in 13..15
    val isTeen = age != null && age in 13..17
    val adult = age != null && age >= 18
    val parentOk = parentEmail.contains("@") && parentEmail.substringAfter("@").contains(".")

    val stepOk = when (step) {
        0 -> name.trim().length >= 2
        1 -> birthDate != null && !tooYoung && age!! <= 110 && (!needsParent || parentOk)
        2 -> gender.isNotEmpty()
        else -> accepted
    }

    fun next() {
        if (!stepOk) return
        if (step < ONBOARDING_STEPS - 1) {
            step++
        } else if (birthDate != null) {
            vm.submit(name, birthDate, country, parentEmail, invite, gender)
        }
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(Night)
            .systemBarsPadding()
            .imePadding()
    ) {
        // Kopf: zurück · Fortschritt · abmelden
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = t("Zurück"), tint = TextHigh)
                    }
                }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(ONBOARDING_STEPS) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) Teal else InkLine)
                    )
                }
            }
            TextButton(onClick = onSignOut) { Text(t("Abmelden"), color = TextLow, fontSize = 13.sp) }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
            label = "schritt",
            modifier = Modifier.weight(1f)
        ) { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(22.dp))
                when (s) {
                    0 -> {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                        StepTitle(t("Wie heißt du?"), t("Vorname oder Spitzname – so sehen dich die anderen."))
                        Spacer(Modifier.height(26.dp))
                        // Namensschild aus Pappe, der Name erscheint beim Tippen
                        Box(
                            Modifier
                                .fillMaxWidth(0.88f)
                                .align(Alignment.CenterHorizontally)
                                .aspectRatio(640f / 440f)
                                .graphicsLayer { rotationZ = -3f }
                        ) {
                            Image(painterResource(R.drawable.onb_nametag), contentDescription = null, modifier = Modifier.fillMaxSize())
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                Box(
                                    Modifier
                                        .offset(x = maxWidth * 0.072f, y = maxHeight * 0.423f)
                                        .size(maxWidth * 0.856f, maxHeight * 0.482f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val n = name.trim()
                                    Text(
                                        n.ifEmpty { "…" },
                                        fontFamily = Marker,
                                        color = if (n.isEmpty()) InkBlue.copy(alpha = 0.3f) else InkBlue,
                                        fontSize = when {
                                            n.length > 18 -> 20.sp
                                            n.length > 11 -> 26.sp
                                            else -> 36.sp
                                        },
                                        maxLines = 1,
                                        modifier = Modifier.graphicsLayer { rotationZ = 1.5f }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(30.dp))
                        var focused by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it.take(30) },
                            singleLine = true,
                            textStyle = TextStyle(color = TextHigh, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { next() }),
                            cursorBrush = SolidColor(Teal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus)
                                .onFocusChanged { focused = it.isFocused },
                            decorationBox = { inner ->
                                Column {
                                    Box {
                                        if (name.isEmpty()) Text(t("Dein Name"), color = TextLow.copy(alpha = 0.6f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        inner()
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(if (focused) Teal else InkLine)
                                    )
                                }
                            }
                        )
                    }

                    1 -> {
                        val fd = remember { FocusRequester() }
                        val fm = remember { FocusRequester() }
                        val fy = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { fd.requestFocus() } }
                        StepTitle(
                            t("Wann hast du Geburtstag?"),
                            t("Dein Alter bestimmt, wen du auf kumi triffst. Es lässt sich später nicht mehr ändern.")
                        )
                        Spacer(Modifier.height(18.dp))
                        Image(
                            painterResource(R.drawable.onb_cake),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .align(Alignment.CenterHorizontally)
                                .aspectRatio(600f / 560f)
                                .graphicsLayer { rotationZ = 3f }
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DateBox(t("Tag"), day, 2, fd, Modifier.weight(1f)) {
                                day = it
                                if (it.length == 2) runCatching { fm.requestFocus() }
                            }
                            DateBox(t("Monat"), month, 2, fm, Modifier.weight(1f)) {
                                month = it
                                if (it.length == 2) runCatching { fy.requestFocus() }
                            }
                            DateBox(t("Jahr"), year, 4, fy, Modifier.weight(1.5f)) { year = it }
                        }
                        Spacer(Modifier.height(14.dp))
                        when {
                            dateTyped && birthDate == null ->
                                Text(t("Dieses Datum gibt es nicht."), color = Coral, fontWeight = FontWeight.Bold)
                            tooYoung ->
                                Text(t("kumi ist erst ab 13 Jahren erlaubt."), color = Coral, fontWeight = FontWeight.Bold)
                            age != null && age > 110 ->
                                Text(t("Bitte prüf das Jahr noch mal."), color = Coral, fontWeight = FontWeight.Bold)
                            age != null ->
                                Text(t("🎂  Du bist {0}", age), color = TextHigh, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        }
                        if (isTeen) {
                            Spacer(Modifier.height(12.dp))
                            InfoBox(
                                t("🛡️ Du kommst in die Jugend-Community (13–17). Dort triffst du nur Leute in deinem Alter – Erwachsene können dich nicht sehen und dir nicht schreiben.")
                            )
                        }
                        if (needsParent) {
                            Spacer(Modifier.height(12.dp))
                            InfoBox(t("👨‍👩‍👧 Weil du unter 16 bist, muss ein Elternteil zustimmen. Wir schicken eine E-Mail an diese Adresse."))
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = parentEmail,
                                onValueChange = { parentEmail = it.trim() },
                                label = { Text(t("E-Mail eines Elternteils")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(16.dp),
                                colors = kumiFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    2 -> {
                        StepTitle(t("Ich bin …"), t("Damit dich die richtigen Leute in der Suche finden."))
                        Spacer(Modifier.height(26.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GenderCard(
                                R.drawable.gender_male, if (adult) t("Mann") else t("Junge"), selected = gender == "male",
                                dim = gender.isNotEmpty() && gender != "male", accent = Sky, modifier = Modifier.weight(1f)
                            ) { gender = "male" }
                            GenderCard(
                                R.drawable.gender_female, if (adult) t("Frau") else t("Mädchen"), selected = gender == "female",
                                dim = gender.isNotEmpty() && gender != "female", accent = GenderPink, modifier = Modifier.weight(1f)
                            ) { gender = "female" }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            if (gender == "other") t("✓ Keine Angabe") else t("Lieber keine Angabe"),
                            color = if (gender == "other") Teal else TextLow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .clickable { gender = "other" }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    else -> {
                        StepTitle(t("Wo wohnst du?"), t("Leute aus deiner Gegend findest du so schneller."))
                        Spacer(Modifier.height(22.dp))
                        // Kofferanhänger aus Pappe mit Flagge – tippen zum Ändern
                        Box(
                            Modifier
                                .fillMaxWidth(0.94f)
                                .align(Alignment.CenterHorizontally)
                                .aspectRatio(640f / 400f)
                                .graphicsLayer { rotationZ = -4f }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCountry = true }
                        ) {
                            Image(painterResource(R.drawable.onb_tag), contentDescription = null, modifier = Modifier.fillMaxSize())
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                Column(
                                    Modifier
                                        .offset(x = maxWidth * 0.306f, y = maxHeight * 0.195f)
                                        .size(maxWidth * 0.6f, maxHeight * 0.61f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(Countries.flag(country), fontSize = 38.sp)
                                    Text(
                                        Countries.name(country),
                                        fontFamily = Marker,
                                        color = InkBlue,
                                        fontSize = if (Countries.name(country).length > 12) 18.sp else 24.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(t("tippen zum Ändern"), color = Color(0xFF9A8F80), fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(26.dp))
                        if (showInvite) {
                            OutlinedTextField(
                                value = invite,
                                onValueChange = { invite = it.take(12) },
                                label = { Text(t("Einladungscode")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                shape = RoundedCornerShape(16.dp),
                                colors = kumiFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                t("🎁  Ich habe einen Einladungscode"),
                                color = Teal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showInvite = true }
                                    .padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { accepted = !accepted }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = accepted,
                                onCheckedChange = { accepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = Teal, checkmarkColor = Ink, uncheckedColor = TextLow)
                            )
                            Text(
                                t("Ich akzeptiere die Nutzungsbedingungen und die Datenschutzerklärung und halte mich an die goldene Regel: Sei immer freundlich."),
                                color = TextLow,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Box(Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
            PrimaryButton(
                text = if (step < ONBOARDING_STEPS - 1) t("Weiter") else t("Los geht's"),
                onClick = { next() },
                enabled = stepOk,
                loading = vm.busy
            )
        }
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

// ---- Warten auf die Eltern ------------------------------------------------

class ConsentViewModel : BaseViewModel() {
    fun resend(email: String) = launchSafe {
        ProfileRepo.resendConsent(email.trim())
        toast(t("E-Mail wurde gesendet ✉️"))
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
        Text(t("Fast geschafft!"), style = MaterialTheme.typography.headlineMedium)
        Text(
            t("Wir haben eine E-Mail an {0} geschickt. Sobald ein Elternteil zustimmt, geht's los – die App aktualisiert sich dann von selbst.", maskEmail(account.parentEmail)),
            color = TextLow,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it.trim() },
            label = { Text(t("Andere Eltern-E-Mail (optional)")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = t("E-Mail erneut senden"),
            onClick = { vm.resend(newEmail) },
            loading = vm.busy
        )
        TextButton(onClick = onSignOut) { Text(t("Abmelden"), color = TextLow) }
    }
}
