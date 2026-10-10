package androidx.core.content
object ContextCompat {
    fun checkSelfPermission(context: android.content.Context, permission: String): Int = TODO()
    fun getMainExecutor(context: android.content.Context): java.util.concurrent.Executor = TODO()
    const val RECEIVER_NOT_EXPORTED = 4
    fun registerReceiver(context: android.content.Context, receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter, flags: Int): android.content.Intent? = TODO()
}

open class FileProvider {
    companion object {
        @JvmStatic fun getUriForFile(context: android.content.Context, authority: String, file: java.io.File): android.net.Uri = TODO()
    }
}
