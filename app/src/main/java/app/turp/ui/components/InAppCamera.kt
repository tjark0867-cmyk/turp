package app.turp.ui.components

import app.turp.data.t

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.turp.ui.theme.InkHigh
import app.turp.ui.theme.Teal
import coil.compose.AsyncImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun cameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

/**
 * Eigene Kamera für den Chat. Selfies werden so gespeichert, wie man sie in
 * der Vorschau sieht – nach dem Auslösen wird also nichts gespiegelt.
 */
@Composable
fun InAppCamera(onClose: () -> Unit, onPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) {
            context.toast(t("Ohne Kamera-Erlaubnis geht das leider nicht."))
            onClose()
        }
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    var front by rememberSaveable { mutableStateOf(true) }
    var shot by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val capture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    LaunchedEffect(granted, front, shot) {
        if (!granted || shot != null) return@LaunchedEffect
        try {
            val provider = cameraProvider(context)
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
        } catch (e: Exception) {
            if (front) front = false else context.toast(t("Die Kamera ist gerade nicht verfügbar."))
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    future.get().unbindAll()
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun takePhoto() {
        if (busy) return
        busy = true
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
        // Frontkamera: wie in der Vorschau speichern (nicht nachträglich umdrehen)
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = front }
        val options = ImageCapture.OutputFileOptions.Builder(file).setMetadata(metadata).build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                busy = false
                shot = file
            }

            override fun onError(exception: ImageCaptureException) {
                busy = false
                context.toast(t("Das Foto konnte nicht aufgenommen werden."))
            }
        })
    }

    BackHandler {
        val current = shot
        if (current != null) {
            current.delete()
            shot = null
        } else {
            onClose()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val current = shot
        if (current == null) {
            if (granted) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = t("Schließen"), tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(52.dp))
                // Auslöser
                Box(
                    Modifier
                        .size(80.dp)
                        .border(5.dp, Color.White, CircleShape)
                        .padding(9.dp)
                        .background(if (busy) Color.White.copy(alpha = 0.45f) else Color.White, CircleShape)
                        .clickable(enabled = granted && !busy) { takePhoto() }
                )
                IconButton(
                    onClick = { front = !front },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = t("Kamera wechseln"), tint = Color.White)
                }
            }
        } else {
            // Vorschau des Fotos: nochmal oder senden
            AsyncImage(
                model = current,
                contentDescription = t("Aufgenommenes Foto"),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CandyButton(
                    text = t("Nochmal"),
                    onClick = {
                        current.delete()
                        shot = null
                    },
                    color = InkHigh,
                    modifier = Modifier.weight(1f)
                )
                CandyButton(
                    text = t("Senden"),
                    onClick = { onPhoto(Uri.fromFile(current)) },
                    color = Teal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
