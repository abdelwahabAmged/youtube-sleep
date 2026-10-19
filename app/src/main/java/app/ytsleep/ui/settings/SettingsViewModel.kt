package app.ytsleep.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.AudioQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val accountStatus: String = "DISCONNECTED",
    val wifiQuality: AudioQuality = AudioQuality.NORMAL,
    val mobileQuality: AudioQuality = AudioQuality.DATA_SAVER,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as YtSleepApp).container
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.accountStore.status.collect { status ->
                _state.value = _state.value.copy(accountStatus = status.name)
            }
        }
    }

    init {
        viewModelScope.launch {
            container.settingsStore.wifiQuality.collect { _state.value = _state.value.copy(wifiQuality = it) }
        }
        viewModelScope.launch {
            container.settingsStore.mobileQuality.collect { _state.value = _state.value.copy(mobileQuality = it) }
        }
    }

    fun setWifiQuality(quality: AudioQuality) {
        viewModelScope.launch { container.settingsStore.setWifiQuality(quality) }
    }

    fun setMobileQuality(quality: AudioQuality) {
        viewModelScope.launch { container.settingsStore.setMobileQuality(quality) }
    }

    fun signOut() {
        viewModelScope.launch { container.accountStore.signOut() }
    }

    fun checkForUpdates() {
        viewModelScope.launch { container.updateChecker.checkNow() }
    }
}
