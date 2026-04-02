package com.powerguard.app.ui.screen.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.domain.model.EventLogItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PowerGuardApp

    val events: StateFlow<List<EventLogItem>> =
        app.eventLogRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearAll() {
        viewModelScope.launch { app.eventLogRepository.clearAll() }
    }
}
