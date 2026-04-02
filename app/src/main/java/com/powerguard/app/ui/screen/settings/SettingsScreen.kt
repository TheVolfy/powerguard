package com.powerguard.app.ui.screen.settings

import android.app.Activity
import android.os.Build
import com.powerguard.app.data.datastore.LangPrefs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powerguard.app.R

private val LANGUAGES = listOf("ru" to R.string.lang_ru, "en" to R.string.lang_en)

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        // ---- Language selector -------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    LANGUAGES.forEach { (code, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.languageCode == code,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (state.languageCode != code) {
                                            // Write synchronously to SharedPreferences BEFORE
                                            // recreate() — vm.setLanguage() runs in a coroutine
                                            // and would not finish in time for attachBaseContext.
                                            LangPrefs.write(context, code)
                                            vm.setLanguage(code) // keeps DataStore in sync
                                            activity?.recreate()
                                        }
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(selected = state.languageCode == code, onClick = null)
                            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // ---- Home Wi-Fi --------------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_home_wifi), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_home_wifi_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                var ssidDraft by remember(state.homeWifiSsid) { mutableStateOf(state.homeWifiSsid) }
                OutlinedTextField(
                    value = ssidDraft,
                    onValueChange = { ssidDraft = it },
                    label = { Text(stringResource(R.string.settings_home_wifi_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.setHomeWifiSsid(ssidDraft) }),
                )
                FilledTonalButton(onClick = { vm.setHomeWifiSsid(ssidDraft) },
                    modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.btn_save))
                }
            }
        }

        // ---- Log retention ----------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.settings_retention), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_days, state.logRetentionDays),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = state.logRetentionDays.toFloat(),
                    onValueChange = { vm.setLogRetentionDays(it.toLong()) },
                    valueRange = 1f..365f,
                    steps = 363,
                )
                FilledTonalButton(onClick = { vm.pruneLog() }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.settings_prune))
                }
            }
        }

        // ---- Boot autostart --------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.settings_boot), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_boot_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.bootAutostart, onCheckedChange = { vm.setBootAutostart(it) })
            }
        }

        // ---- Device info -----------------------------------------------
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.settings_device_info), style = MaterialTheme.typography.titleSmall)
                HorizontalDivider()
                Text(stringResource(R.string.settings_api_level, Build.VERSION.SDK_INT),
                    style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        R.string.settings_bt_direct else R.string.settings_bt_assisted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
