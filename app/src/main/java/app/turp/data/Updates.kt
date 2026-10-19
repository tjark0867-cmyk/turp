package app.turp.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import app.turp.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Infos zu einer neueren App-Version */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Updates direkt in der App: Die GitHub-Builds legen neben der APK eine
 * version.json ab. Ist dort eine höhere Versionsnummer eingetragen, bietet die
 * App das Update an, lädt die APK und startet die Installation.
 */
object Updates {

    private val baseUrl: String get() = BuildConfig.UPDATE_URL

    val enabled: Boolean get() = baseUrl.isNotBlank()

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        val connection = URL("${baseUrl}version.json?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.useCaches = false
        try {
            if (connection.responseCode != 200) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val code = json.optInt("versionCode", 0)
            if (code <= BuildConfig.VERSION_CODE) return@withContext null
            UpdateInfo(
                versionCode = code,
                versionName = json.optString("versionName", "1.0.$code"),
                apkUrl = baseUrl + json.optString("apk", "turp.apk"),
                notes = json.optString("notes", ""),
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "turp-${info.versionCode}.apk")
            val connection = URL("${info.apkUrl}?v=${info.versionCode}").openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            try {
                if (connection.responseCode != 200) {
                    throw IllegalStateException("Download fehlgeschlagen (Fehler ${connection.responseCode}).")
                }
                val total = connection.contentLengthLong
                var done = 0L
                var lastPercent = -1
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            done += read
                            if (total > 0) {
                                val percent = (done * 100 / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent / 100f)
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            file
        }

    /**
     * Prüft, ob die geladene APK mit demselben Schlüssel signiert ist wie die
     * installierte App. Nur dann kann Android sie als Update installieren.
     * Lässt sich das nicht feststellen, wird es einfach versucht.
     */
    fun sameSigner(context: Context, file: File): Boolean = try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            true
        } else {
            val pm = context.packageManager
            val flags = PackageManager.GET_SIGNING_CERTIFICATES
            val apk = pm.getPackageArchiveInfo(file.path, flags)?.signingInfo?.apkContentsSigners
            val own = pm.getPackageInfo(context.packageName, flags).signingInfo?.apkContentsSigners
            apk.isNullOrEmpty() || own.isNullOrEmpty() || apk.toSet() == own.toSet()
        }
    } catch (e: Exception) {
        true
    }

    /** Lädt die neue Version im Browser herunter (für eine Neuinstallation). */
    fun openDownload(context: Context, info: UpdateInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${info.apkUrl}?v=${info.versionCode}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Öffnet den Android-Installer. Beim ersten Mal fragt Android nach der Erlaubnis. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

class UpdateViewModel : BaseViewModel() {

    var available by mutableStateOf<UpdateInfo?>(null)
        private set

    /** Fortschritt des Downloads (0..1) oder null, wenn gerade nichts geladen wird */
    var progress by mutableStateOf<Float?>(null)
        private set

    /** true, wenn die neue Version nicht über die alte installiert werden kann */
    var reinstallNeeded by mutableStateOf(false)
        private set

    private var checkedOnStart = false

    fun checkOnStart() {
        if (checkedOnStart) return
        checkedOnStart = true
        viewModelScope.launch {
            available = try {
                Updates.check()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    fun checkManually() = launchSafe(showBusy = false) {
        if (!Updates.enabled) {
            toast("Updates gibt es nur für die Version von GitHub.")
            return@launchSafe
        }
        val info = Updates.check()
        if (info == null) toast("Du hast schon die neueste Version ✅") else available = info
    }

    fun dismiss() {
        if (progress == null) {
            available = null
            reinstallNeeded = false
        }
    }

    fun update(context: Context) {
        val info = available ?: return
        if (progress != null) return
        viewModelScope.launch {
            progress = 0f
            try {
                val file = Updates.download(context.applicationContext, info) { p -> progress = p }
                if (Updates.sameSigner(context, file)) {
                    Updates.install(context, file)
                } else {
                    file.delete()
                    reinstallNeeded = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.toUserMessage())
            } finally {
                progress = null
            }
        }
    }
}
