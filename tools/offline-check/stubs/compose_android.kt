@file:Suppress("unused")
package androidx.compose.ui.platform
import androidx.compose.runtime.staticCompositionLocalOf
val LocalContext = staticCompositionLocalOf<android.content.Context> { TODO() }
val LocalView = staticCompositionLocalOf<android.view.View> { TODO() }
