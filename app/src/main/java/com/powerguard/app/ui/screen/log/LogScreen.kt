package com.powerguard.app.ui.screen.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powerguard.app.R
import com.powerguard.app.domain.model.EventLogItem
import com.powerguard.app.ui.theme.ColorAssisted
import com.powerguard.app.ui.theme.ColorDirect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dtFormat = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())

@Composable
fun LogScreen(vm: LogViewModel = viewModel()) {
    val events by vm.events.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium)
            if (events.isNotEmpty()) {
                IconButton(onClick = { vm.clearAll() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                }
            }
        }

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: EventLogItem) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (event.wasDirectControl) Icons.Filled.AutoAwesome else Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (event.wasDirectControl) ColorDirect else ColorAssisted,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(event.featureDisplayName, style = MaterialTheme.typography.titleSmall)
                    Text(dtFormat.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(event.action, style = MaterialTheme.typography.bodyMedium)
                Text(event.reason, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val modeLabel = stringResource(if (event.wasDirectControl) R.string.log_direct else R.string.log_assisted)
                val modeColor = if (event.wasDirectControl) ColorDirect else ColorAssisted
                Badge(containerColor = modeColor.copy(alpha = 0.12f)) {
                    Text(modeLabel, color = modeColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
