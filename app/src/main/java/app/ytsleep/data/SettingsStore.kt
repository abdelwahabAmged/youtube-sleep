package app.ytsleep.data

import app.ytsleep.youtube.AudioQuality
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
        val wifiQuality = stringPreferencesKey("audio_quality_wifi")
        val mobileQuality = stringPreferencesKey("audio_quality_mobile")
    }

    val wifiQuality: Flow<AudioQuality> = context.dataStore.data.map { prefs ->
        prefs[Keys.wifiQuality].toQuality() ?: AudioQuality.NORMAL
    }

    /** Defaults to the smallest stream: mobile data is the expensive network. */
    val mobileQuality: Flow<AudioQuality> = context.dataStore.data.map { prefs ->
        prefs[Keys.mobileQuality].toQuality() ?: AudioQuality.DATA_SAVER
    }

    suspend fun setWifiQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.wifiQuality] = quality.name }
    }

    suspend fun setMobileQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.mobileQuality] = quality.name }
    }

    private fun String?.toQuality(): AudioQuality? = this?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }

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
