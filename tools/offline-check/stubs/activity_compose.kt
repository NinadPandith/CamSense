@file:Suppress("unused")
package androidx.activity.compose
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
fun androidx.activity.ComponentActivity.setContent(content: @Composable () -> Unit) {}
@Composable fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {}
abstract class ManagedActivityResultLauncher<I, O> { abstract fun launch(input: I) }
@Composable fun <I, O> rememberLauncherForActivityResult(contract: ActivityResultContract<I, O>, onResult: (O) -> Unit): ManagedActivityResultLauncher<I, O> = TODO()
