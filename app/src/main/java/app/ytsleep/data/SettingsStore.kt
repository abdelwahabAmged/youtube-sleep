package app.ytsleep.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class RepeatMode { OFF, ONE, PLAYLIST }

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val repeatMode = stringPreferencesKey("repeat_mode")
        val lastSleepTimerMinutes = intPreferencesKey("last_sleep_timer_minutes")
        val batteryPromptShown = booleanPreferencesKey("battery_prompt_shown")
        val lastUpdateCheckAt = longPreferencesKey("last_update_check_at")
    }

    val repeatMode: Flow<RepeatMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.repeatMode]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() } ?: RepeatMode.OFF
    }

    val lastSleepTimerMinutes: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[Keys.lastSleepTimerMinutes]
    }

    val batteryPromptShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.batteryPromptShown] ?: false
    }

    val lastUpdateCheckAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.lastUpdateCheckAt] ?: 0L
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.dataStore.edit { it[Keys.repeatMode] = mode.name }
    }

    suspend fun setLastSleepTimerMinutes(minutes: Int?) {
        context.dataStore.edit {
            if (minutes == null) it.remove(Keys.lastSleepTimerMinutes)
            else it[Keys.lastSleepTimerMinutes] = minutes
        }
    }

    suspend fun setBatteryPromptShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.batteryPromptShown] = shown }
    }

    suspend fun setLastUpdateCheckAt(epochMs: Long) {
        context.dataStore.edit { it[Keys.lastUpdateCheckAt] = epochMs }
    }
}
