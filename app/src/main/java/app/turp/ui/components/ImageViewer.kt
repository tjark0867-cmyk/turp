package app.turp.ui.components

import app.turp.data.t

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import app.turp.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Foto im Vollbild. Zwei Finger oder Doppeltippen zoomen, gezoomt kann man verschieben.
 * Nach unten (oder oben) wischen, Zurück oder X schließen.
 * [model] ist alles, was Coil anzeigen kann (Adresse, Bilddaten, Uri).
 */
@Composable
fun ImageViewer(model: Any, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val scope = rememberCoroutineScope()
        var size by remember { mutableStateOf(IntSize.Zero) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        // Wischen zum Schließen (nur ungezoomt)
        val dismissDrag = remember { Animatable(0f) }

        fun clamp(o: Offset, s: Float): Offset {
            val maxX = (s - 1f) * size.width / 2f
            val maxY = (s - 1f) * size.height / 2f
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }

        val fade = (1f - abs(dismissDrag.value) / (size.height.coerceAtLeast(1) * 0.6f)).coerceIn(0.3f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = fade))
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > 1.01f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                scale = DOUBLE_TAP_ZOOM
                                offset = clamp((center - tap) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                        if (newScale > 1.01f || scale > 1.01f) {
                            scale = newScale
                            offset = clamp(offset + pan, newScale)
                        } else {
                            // Ungezoomt: senkrecht ziehen = schließen
                            scope.launch { dismissDrag.snapTo(dismissDrag.value + pan.y) }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Ende einer Wisch-Geste erkennen (ohne die Berührung zu verbrauchen)
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        if (abs(dismissDrag.value) > size.height * 0.18f) {
                            onDismiss()
                        } else if (dismissDrag.value != 0f) {
                            scope.launch { dismissDrag.animateTo(0f) }
                        }
                    }
                }
        ) {
            AsyncImage(
                model = model,
                contentDescription = t("Foto"),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y + dismissDrag.value
                    }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = t("Schließen"), tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}
