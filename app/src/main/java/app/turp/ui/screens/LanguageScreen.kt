package app.turp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.R
import app.turp.data.I18n
import app.turp.ui.components.TurpLogo
import app.turp.ui.theme.Display
import app.turp.ui.theme.Marker

private val InkBrown = Color(0xFF2B1A0C)

/**
 * Sprachauswahl im Pappe-Look – der allererste Bildschirm nach der Installation
 * und später über die Einstellungen erreichbar.
 */
@Composable
fun LanguageScreen(onDone: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(I18n.lang) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.kraft_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // oben verankert: Abriss und Aufdruck sitzen auf jedem Handy neben Logo/Titel
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TurpLogo(fontSize = 44.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    I18n.tIn(selected, "Wähle deine Sprache"),
                    fontFamily = Marker,
                    color = InkBrown,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { rotationZ = -2f }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    I18n.tIn(selected, "Du kannst sie später in den Einstellungen ändern."),
                    color = InkBrown.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                val lastAlone = I18n.LANGS.size % 2 == 1
                itemsIndexed(
                    I18n.LANGS,
                    key = { _, l -> l.code },
                    // ungerade Anzahl: das letzte Schild steht mittig statt allein links
                    span = { i, _ -> GridItemSpan(if (lastAlone && i == I18n.LANGS.lastIndex) 2 else 1) }
                ) { i, l ->
                    val sel = l.code == selected
                    val alone = lastAlone && i == I18n.LANGS.lastIndex
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (alone) 0.5f else 1f)
                            .height(58.dp)
                            .graphicsLayer {
                                rotationZ = if (sel) 0f else if (i % 3 == 0) -1.8f else if (i % 3 == 1) 1.2f else -0.6f
                                scaleX = if (sel) 1.04f else 1f
                                scaleY = if (sel) 1.04f else 1f
                            }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                selected = l.code
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(if (sel) R.drawable.tag_teal else R.drawable.tag_white),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                        Row(
                            Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(l.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                l.name,
                                fontFamily = Display,
                                color = InkBrown,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (sel) {
                                Spacer(Modifier.width(6.dp))
                                Text("✓", fontFamily = Display, color = InkBrown, fontSize = 18.sp)
                            }
                        }
                    }
                    }
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .fillMaxWidth()
                    .height(66.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onDone(selected)
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.tag_button),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
                Text(
                    I18n.tIn(selected, "Weiter") + "  →",
                    fontFamily = Display,
                    color = InkBrown,
                    fontSize = 22.sp
                )
            }
        }
    }
}
