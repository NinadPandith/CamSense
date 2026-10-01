@file:Suppress("unused")
package androidx.lifecycle.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
abstract class CreationExtras
class InitializerViewModelFactoryBuilder
inline fun <reified VM : ViewModel> InitializerViewModelFactoryBuilder.initializer(noinline block: CreationExtras.() -> VM) {}
fun viewModelFactory(builder: InitializerViewModelFactoryBuilder.() -> Unit): ViewModelProvider.Factory = TODO()
