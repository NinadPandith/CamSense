@file:Suppress("unused")
package androidx.datastore.preferences.core
import androidx.datastore.core.DataStore
abstract class Preferences { class Key<T>(val name: String); abstract operator fun <T> get(key: Key<T>): T? }
abstract class MutablePreferences : Preferences() { abstract operator fun <T> set(key: Preferences.Key<T>, value: T); abstract fun clear(); abstract fun <T> remove(key: Preferences.Key<T>): T }
fun booleanPreferencesKey(name: String): Preferences.Key<Boolean> = Preferences.Key(name)
suspend fun DataStore<Preferences>.edit(transform: suspend (MutablePreferences) -> Unit): Preferences = TODO()
fun stringSetPreferencesKey(name: String): Preferences.Key<Set<String>> = Preferences.Key(name)
