package app.turp.ui.screens

import app.turp.data.t

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import app.turp.data.Billing
import app.turp.data.PLUS_REQUEST_COST
import app.turp.data.REQUEST_COST
import app.turp.data.requestCost
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
import app.turp.ui.theme.PageBg
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.InkLine
import app.turp.ui.theme.Sun
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import kotlinx.coroutines.delay
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val Berlin: ZoneId = ZoneId.of("Europe/Berlin")

/** Gems pro Tag der Serie – muss zu checkIn in functions/index.js passen (10 + 2 pro Tag, höchstens +10; kumi+ doppelt) */
private fun dailyReward(day: Long, plus: Boolean = false): Long = (10 + minOf(day - 1, 5) * 2) * (if (plus) 2 else 1)

class ShopViewModel : BaseViewModel() {
    /** Produkt, dessen Kauf gerade startet (Knopf zeigt Ladekreis) */
    var starting by mutableStateOf<String?>(null)
        private set

    fun claimDaily() = launchSafe {
        val (reward, streak) = ProfileRepo.checkIn()
        toast(t("+{0} 💎 · {1} {2} Serie 🔥", reward, streak, if (streak == 1L) t("Tag") else t("Tage")))
    }

    fun buy(context: Context, productId: String) {
        if (starting != null) return
        starting = productId
        launchSafe(showBusy = false) {
            try {
                Billing.buy(context, productId)
            } finally {
                starting = null
            }
        }
    }
}

/** kumi+-Banner: Berglandschaft aus Pappe, Logo mit goldenem Plus */
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
                t("Günstigere Anfragen, doppelte Belohnung."),
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
    val ready by Billing.ready.collectAsState()
    val products by Billing.products.collectAsState()
    val storeOk = ready == true && products.isNotEmpty()
    val isTeen = account.ageGroup == "teen"

    fun buy(productId: String) {
        if (Billing.canBuy(productId)) {
            vm.buy(context, productId)
        } else {
            context.toast(
                if (ready == null) t("Verbinde mit Google Play …")
                else t("Käufe gehen nur mit kumi aus dem Play Store – bis dahin gibt's Gems gratis weiter unten 💎")
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = t("Zurück"), tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text(t("Shop"), color = TextHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            GemPill(gems = account.gems, showDot = false)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlusBanner()
            Surface(shape = RoundedCornerShape(20.dp), color = InkElevated) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Benefit("✉️", t("Anfragen kosten nur {0} statt {1} Gems", PLUS_REQUEST_COST, REQUEST_COST))
                    Benefit("💎", t("Doppelte tägliche Belohnung"))
                    Benefit("✨", t("Goldenes Plus an deinem Namen"))
                    Spacer(Modifier.height(4.dp))
                    if (account.plusActive) {
                        val until = account.plusUntil?.toDate()
                        Text(
                            if (until != null) t("kumi+ ist aktiv bis {0} ✨", SimpleDateFormat("dd.MM.yyyy", app.turp.data.I18n.locale()).format(until))
                            else t("kumi+ ist aktiv ✨"),
                            color = Sun,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        CandyButton(
                            text = t("Abo verwalten"),
                            onClick = { openSubscriptions(context) },
                            color = InkHigh,
                            height = 44.dp
                        )
                    } else {
                        val price = Billing.price(Billing.PLUS) ?: Billing.PLUS_LIST_PRICE
                        CandyButton(
                            text = t("kumi+ holen · {0} / Monat", price),
                            onClick = { buy(Billing.PLUS) },
                            loading = vm.starting == Billing.PLUS,
                            color = Sun,
                            height = 46.dp
                        )
                        Text(
                            t("Monatlich kündbar über Google Play. Verlängert sich automatisch, bis du kündigst."),
                            color = TextLow,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            SectionHeading(t("Gems"), Modifier.padding(top = 6.dp))
            if (!storeOk && ready != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = InkHigh) {
                    Text(
                        t("Gems kaufen geht, sobald du kumi aus dem Play Store hast. Bis dahin: Gems gratis per Einladung und täglicher Belohnung 👇"),
                        color = TextLow,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Billing.PACKS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { pack ->
                        GemPackTile(
                            pack = pack,
                            price = Billing.price(pack.id) ?: pack.listPrice,
                            enabled = storeOk,
                            loading = vm.starting == pack.id,
                            requestCost = requestCost(account.plusActive),
                            modifier = Modifier.weight(1f)
                        ) { buy(pack.id) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                t("Preise inkl. MwSt. Bezahlung über Google Play.") +
                    if (isTeen) " " + t("Unter 18 kannst du höchstens 30 € im Monat ausgeben – frag am besten vorher deine Eltern.") else "",
                color = TextLow,
                fontSize = 11.sp
            )

            SectionHeading(t("Gratis Gems"), Modifier.padding(top = 6.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = InkElevated) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t("😊 Freunde einladen"), color = TextHigh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            GemIcon(14.dp)
                            Text(" +${INVITE_BONUS}", color = TextHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            t("Pro Person, die sich mit deinem Code {0} anmeldet", account.inviteCode.ifBlank { "–" }),
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
                            t("Einladen"),
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            SectionHeading(t("Tägliche Belohnung"), Modifier.padding(top = 6.dp))
            Text(
                t("Hol dir jeden Tag gratis Gems – je länger deine Serie, desto mehr.") +
                    if (account.plusActive) " " + t("Mit kumi+ doppelt!") else "",
                color = TextLow,
                fontSize = 13.sp
            )
            DailyRewards(
                streak = account.streak,
                lastCheckIn = account.lastCheckIn,
                busy = vm.busy,
                plus = account.plusActive,
                onClaim = { vm.claimDaily() }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Öffnet die Abo-Verwaltung von Google Play */
private fun openSubscriptions(context: Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${Billing.PLUS}&package=${context.packageName}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
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
private fun GemPackTile(
    pack: Billing.Pack,
    price: String,
    enabled: Boolean,
    loading: Boolean,
    requestCost: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val highlight = pack.badge != null
    Box(modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = InkElevated,
            border = if (highlight) BorderStroke(1.5.dp, Teal) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(top = 14.dp, bottom = 12.dp, start = 6.dp, end = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GemIcon(32.dp)
                Spacer(Modifier.height(6.dp))
                Text("${pack.gems}", color = TextHigh, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(t("= {0} Anfragen", pack.gems / requestCost), color = TextLow, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .background(if (enabled) Teal else InkHigh, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            price,
                            color = if (enabled) Ink else TextLow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (pack.badge != null) {
            Text(
                pack.badge.uppercase(),
                color = Ink,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)
                    .background(Teal, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/** Tage der Serie mit ihren Belohnungen, darunter Abholen oder Countdown bis Mitternacht */
@Composable
private fun DailyRewards(streak: Long, lastCheckIn: String, busy: Boolean, plus: Boolean, onClaim: () -> Unit) {
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
                            (if (done) "✓ " else "") + t("Tag {0}", day),
                            color = if (done) Teal else TextHigh,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        GemIcon(26.dp, spin = isFocus && !claimedToday)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${dailyReward(day, plus)}",
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
                    text = t("Abholen · +{0} 💎", dailyReward(nextDay, plus)),
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
                    t("Nächste Belohnung in %02d:%02d:%02d ⏰").format(left / 3600, (left % 3600) / 60, left % 60),
                    color = TextLow,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
