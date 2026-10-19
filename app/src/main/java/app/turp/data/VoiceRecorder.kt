package app.turp.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Nimmt eine Sprachnachricht als AAC (.m4a) auf. */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Wie lange schon aufgenommen wird (in Millisekunden) */
    val elapsedMs: Long get() = if (recorder != null) SystemClock.elapsedRealtime() - startedAt else 0L

    fun start(): Boolean {
        if (recorder != null) return true
        val dir = File(context.cacheDir, "voice").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "sprache_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = target
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            r.release()
            target.delete()
            false
        }
    }

    /** Beendet die Aufnahme. Liefert Datei und Dauer – oder null, wenn sie zu kurz war oder nicht klappte. */
    fun stop(): Pair<File, Long>? {
        val r = recorder ?: return null
        val target = file
        val duration = SystemClock.elapsedRealtime() - startedAt
        recorder = null
        file = null
        return try {
            r.stop()
            r.release()
            if (target != null && duration >= MIN_MS && target.length() > 0) {
                target to duration.coerceAtMost(MAX_MS)
            } else {
                target?.delete()
                null
            }
        } catch (e: Exception) {
            r.release()
            target?.delete()
            null
        }
    }

    fun cancel() {
        val r = recorder ?: return
        recorder = null
        try {
            r.stop()
        } catch (_: Exception) {
        }
        r.release()
        file?.delete()
        file = null
    }

    companion object {
        const val MIN_MS = 700L
        const val MAX_MS = 120_000L
    }
}
