@file:Suppress("unused")
package androidx.camera.core
import java.nio.ByteBuffer
import java.util.concurrent.Executor
interface ImageInfo { val rotationDegrees: Int }
interface ImageProxy : AutoCloseable {
    interface PlaneProxy { val buffer: ByteBuffer; val rowStride: Int; val pixelStride: Int }
    val planes: Array<PlaneProxy>
    val width: Int
    val height: Int
    val imageInfo: ImageInfo
    fun toBitmap(): android.graphics.Bitmap
    override fun close()
}
abstract class UseCase
class ImageAnalysis : UseCase() {
    fun interface Analyzer { fun analyze(image: ImageProxy) }
    fun setAnalyzer(executor: Executor, analyzer: Analyzer) {}
    class Builder {
        fun setResolutionSelector(s: androidx.camera.core.resolutionselector.ResolutionSelector): Builder = this
        fun setBackpressureStrategy(s: Int): Builder = this
        fun build(): ImageAnalysis = ImageAnalysis()
    }
    companion object { const val STRATEGY_KEEP_ONLY_LATEST = 0 }
}
class Preview : UseCase() {
    fun interface SurfaceProvider { fun onSurfaceRequested(r: Any) }
    fun setSurfaceProvider(p: SurfaceProvider?) {}
    class Builder {
        fun setResolutionSelector(s: androidx.camera.core.resolutionselector.ResolutionSelector): Builder = this
        fun build(): Preview = Preview()
    }
}
interface CameraControl { fun enableTorch(on: Boolean): Any }
interface CameraInfo { fun hasFlashUnit(): Boolean }
interface Camera { val cameraControl: CameraControl; val cameraInfo: CameraInfo }
class CameraSelector {
    class Builder { fun requireLensFacing(f: Int): Builder = this; fun build(): CameraSelector = CameraSelector() }
    companion object { const val LENS_FACING_FRONT = 0; const val LENS_FACING_BACK = 1 }
}
