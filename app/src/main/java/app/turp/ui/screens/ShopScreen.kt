package app.turp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.turp.R
import app.turp.data.BaseViewModel
import app.turp.data.INVITE_BONUS
import app.turp.data.ProfileRepo
import app.turp.data.SessionState
import app.turp.ui.components.CandyButton
import app.turp.ui.components.GemIcon
import app.turp.ui.components.GemPill
import app.turp.ui.components.SectionHeading
import app.turp.ui.components.TurpLogo
import app.turp.ui.components.toast
import app.turp.ui.components.todayBerlin
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val Berlin: ZoneId = ZoneId.of("Europe/Berlin")

/** Gems pro Tag der Serie – muss zu checkIn in functions/index.js passen (10 + 2 pro Tag, höchstens +10) */
private fun dailyReward(day: Long): Long = 10 + minOf(day - 1, 5) * 2

class ShopViewModel : BaseViewModel() {
    fun claimDaily() = launchSafe {
        val (reward, streak) = ProfileRepo.checkIn()
        toast("+$reward 💎 · $streak ${if (streak == 1L) "Tag" else "Tage"} Serie 🔥")
    }
}

/** turp+-Banner: Berglandschaft aus Pappe, Logo mit goldenem Plus */
@Composable
fun PlusBanner(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, buttonLabel: String? = null) {
    Box(
        modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(26.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Image(
            painter = painterResource(R.drawable.premium_mountains),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurpLogo(fontSize = 42.sp)
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.gold_plus),
                contentDescription = "plus",
                modifier = Modifier.size(40.dp)
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Mehr Gems, mehr Möglichkeiten.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 2f), 10f))
            )
            if (buttonLabel != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    buttonLabel,
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Sun, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun ShopScreen(session: SessionState.Active, onBack: () -> Unit, vm: ShopViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.notices.collect { context.toast(it) } }
    val account = session.account
    var showBenefits by remember { mutableStateOf(false) }
    fun soon() = context.toast("Käufe kommen bald – Gems gibt's schon jetzt gratis weiter unten 💎")

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Zurück", tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text("Shop", color = TextHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            GemPill(gems = account.gems, showDot = false)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlusBanner(
                onClick = { showBenefits = !showBenefits },
                buttonLabel = if (showBenefits) "Weniger" else "Mehr erfahren"
            )
            if (showBenefits) {
                Surface(shape = RoundedCornerShape(20.dp), color = InkElevated) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Benefit("💎", "Doppelte tägliche Belohnung")
                        Benefit("⭐", "Dein Profil wird beim Entdecken öfter gezeigt")
                        Benefit("🎨", "Exklusive Profilfarben")
                        Benefit("🔁", "Übersprungene Profile zurückholen")
                        Spacer(Modifier.height(4.dp))
                        CandyButton(text = "Bald verfügbar", onClick = { soon() }, color = Sun, height = 46.dp)
                    }
                }
            }

            SectionHeading("Gems", Modifier.padding(top = 6.dp))
            val packs = listOf(100 to false, 500 to true, 1000 to false, 2500 to false, 7500 to false)
            packs.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (amount, popular) ->
                        GemPackTile(amount, popular, Modifier.weight(1f)) { soon() }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            SectionHeading("Gratis Gems", Modifier.padding(top = 6.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = InkElevated) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("😊 Freunde einladen", color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            GemIcon(14.dp)
                            Text(" +$INVITE_BONUS", color = TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Pro Person, die sich mit deinem Code ${account.inviteCode.ifBlank { "–" }} anmeldet",
                            color = TextLow,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = { if (account.inviteCode.isNotBlank()) shareInvite(context, account.inviteCode) },
                        shape = CircleShape,
                        color = Teal
                    ) {
                        Text(
                            "Einladen",
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            SectionHeading("Tägliche Belohnung", Modifier.padding(top = 6.dp))
            Text("Hol dir jeden Tag gratis Gems – je länger deine Serie, desto mehr.", color = TextLow, fontSize = 13.sp)
            DailyRewards(
                streak = account.streak,
                lastCheckIn = account.lastCheckIn,
                busy = vm.busy,
                onClaim = { vm.claimDaily() }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Benefit(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextHigh, fontSize = 15.sp)
    }
}

@Composable
private fun GemPackTile(amount: Int, popular: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = InkElevated,
            border = if (popular) BorderStroke(1.5.dp, Teal) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GemIcon(34.dp)
                Spacer(Modifier.height(8.dp))
                Text("$amount Gems", color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Text("Bald", color = TextLow, fontSize = 13.sp)
            }
        }
        if (popular) {
            Text(
                "BELIEBT",
                color = Ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .background(Teal, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

/** Tage der Serie mit ihren Belohnungen, darunter Abholen oder Countdown bis Mitternacht */
@Composable
private fun DailyRewards(streak: Long, lastCheckIn: String, busy: Boolean, onClaim: () -> Unit) {
    val today = todayBerlin()
    val yesterday = LocalDate.now(Berlin).minusDays(1).toString()
    val claimedToday = lastCheckIn == today
    // Serie zählt nur, wenn gestern oder heute abgeholt wurde
    val current = if (claimedToday || lastCheckIn == yesterday) streak else 0L
    val nextDay = current + 1
    val focus = if (claimedToday) current else nextDay
    val first = maxOf(1L, focus - 1)

    Surface(shape = RoundedCornerShape(20.dp), color = InkElevated) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (first until first + 4).forEach { day ->
                    val done = day <= current
                    val isFocus = day == focus
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (isFocus) Teal.copy(alpha = 0.12f) else InkHigh)
                            .border(if (isFocus) 2.dp else 1.dp, if (isFocus) Teal else InkLine, shape)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            (if (done) "✓ " else "") + "Tag $day",
                            color = if (done) Teal else TextHigh,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        GemIcon(26.dp, spin = isFocus && !claimedToday)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${dailyReward(day)}",
                            color = if (done && !isFocus) TextLow else TextHigh,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (!claimedToday) {
                CandyButton(
                    text = "Abholen · +${dailyReward(nextDay)} 💎",
                    onClick = onClaim,
                    loading = busy,
                    color = Teal,
                    height = 48.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val left by produceState(0L) {
                    while (true) {
                        val now = ZonedDateTime.now(Berlin)
                        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(Berlin)
                        value = Duration.between(now, midnight).seconds.coerceAtLeast(0)
                        delay(1000)
                    }
                }
                Text(
                    "Nächste Belohnung in %02d:%02d:%02d ⏰".format(left / 3600, (left % 3600) / 60, left % 60),
                    color = TextLow,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
