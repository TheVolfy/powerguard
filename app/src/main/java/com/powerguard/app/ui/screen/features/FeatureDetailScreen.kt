package com.powerguard.app.ui.screen.features

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.R
import com.powerguard.app.domain.model.ControlMode
import com.powerguard.app.domain.model.FeatureRule
import com.powerguard.app.domain.model.FeatureType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---- ViewModel -----------------------------------------------------------

class FeatureDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val app = application as PowerGuardApp
    private val featureKey: String = checkNotNull(savedStateHandle["featureKey"])
    val featureType: FeatureType = FeatureType.fromKey(featureKey)
        ?: error("Unknown feature key: $featureKey")

    val rule: StateFlow<FeatureRule> =
        app.settingsRepository.getFeatureRule(featureType)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
                FeatureRule(featureTypeKey = featureKey))

    private fun save(transform: FeatureRule.() -> FeatureRule) {
        viewModelScope.launch { app.settingsRepository.saveFeatureRule(rule.value.transform()) }
    }

    fun setEnabled(v: Boolean)                 = save { copy(enabled = v) }
    fun setTimeoutMinutes(v: Int)              = save { copy(timeoutMinutes = v) }
    fun setWarnBeforeMinutes(v: Int)           = save { copy(warnBeforeMinutes = v) }
    fun setExceptWhenCharging(v: Boolean)      = save { copy(exceptWhenCharging = v) }
    fun setExceptOnHomeWifi(v: Boolean)        = save { copy(exceptOnHomeWifi = v) }
    fun setExceptTimeWindowEnabled(v: Boolean) = save { copy(exceptTimeWindowEnabled = v) }
    fun setExceptTimeWindowStartHour(v: Int)   = save { copy(exceptTimeWindowStartHour = v) }
    fun setExceptTimeWindowEndHour(v: Int)     = save { copy(exceptTimeWindowEndHour = v) }
    fun setRestoreOnUnlock(v: Boolean)         = save { copy(restoreOnUnlock = v) }
}

// ---- Screen --------------------------------------------------------------

@Composable
fun FeatureDetailScreen(
    featureKey: String,
    onBack: () -> Unit,
    vm: FeatureDetailViewModel = viewModel(),
) {
    val rule by vm.rule.collectAsStateWithLifecycle()
    val feature = vm.featureType

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(feature.nameRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_dashboard))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text(stringResource(feature.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            val modeLabelRes = when (feature.resolvedControlMode()) {
                ControlMode.DIRECT      -> R.string.mode_direct
                ControlMode.ASSISTED    -> R.string.mode_assisted
                ControlMode.CONDITIONAL -> R.string.mode_conditional
            }
            Text(stringResource(modeLabelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary)

            HorizontalDivider()

            SettingRow(stringResource(R.string.label_enable), stringResource(R.string.label_enable_sub)) {
                Switch(checked = rule.enabled, onCheckedChange = { vm.setEnabled(it) })
            }

            LabeledSlider(
                title = stringResource(R.string.label_timeout),
                value = rule.timeoutMinutes,
                valueRange = 1..120,
                unit = stringResource(R.string.label_min),
                onValueChange = { vm.setTimeoutMinutes(it) },
            )

            LabeledSlider(
                title = stringResource(R.string.label_warn),
                value = rule.warnBeforeMinutes,
                valueRange = 0..15,
                unit = stringResource(R.string.label_warn_unit),
                onValueChange = { vm.setWarnBeforeMinutes(it) },
            )

            HorizontalDivider()
            Text(stringResource(R.string.exceptions_title), style = MaterialTheme.typography.titleSmall)

            SettingRow(stringResource(R.string.except_charging), stringResource(R.string.except_charging_sub)) {
                Switch(checked = rule.exceptWhenCharging, onCheckedChange = { vm.setExceptWhenCharging(it) })
            }
            SettingRow(stringResource(R.string.except_home_wifi), stringResource(R.string.except_home_wifi_sub)) {
                Switch(checked = rule.exceptOnHomeWifi, onCheckedChange = { vm.setExceptOnHomeWifi(it) })
            }
            SettingRow(stringResource(R.string.except_time_window), stringResource(R.string.except_time_window_sub)) {
                Switch(checked = rule.exceptTimeWindowEnabled, onCheckedChange = { vm.setExceptTimeWindowEnabled(it) })
            }

            if (rule.exceptTimeWindowEnabled) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledSlider(
                            title = stringResource(R.string.window_start),
                            value = rule.exceptTimeWindowStartHour,
                            valueRange = 0..23,
                            unit = ":00",
                            onValueChange = { vm.setExceptTimeWindowStartHour(it) },
                        )
                        LabeledSlider(
                            title = stringResource(R.string.window_end),
                            value = rule.exceptTimeWindowEndHour,
                            valueRange = 0..23,
                            unit = ":00",
                            onValueChange = { vm.setExceptTimeWindowEndHour(it) },
                        )
                        Text(stringResource(R.string.overnight_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // "Restore on unlock" — only for DIRECT features where the app can reverse its action
            if (feature.resolvedControlMode() == ControlMode.DIRECT) {
                HorizontalDivider()
                Text(stringResource(R.string.restore_section_title), style = MaterialTheme.typography.titleSmall)
                SettingRow(
                    stringResource(R.string.label_restore_on_unlock),
                    stringResource(R.string.label_restore_on_unlock_sub),
                ) {
                    Switch(checked = rule.restoreOnUnlock, onCheckedChange = { vm.setRestoreOnUnlock(it) })
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}

@Composable
private fun LabeledSlider(title: String, value: Int, valueRange: IntRange, unit: String, onValueChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("$value $unit", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = valueRange.last - valueRange.first - 1,
        )
    }
}
