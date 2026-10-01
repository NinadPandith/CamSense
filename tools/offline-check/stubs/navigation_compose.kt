@file:Suppress("unused")
package androidx.navigation.compose
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
@Composable fun rememberNavController(): NavHostController = TODO()
@Composable fun NavHost(navController: NavHostController, startDestination: String, builder: NavGraphBuilder.() -> Unit) {}
fun NavGraphBuilder.composable(route: String, arguments: List<NamedNavArgument> = emptyList(), content: @Composable (NavBackStackEntry) -> Unit) {}
fun NavGraphBuilder.navigation(startDestination: String, route: String, arguments: List<NamedNavArgument> = emptyList(), builder: NavGraphBuilder.() -> Unit) {}
