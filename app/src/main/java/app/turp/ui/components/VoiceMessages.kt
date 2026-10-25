package app.turp.ui.components

import app.turp.data.t

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.turp.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.turp.ui.theme.Coral
import app.turp.ui.theme.Ink
import app.turp.ui.theme.InkElevated
import app.turp.ui.theme.Teal
import app.turp.ui.theme.TextHigh
import app.turp.ui.theme.TextLow

/** "0:07", "1:23" */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** Spielt Sprachnachrichten ab – immer nur eine gleichzeitig. */
class VoicePlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    var playingId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playingId = null
                    progress = 0f
                }
            }
        })
    }

    fun toggle(id: String, url: String) {
        if (playingId == id) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        playingId = id
        progress = 0f
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun tick() {
        val duration = player.duration
        if (playingId != null && duration > 0) progress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    }

    fun release() = player.release()
}

/** Inhalt einer Sprachnachricht: Abspielen, Wellenform mit Fortschritt, Dauer */
@Composable
fun VoiceMessageContent(
    id: String,
    status: String,
    durationMs: Long,
    playing: Boolean,
    progress: Float,
    mine: Boolean,
    time: String,
    onToggle: () -> Unit,
) {
    val fg = if (mine) Ink else TextHigh
    val weak = if (mine) Ink.copy(alpha = 0.35f) else TextHigh.copy(alpha = 0.3f)
    Row(
        Modifier
            .width(236.dp)
            .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(if (mine) Ink.copy(alpha = 0.14f) else Teal.copy(alpha = 0.18f), CircleShape)
                .clickable(enabled = status == "ok", onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                "ok" -> Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) t("Pause") else t("Abspielen"),
                    tint = if (mine) Ink else Teal,
                    modifier = Modifier.size(24.dp)
                )
                "pending" -> CircularProgressIndicator(
                    color = if (mine) Ink else Teal,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                else -> Text("🚫", fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        // Wellenform: feste Balkenhöhen pro Nachricht, gespielter Teil kräftig
        val bars = 26
        val seed = id.hashCode() and 0x7fffffff
        Row(
            Modifier
                .weight(1f)
                .height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(bars) { i ->
                val h = 6 + ((seed / (i + 1) + i * 7) % 20)
                val played = progress > 0f && i.toFloat() / bars <= progress
                Box(
                    Modifier
                        .weight(1f)
                        .height(h.dp)
                        .background(if (played) fg else weak, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatDuration(durationMs),
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(6.dp))
        Text(time, color = if (mine) Ink.copy(alpha = 0.55f) else TextLow, fontSize = 11.sp)
    }
}

/** Leiste während der Aufnahme: Verwerfen | roter Punkt + Zeit | Senden */
@Composable
fun RecordingBar(elapsedMs: Long, onCancel: () -> Unit, onSend: () -> Unit, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "rec")
    val dot by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "recDot"
    )
    Surface(color = InkElevated, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Delete, contentDescription = t("Verwerfen"), tint = Coral)
            }
            Box(
                Modifier
                    .size(12.dp)
                    .alpha(dot)
                    .background(Coral, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(formatDuration(elapsedMs), color = TextHigh, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                t("Aufnahme läuft …"),
                color = TextLow,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .size(46.dp)
                    .background(Teal, CircleShape)
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = t("Senden"), tint = Ink, modifier = Modifier.size(22.dp))
            }
        }
    }
}
