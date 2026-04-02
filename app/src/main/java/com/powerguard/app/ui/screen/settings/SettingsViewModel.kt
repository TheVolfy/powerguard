package com.powerguard.app.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerguard.app.PowerGuardApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val homeWifiSsid: String = "",
    val logRetentionDays: Long = 30L,
    val bootAutostart: Boolean = true,
    val languageCode: String = "ru",
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PowerGuardApp

    val uiState: StateFlow<SettingsUiState> = combine(
        app.settingsRepository.getHomeWifiSsid(),
        app.settingsRepository.getLogRetentionDays(),
        app.settingsRepository.isBootAutostartEnabled(),
        app.settingsRepository.getLanguage(),
    ) { ssid, retention, boot, lang ->
        SettingsUiState(homeWifiSsid = ssid, logRetentionDays = retention,
            bootAutostart = boot, languageCode = lang)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setHomeWifiSsid(ssid: String) =
        viewModelScope.launch { app.settingsRepository.setHomeWifiSsid(ssid) }

    fun setLogRetentionDays(days: Long) =
        viewModelScope.launch { app.settingsRepository.setLogRetentionDays(days) }

    fun setBootAutostart(enabled: Boolean) =
        viewModelScope.launch { app.settingsRepository.setBootAutostart(enabled) }

    /**
     * Saves the new language code. The caller (SettingsScreen) is responsible for calling
     * [android.app.Activity.recreate] so [attachBaseContext] re-runs with the new locale.
     */
    fun setLanguage(code: String) =
        viewModelScope.launch { app.settingsRepository.setLanguage(code) }

    fun pruneLog() =
        viewModelScope.launch { app.eventLogRepository.pruneOlderThan(uiState.value.logRetentionDays) }
}
