package androidx.camera.view
import androidx.camera.core.Preview
class PreviewView(context: android.content.Context) : android.widget.FrameLayout(context) {
    enum class ScaleType { FILL_CENTER, FIT_CENTER }
    enum class ImplementationMode { COMPATIBLE, PERFORMANCE }
    var scaleType: ScaleType = ScaleType.FILL_CENTER
    var implementationMode: ImplementationMode = ImplementationMode.PERFORMANCE
    val surfaceProvider: Preview.SurfaceProvider get() = TODO()
}
