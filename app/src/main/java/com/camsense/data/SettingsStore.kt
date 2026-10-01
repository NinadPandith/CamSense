package com.camsense.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Device settings that don't belong in the scan database (Backend Schema doc). */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val IR_SUPPORTED = booleanPreferencesKey("ir_supported")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    /** null until the TV-remote calibration has run once. */
    val irSupported: Flow<Boolean?> = context.dataStore.data.map { it[Keys.IR_SUPPORTED] }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()
    suspend fun isIrSupported(): Boolean = irSupported.first() ?: false

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setIrSupported(supported: Boolean) {
        context.dataStore.edit { it[Keys.IR_SUPPORTED] = supported }
    }

    /** "Delete all data" keeps onboarding_done so the user isn't sent through onboarding again. */
    suspend fun clearAllExceptOnboarding() {
        context.dataStore.edit { prefs ->
            val done = prefs[Keys.ONBOARDING_DONE]
            prefs.clear()
            if (done != null) prefs[Keys.ONBOARDING_DONE] = done
        }
    }
}
