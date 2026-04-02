package com.powerguard.app.ui.screen.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.domain.model.ControlMode
import com.powerguard.app.domain.model.FeatureRule
import com.powerguard.app.domain.model.FeatureType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeatureListItem(
    val featureType: FeatureType,
    val rule: FeatureRule,
    val effectiveControlMode: ControlMode,
    /** Nullable — null means "state cannot be read" (assisted feature) */
    val isCurrentlyOn: Boolean?,
)

class FeaturesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PowerGuardApp

    val items: StateFlow<List<FeatureListItem>> =
        app.settingsRepository.getAllFeatureRules()
            .map { rules ->
                rules.mapNotNull { rule ->
                    val featureType = FeatureType.fromKey(rule.featureTypeKey) ?: return@mapNotNull null
                    FeatureListItem(
                        featureType = featureType,
                        rule = rule,
                        effectiveControlMode = featureType.resolvedControlMode(),
                        isCurrentlyOn = app.featureController.isFeatureOn(featureType),
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleEnabled(featureType: FeatureType, enabled: Boolean) {
        viewModelScope.launch {
            val existing = app.settingsRepository.getAllFeatureRulesOnce()
                .find { it.featureTypeKey == featureType.key }
                ?: FeatureRule(featureTypeKey = featureType.key)
            app.settingsRepository.saveFeatureRule(existing.copy(enabled = enabled))
        }
    }
}
