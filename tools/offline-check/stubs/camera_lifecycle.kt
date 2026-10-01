package androidx.camera.lifecycle
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import com.google.common.util.concurrent.ListenableFuture
class ProcessCameraProvider {
    fun bindToLifecycle(owner: androidx.lifecycle.LifecycleOwner, selector: CameraSelector, vararg useCases: UseCase): Camera = TODO()
    fun unbindAll() {}
    fun unbind(vararg useCases: UseCase) {}
    companion object { fun getInstance(context: android.content.Context): ListenableFuture<ProcessCameraProvider> = TODO() }
}
