package com.powerguard.app.ui.screen.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powerguard.app.R
import com.powerguard.app.domain.model.ControlMode
import com.powerguard.app.domain.model.FeatureType
import com.powerguard.app.ui.theme.ColorAssisted
import com.powerguard.app.ui.theme.ColorDirect
import com.powerguard.app.ui.theme.ColorDisabled

@Composable
fun FeaturesScreen(
    onFeatureClick: (String) -> Unit,
    vm: FeaturesViewModel = viewModel(),
) {
    val items by vm.items.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(R.string.features_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Text(stringResource(R.string.features_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        items(items, key = { it.featureType.key }) { item ->
            FeatureCard(
                item = item,
                onToggle = { vm.toggleEnabled(item.featureType, it) },
                onClick = { onFeatureClick(item.featureType.key) },
            )
        }
    }
}

@Composable
private fun FeatureCard(
    item: FeatureListItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.featureType.icon(),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (item.rule.enabled) item.effectiveControlMode.tint() else ColorDisabled,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(item.featureType.nameRes), style = MaterialTheme.typography.titleSmall)
                    ControlModeBadge(item.effectiveControlMode)
                }
                Text(
                    text = if (item.rule.enabled)
                        stringResource(R.string.timeout_label, item.rule.timeoutMinutes)
                    else stringResource(R.string.automation_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = item.rule.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ControlModeBadge(mode: ControlMode) {
    val labelRes = when (mode) {
        ControlMode.DIRECT      -> R.string.control_direct
        ControlMode.ASSISTED    -> R.string.control_assisted
        ControlMode.CONDITIONAL -> R.string.control_conditional
    }
    val color = if (mode == ControlMode.DIRECT) ColorDirect else ColorAssisted
    Badge(containerColor = color.copy(alpha = 0.15f)) {
        Text(stringResource(labelRes), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private fun ControlMode.tint(): Color = if (this == ControlMode.DIRECT) ColorDirect else ColorAssisted

private fun FeatureType.icon(): ImageVector = when (this) {
    FeatureType.AUTO_ROTATE       -> Icons.Filled.ScreenRotation
    FeatureType.SCREEN_BRIGHTNESS -> Icons.Filled.BrightnessMedium
    FeatureType.AUTO_BRIGHTNESS   -> Icons.Filled.BrightnessAuto
    FeatureType.MEDIA_VOLUME      -> Icons.AutoMirrored.Filled.VolumeDown
    FeatureType.RINGER_MODE       -> Icons.AutoMirrored.Filled.VolumeOff
    FeatureType.BLUETOOTH         -> Icons.Filled.Bluetooth
    FeatureType.WIFI              -> Icons.Filled.Wifi
    FeatureType.NFC               -> Icons.Filled.Nfc
}
