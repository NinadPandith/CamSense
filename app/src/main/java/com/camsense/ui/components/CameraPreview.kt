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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.camsense.scoring.ScanConfig
import java.util.concurrent.Executors

/**
 * Live camera preview plus an optional frame analyzer, bound to the screen's
 * lifecycle: the camera (and torch) stop when the app goes to the background or
 * the step is left for any reason. Preview and analysis both use 16:9 at about
 * 720p so overlay coordinates line up with the preview.
 */
@Composable
fun CameraPreview(
    lensFacing: Int,
    analyzer: ImageAnalysis.Analyzer?,
    modifier: Modifier = Modifier,
    torch: Boolean = false,
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

    DisposableEffect(lensFacing, analyzer, torch, lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        var camera: Camera? = null
        var disposed = false
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
                if (analyzer != null) {
                    useCases += ImageAnalysis.Builder()
                        .setResolutionSelector(resolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                p.unbindAll()
                val bound = p.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
                camera = bound
                if (torch && bound.cameraInfo.hasFlashUnit()) bound.cameraControl.enableTorch(true)
            } catch (e: Exception) {
                Log.e("CamSense/Camera", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            try {
                camera?.cameraControl?.enableTorch(false)
                provider?.unbindAll()
            } catch (e: Exception) {
                Log.w("CamSense/Camera", "Camera release failed", e)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(factory = { previewView }, modifier = modifier)
}
