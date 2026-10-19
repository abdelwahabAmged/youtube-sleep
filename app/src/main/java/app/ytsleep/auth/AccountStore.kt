package app.ytsleep.auth

import android.content.Context
import android.webkit.CookieManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AccountStatus { DISCONNECTED, CONNECTED, EXPIRED }

private val Context.accountDataStore by preferencesDataStore(name = "account")

class AccountStore(private val context: Context) {

    private object Keys {
        val cookies = stringPreferencesKey("cookies_json")
        val connectedAt = longPreferencesKey("connected_at")
        val expired = stringPreferencesKey("status")
    }

    val status: Flow<AccountStatus> = context.accountDataStore.data.map { prefs ->
        when {
            prefs[Keys.cookies].isNullOrEmpty() -> AccountStatus.DISCONNECTED
            prefs[Keys.expired] == "EXPIRED" -> AccountStatus.EXPIRED
            else -> AccountStatus.CONNECTED
        }
    }

    val cookies: Flow<Map<String, String>> = context.accountDataStore.data.map { prefs ->
        val json = prefs[Keys.cookies] ?: return@map emptyMap()
        runCatching { Json.decodeFromString<Map<String, String>>(json) }.getOrDefault(emptyMap())
    }

    suspend fun save(cookieMap: Map<String, String>) {
        context.accountDataStore.edit {
            it[Keys.cookies] = Json.encodeToString(cookieMap)
            it[Keys.connectedAt] = System.currentTimeMillis()
            it.remove(Keys.expired)
        }
    }

    suspend fun markExpired() {
        context.accountDataStore.edit { it[Keys.expired] = "EXPIRED" }
    }

    suspend fun signOut() {
        context.accountDataStore.edit {
            it.remove(Keys.cookies)
            it.remove(Keys.connectedAt)
            it.remove(Keys.expired)
        }
        CookieManager.getInstance().removeAllCookies(null)
    }
}
