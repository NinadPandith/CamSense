@file:Suppress("unused")
package androidx.lifecycle
import kotlinx.coroutines.CoroutineScope
abstract class Lifecycle {
    enum class State { DESTROYED, INITIALIZED, CREATED, STARTED, RESUMED; fun isAtLeast(state: State) = compareTo(state) >= 0 }
    abstract val currentState: State
}
interface LifecycleOwner { val lifecycle: Lifecycle }
abstract class ViewModel { protected open fun onCleared() {} }
val ViewModel.viewModelScope: CoroutineScope get() = TODO()
class SavedStateHandle { operator fun <T> get(key: String): T? = TODO() }
fun androidx.lifecycle.viewmodel.CreationExtras.createSavedStateHandle(): SavedStateHandle = TODO()
interface ViewModelStoreOwner
interface ViewModelProvider { interface Factory }
