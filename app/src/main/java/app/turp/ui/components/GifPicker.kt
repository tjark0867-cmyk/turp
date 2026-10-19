package app.turp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.data.Gif
import app.turp.data.Gifs
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextLow
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** GIF-Auswahl von unten: Suche, beliebte GIFs, Tippen = senden */
@Composable
fun GifPicker(onDismiss: () -> Unit, onPick: (Gif) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Gif>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(400) // erst suchen, wenn kurz nicht getippt wird
        loading = true
        try {
            results = if (query.isBlank()) Gifs.trending() else Gifs.search(query)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "GIFs konnten nicht geladen werden."
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = InkElevated,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 12.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it.take(50) },
                placeholder = { Text("GIFs suchen") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = InkHigh,
                    unfocusedContainerColor = InkHigh,
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 6.dp,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { it.id }) { gif ->
                        AsyncImage(
                            model = gif.previewUrl,
                            contentDescription = "GIF",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio((gif.width.toFloat() / gif.height).coerceIn(0.5f, 2.2f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(InkHigh)
                                .clickable { onPick(gif) }
                        )
                    }
                }
                when {
                    loading && results.isEmpty() ->
                        CircularProgressIndicator(color = Teal, modifier = Modifier.align(Alignment.Center))
                    error != null && results.isEmpty() ->
                        Text(error.orEmpty(), color = TextLow, modifier = Modifier.align(Alignment.Center))
                    !loading && results.isEmpty() ->
                        Text("Keine GIFs gefunden.", color = TextLow, modifier = Modifier.align(Alignment.Center))
                }
            }
            // Pflicht-Hinweis von GIPHY
            Text(
                "Powered by GIPHY",
                color = TextLow,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }
}
