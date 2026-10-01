package com.camsense.ui.components

import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

private const val TAG = "CamSense/Camera"

/**
 * Live camera preview plus an optional frame analyzer, bound to the screen's
 * lifecycle: the camera (and torch) stop when the app goes to the background or
 * the step is left. Preview and analysis both use 16:9 at about 720p so overlay
 * coordinates line up with the preview.
 *
 * Only this screen's own use cases are ever unbound. Screens overlap during the
 * navigation transition, so unbinding everything (or switching the torch off) from
 * the screen being left would stop the next screen's camera and freeze its preview.
 *
 * If no frame reaches the analyzer for a while, the camera is bound again.
 *
 * @param exposureBias -1..0: fraction of the lowest exposure compensation to apply
 *   (the lens check darkens the scene so lens reflections stand out).
 * @param onCamera called with the bound camera, and with null when it is released.
 */
@Composable
fun CameraPreview(
    lensFacing: Int,
    analyzer: ImageAnalysis.Analyzer?,
    modifier: Modifier = Modifier,
    torch: Boolean = false,
    exposureBias: Float = 0f,
    onCamera: ((Camera?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val lastFrameAt = remember { AtomicLong(0L) }
    var bindCount by remember { mutableIntStateOf(0) }
    // Wraps the analyzer so the watchdog can tell when frames stop arriving.
    val watched = remember(analyzer) {
        analyzer?.let { inner ->
            ImageAnalysis.Analyzer { image ->
                lastFrameAt.set(System.currentTimeMillis())
                inner.analyze(image)
            }
        }
    }

    DisposableEffect(lensFacing, watched, torch, lifecycleOwner, bindCount) {
        var provider: ProcessCameraProvider? = null
        var ownUseCases: Array<UseCase> = emptyArray()
        var torchOn = false
        var disposed = false
        lastFrameAt.set(System.currentTimeMillis()) // grace period while binding
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get().also { provider = it }
                val resolution = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy(
                        Size(ScanConfig.GLINT_TARGET_WIDTH, ScanConfig.GLINT_TARGET_HEIGHT),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build()
                val useCases = mutableListOf<UseCase>(
                    Preview.Builder().setResolutionSelector(resolution).build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                )
                if (watched != null) {
                    useCases += ImageAnalysis.Builder()
                        .setResolutionSelector(resolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, watched) }
                }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                // The newest screen takes the camera over; the one being left only
                // unbinds its own use cases in onDispose, which are gone by then.
                p.unbindAll()
                ownUseCases = useCases.toTypedArray()
                val camera = p.bindToLifecycle(lifecycleOwner, selector, *ownUseCases)
                if (torch && camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(true)
                    torchOn = true
                }
                applyExposureBias(camera, exposureBias)
                onCamera?.invoke(camera)
                Log.i(TAG, "Bound lens=$lensFacing torch=$torchOn analyzer=${watched != null} (bind #$bindCount)")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            onCamera?.invoke(null)
            try {
                provider?.unbind(*ownUseCases)
            } catch (e: Exception) {
                Log.w(TAG, "Camera release failed", e)
            }
        }
    }

    // Watchdog: if frames stop (camera taken away, HAL hiccup), bind again.
    if (watched != null) {
        LaunchedEffect(watched, lensFacing, torch, lifecycleOwner) {
            while (true) {
                delay(1_000)
                // In the background the camera is stopped on purpose.
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    lastFrameAt.set(System.currentTimeMillis())
                    continue
                }
                val silentMs = System.currentTimeMillis() - lastFrameAt.get()
                if (silentMs > ScanConfig.CAMERA_STALL_MS) {
                    Log.w(TAG, "No frames for $silentMs ms; binding the camera again")
                    bindCount++
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun applyExposureBias(camera: Camera, bias: Float) {
    if (bias >= 0f) return
    val state = camera.cameraInfo.exposureState
    if (!state.isExposureCompensationSupported) return
    val index = (state.exposureCompensationRange.lower * -bias).roundToInt()
    camera.cameraControl.setExposureCompensationIndex(index)
}
