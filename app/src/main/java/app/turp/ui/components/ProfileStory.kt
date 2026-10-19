package app.turp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.data.UserProfile
import app.turp.ui.theme.InkHigh
import coil.compose.AsyncImage
import kotlin.math.abs

/** Eine Seite in der Profil-Ansicht */
private sealed interface StoryPage {
    data class Photo(val url: String?) : StoryPage
    data object Info : StoryPage
}

/** Seite 1 = erstes Foto, Seite 2 = Bio und Interessen, danach die weiteren Fotos */
private fun storyPages(photos: List<String>): List<StoryPage> = buildList {
    add(StoryPage.Photo(photos.firstOrNull()))
    add(StoryPage.Info)
    photos.drop(1).forEach { add(StoryPage.Photo(it)) }
}

/**
 * Profil wie eine Story. Tippen irgendwo aufs Bild = nächste Seite,
 * Tippen am linken Rand = eine Seite zurück. Nach der letzten Seite geht es
 * wieder von vorne los.
 *
 * [header] erscheint auf allen Seiten unter den Fortschritts-Strichen
 * (z. B. Name und Alter), [bottomSpace] hält unten Platz für Buttons frei.
 */
@Composable
fun ProfileStory(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    topSpace: Dp = 14.dp,
    bottomSpace: Dp = 16.dp,
    header: @Composable () -> Unit = {},
    infoExtra: (@Composable () -> Unit)? = null,
) {
    val pages = remember(profile.uid, profile.photos) { storyPages(profile.photos) }
    var index by remember(profile.uid) { mutableIntStateOf(0) }
    val page = pages[index.coerceIn(0, pages.lastIndex)]
    val accent = parseColor(profile.color)

    Box(
        modifier
            .background(InkHigh)
            .then(
                if (interactive) {
                    Modifier.pointerInput(profile.uid, pages.size) {
                        detectTapGestures { pos ->
                            index = when {
                                pos.x < size.width * 0.22f -> (index - 1).coerceAtLeast(0)
                                index >= pages.lastIndex -> 0
                                else -> index + 1
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        when (page) {
            is StoryPage.Photo -> {
                if (page.url != null) {
                    AsyncImage(
                        model = page.url,
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Avatar("", profile.name, profile.color, 150.dp)
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.40f),
                                0.22f to Color.Transparent,
                                0.70f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.60f)
                            )
                        )
                )
            }
            StoryPage.Info -> {
                // Hintergrund: dasselbe Foto wie auf Seite 1, klar erkennbar, nur etwas dunkler
                profile.photos.firstOrNull()?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.18f),
                                0.45f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.30f)
                            )
                        )
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = topSpace, bottom = bottomSpace)
        ) {
            StoryBars(count = pages.size, current = index, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(14.dp))
            header()
            if (page == StoryPage.Info) {
                InfoPage(
                    profile = profile,
                    accent = accent,
                    extra = infoExtra,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StoryBars(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Color.White else Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

/** Leichter Schatten, damit weiße Schrift auf jedem Foto lesbar bleibt */
private val TextOnPhoto = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 10f))

/** Seite 2: Bio groß und gut lesbar, darunter die Interessen als Sticker */
@Composable
private fun InfoPage(profile: UserProfile, accent: Color, modifier: Modifier = Modifier, extra: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val bio = profile.bio.trim()
        if (bio.isNotEmpty()) {
            val long = bio.length > 160
            Text(
                text = bio,
                color = Color.White,
                fontSize = if (long) 18.sp else 22.sp,
                lineHeight = if (long) 25.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                style = TextOnPhoto,
            )
        } else {
            Text(
                text = "${profile.name} hat noch nichts über sich geschrieben.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                style = TextOnPhoto,
            )
        }
        if (profile.interests.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Sticker("Interessen", accent, rotation = -3f, fontSize = 20.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                profile.interests.forEach { InterestTile(it) }
            }
        }
        if (extra != null) {
            Spacer(Modifier.height(2.dp))
            extra()
        }
    }
}

/** Interesse als eigener Sticker, leicht schräg, mit kleinem Namen darunter */
@Composable
private fun InterestTile(interest: String) {
    val tilt = ((abs(interest.hashCode()) % 11) - 5).toFloat()
    Column(
        Modifier.widthIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InterestIcon(interest, 58.dp, Modifier.graphicsLayer { rotationZ = tilt })
        Text(
            interestLabel(interest),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextOnPhoto
        )
    }
}

/** Name und Land/Alter als Sticker – tippen öffnet das ganze Profil */
@Composable
fun ProfileNameStickers(profile: UserProfile, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val accent = parseColor(profile.color)
    Column(
        modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Sticker(profile.name, accent, rotation = -3f, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Sticker(
            if (profile.bot) "🤖 Testprofil" else "${Countries.flag(profile.country)} ${profile.country}, ${profile.age}",
            accent,
            rotation = -3f,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
