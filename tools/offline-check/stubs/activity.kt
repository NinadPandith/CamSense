@file:Suppress("unused")
package androidx.activity
open class ComponentActivity : android.app.Activity(), androidx.lifecycle.LifecycleOwner {
    override val lifecycle: androidx.lifecycle.Lifecycle get() = TODO()
}
class SystemBarStyle { companion object { fun dark(scrim: Int): SystemBarStyle = SystemBarStyle() } }
fun ComponentActivity.enableEdgeToEdge(statusBarStyle: SystemBarStyle = SystemBarStyle(), navigationBarStyle: SystemBarStyle = SystemBarStyle()) {}
