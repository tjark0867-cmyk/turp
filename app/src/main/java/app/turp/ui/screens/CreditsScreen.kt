package app.turp.ui.screens

import app.turp.data.t

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.R
import app.turp.ui.components.InterestIcon
import app.turp.ui.components.interestDef
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow
import org.json.JSONArray

/** Ein Foto, auf dem ein Interessen-Sticker beruht */
private data class ImageCredit(
    val value: String,
    val title: String,
    val artist: String,
    val license: String,
    val url: String,
)

/** Bildnachweise für die Interessen-Sticker (Pflicht bei CC-BY-Lizenzen) */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val credits = remember {
        runCatching {
            val text = context.resources.openRawResource(R.raw.image_credits).bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ImageCredit(
                    value = o.optString("value"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    license = o.optString("license"),
                    url = o.optString("url"),
                )
            }
        }.getOrDefault(emptyList())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = t("Zurück"), tint = TextHigh, modifier = Modifier.size(22.dp))
            }
            Text(
                t("Lizenzen"),
                color = TextHigh,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    t("Viele Interessen-Sticker sind selbst gestaltet, einige beruhen auf freien Fotos von Wikimedia Commons. Für die Sticker wurden die Fotos freigestellt, zugeschnitten und mit einem Rand versehen. Sticker nach Fotos unter „CC BY-SA“ stehen unter derselben Lizenz. Fotos ohne Eintrag hier sind gemeinfrei (CC0 / Public Domain) oder selbst erstellt. Tippen öffnet die Quelle."),
                    color = TextLow,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(credits, key = { it.value }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(InkElevated, RoundedCornerShape(14.dp))
                        .clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.url))) }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InterestIcon(c.value, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            interestDef(c.value)?.label?.let { t(it) } ?: c.value,
                            color = TextHigh,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            t("Foto: {0} · {1}", c.artist.ifBlank { t("unbekannt") }, c.license),
                            color = Teal,
                            fontSize = 12.sp
                        )
                        Text(c.title.removePrefix("File:"), color = TextLow, fontSize = 11.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}
