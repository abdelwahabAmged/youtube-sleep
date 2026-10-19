package app.ytsleep.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ytsleep.YtSleepApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val accountStatus: String = "DISCONNECTED",
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

    fun signOut() {
        viewModelScope.launch { container.accountStore.signOut() }
    }

    fun checkForUpdates() {
        viewModelScope.launch { container.updateChecker.checkNow() }
    }
}
