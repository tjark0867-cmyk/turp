package app.turp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.R
import app.turp.ui.theme.Ink
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh

/** Eigene Sticker für jedes Interesse (Schlüssel = Name ohne Emoji) */
private val INTEREST_ICONS = mapOf(
    "Gaming" to R.drawable.interest_gaming,
    "Musik" to R.drawable.interest_musik,
    "Singen" to R.drawable.interest_singen,
    "Instrumente" to R.drawable.interest_instrumente,
    "Fußball" to R.drawable.interest_fussball,
    "Basketball" to R.drawable.interest_basketball,
    "Fitness" to R.drawable.interest_fitness,
    "Skaten" to R.drawable.interest_skaten,
    "Zeichnen" to R.drawable.interest_zeichnen,
    "Fotografie" to R.drawable.interest_fotografie,
    "Filme" to R.drawable.interest_filme,
    "Serien" to R.drawable.interest_serien,
    "Lesen" to R.drawable.interest_lesen,
    "Schreiben" to R.drawable.interest_schreiben,
    "Kochen" to R.drawable.interest_kochen,
    "Tiere" to R.drawable.interest_tiere,
    "Natur" to R.drawable.interest_natur,
    "Reisen" to R.drawable.interest_reisen,
    "Tanzen" to R.drawable.interest_tanzen,
    "Mode" to R.drawable.interest_mode,
    "Coden" to R.drawable.interest_coden,
    "Wissenschaft" to R.drawable.interest_wissenschaft,
    "Anime" to R.drawable.interest_anime,
    "Brettspiele" to R.drawable.interest_brettspiele,
    "Yoga" to R.drawable.interest_yoga,
    "Schwimmen" to R.drawable.interest_schwimmen,
    "Radfahren" to R.drawable.interest_radfahren,
    "Sprachen" to R.drawable.interest_sprachen,
    "Memes" to R.drawable.interest_memes,
    "Podcasts" to R.drawable.interest_podcasts,
)

/** "🎮 Gaming" → "Gaming" */
fun interestLabel(interest: String): String {
    val t = interest.trim()
    return if (t.contains(" ")) t.substringAfter(" ") else t
}

/** Sticker zum Interesse – oder das Emoji, falls es (noch) keinen gibt */
@Composable
fun InterestIcon(interest: String, size: Dp, modifier: Modifier = Modifier) {
    val res = INTEREST_ICONS[interestLabel(interest)]
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = interestLabel(interest),
            modifier = modifier.size(size)
        )
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(interest.trim().substringBefore(" "), fontSize = (size.value * 0.55f).sp)
        }
    }
}

/** Auswahl-Kachel: Sticker mit Namen, ausgewählt mit türkisem Rahmen und Haken */
@Composable
fun InterestPickTile(interest: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box {
        Column(
            Modifier
                .width(78.dp)
                .clip(shape)
                .background(if (selected) Teal.copy(alpha = 0.16f) else Color.Transparent, shape)
                .border(if (selected) 2.dp else 0.dp, if (selected) Teal else Color.Transparent, shape)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InterestIcon(interest, 50.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                interestLabel(interest),
                color = TextHigh,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .background(Teal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(13.dp))
            }
        }
    }
}
