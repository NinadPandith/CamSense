@file:Suppress("unused")
package androidx.navigation
import androidx.compose.runtime.Composable
abstract class NavType<T> { companion object { val StringType: NavType<String?> = TODO(); val BoolType: NavType<Boolean> = TODO() } }
class NamedNavArgument
class NavArgumentBuilder { var type: NavType<*>? = null; var defaultValue: Any? = null }
fun navArgument(name: String, builder: NavArgumentBuilder.() -> Unit): NamedNavArgument = TODO()
class NavBackStackEntry : androidx.lifecycle.ViewModelStoreOwner { val arguments: android.os.Bundle? = null }
class PopUpToBuilder { var inclusive: Boolean = false }
class NavOptionsBuilder {
    fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {}
    fun popUpTo(id: Int, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {}
}
open class NavController {
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {}
    fun popBackStack(): Boolean = TODO()
    fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean = false): Boolean = TODO()
    fun getBackStackEntry(route: String): NavBackStackEntry = TODO()
}
class NavHostController : NavController()
class NavGraphBuilder
