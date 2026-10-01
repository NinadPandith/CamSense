@file:Suppress("unused")
package androidx.lifecycle
import kotlinx.coroutines.CoroutineScope
abstract class Lifecycle
interface LifecycleOwner { val lifecycle: Lifecycle }
abstract class ViewModel { protected open fun onCleared() {} }
val ViewModel.viewModelScope: CoroutineScope get() = TODO()
class SavedStateHandle { operator fun <T> get(key: String): T? = TODO() }
fun androidx.lifecycle.viewmodel.CreationExtras.createSavedStateHandle(): SavedStateHandle = TODO()
interface ViewModelStoreOwner
interface ViewModelProvider { interface Factory }
