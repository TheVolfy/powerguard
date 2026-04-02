package com.powerguard.app.ui.screen.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powerguard.app.R

@Composable
fun DashboardScreen(
    onNavigateToFeatures: () -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Single launcher handles all runtime permissions (POST_NOTIFICATIONS, BLUETOOTH_CONNECT,
    // ACCESS_FINE_LOCATION). Result callback simply refreshes the permission list.
    val runtimePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshPermissions() }

    // Refresh permission states every time the screen resumes — covers the case where
    // the user granted a settings-based permission (WRITE_SETTINGS, DND) in the system
    // Settings UI and returned to the app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        // ---- Master switch ---------------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.monitoring_label), style = MaterialTheme.typography.titleMedium)
                    val statusText = when {
                        !state.monitoringEnabled -> stringResource(R.string.monitoring_off)
                        state.isPausedUntil > System.currentTimeMillis() ->
                            stringResource(R.string.monitoring_paused_until, vm.formatPauseUntil(state.isPausedUntil))
                        else -> stringResource(R.string.monitoring_active, state.enabledFeatureCount)
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.monitoringEnabled, onCheckedChange = { vm.setMonitoringEnabled(it) })
            }

            if (state.monitoringEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val paused = state.isPausedUntil > System.currentTimeMillis()
                    if (paused) {
                        Button(onClick = { vm.resumeNow() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_resume_now))
                        }
                    } else {
                        OutlinedButton(onClick = { vm.pauseOneHour() }) {
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_pause_1h))
                        }
                    }
                    FilledTonalButton(onClick = onNavigateToFeatures) {
                        Text(stringResource(R.string.btn_configure))
                    }
                }
            }
        }

        // ---- Stats ----------------------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.stats_events, state.logCount), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.stats_rules, state.enabledFeatureCount), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ---- Permissions ----------------------------------------------------
        if (state.permissions.any { !it.granted }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.permissions_needed),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(8.dp))
                    state.permissions.forEach { perm ->
                        PermissionRow(
                            perm = perm,
                            onGrantClick = {
                                if (perm.runtimePermission != null) {
                                    runtimePermLauncher.launch(perm.runtimePermission)
                                } else {
                                    perm.settingsAction?.invoke()
                                }
                            },
                        )
                    }
                }
            }
        } else if (state.permissions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text(stringResource(R.string.all_permissions_granted), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(perm: PermissionStatus, onGrantClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(perm.labelRes), style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            if (!perm.granted) Text(stringResource(perm.rationaleRes), style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Icon(
                imageVector = if (perm.granted) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (perm.granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
        },
        trailingContent = {
            if (!perm.granted) FilledTonalButton(onClick = onGrantClick) { Text(stringResource(R.string.btn_grant)) }
        },
    )
}
