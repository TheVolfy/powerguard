package com.powerguard.app.ui.screen.dashboard

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.R
import com.powerguard.app.service.MonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Describes one permission required by the app.
 *
 * Two kinds:
 *  - Settings-based (WRITE_SETTINGS, DND): [settingsAction] opens the correct system page.
 *  - Runtime: [runtimePermission] is the Manifest constant; the Composable launches the
 *    system dialog via [ActivityResultLauncher] — a ViewModel cannot do this directly.
 */
data class PermissionStatus(
    val labelRes: Int,
    val granted: Boolean,
    val rationaleRes: Int,
    /** Non-null → request via ActivityResultLauncher in the screen. */
    val runtimePermission: String? = null,
    /** Non-null → open a Settings page. Used when [runtimePermission] is null. */
    val settingsAction: (() -> Unit)? = null,
)

data class DashboardUiState(
    val monitoringEnabled: Boolean = false,
    val isPausedUntil: Long = 0L,
    val logCount: Int = 0,
    val enabledFeatureCount: Int = 0,
    val permissions: List<PermissionStatus> = emptyList(),
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PowerGuardApp
    private val ctx: Context get() = app

    private val _permissions = MutableStateFlow<List<PermissionStatus>>(emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        app.settingsRepository.isMonitoringEnabled(),
        app.settingsRepository.getPauseUntil(),
        app.eventLogRepository.observeCount(),
        app.settingsRepository.getAllFeatureRules(),
        _permissions,
    ) { enabled, pauseUntil, count, rules, perms ->
        DashboardUiState(
            monitoringEnabled = enabled,
            isPausedUntil = pauseUntil,
            logCount = count,
            enabledFeatureCount = rules.count { it.enabled },
            permissions = perms,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun refreshPermissions() {
        _permissions.update { buildPermissionList() }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.settingsRepository.setMonitoringEnabled(enabled)
            if (enabled) {
                val intent = MonitoringService.startIntent(ctx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ctx.startForegroundService(intent)
                else
                    ctx.startService(intent)
            } else {
                ctx.startService(MonitoringService.stopIntent(ctx))
            }
        }
    }

    fun pauseOneHour() {
        viewModelScope.launch {
            app.settingsRepository.setPauseUntil(System.currentTimeMillis() + 3_600_000L)
            ctx.startService(MonitoringService.pauseIntent(ctx))
        }
    }

    fun resumeNow() {
        viewModelScope.launch {
            app.settingsRepository.setPauseUntil(0L)
            ctx.startService(MonitoringService.resumeIntent(ctx))
        }
    }

    fun formatPauseUntil(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    // ---- Permission list ---------------------------------------------------

    private fun buildPermissionList(): List<PermissionStatus> = buildList {

        // 1. WRITE_SETTINGS — Settings-based special permission
        add(PermissionStatus(
            labelRes = R.string.perm_write_settings,
            granted = Settings.System.canWrite(ctx),
            rationaleRes = R.string.perm_write_settings_rationale,
            settingsAction = {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        ))

        // 2. POST_NOTIFICATIONS — runtime, API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionStatus(
                labelRes = R.string.perm_notifications,
                granted = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
                rationaleRes = R.string.perm_notifications_rationale,
                runtimePermission = Manifest.permission.POST_NOTIFICATIONS,
            ))
        }

        // 3. DND / Notification Policy — Settings-based special permission
        val nm = ctx.getSystemService(NotificationManager::class.java)
        add(PermissionStatus(
            labelRes = R.string.perm_dnd,
            granted = nm.isNotificationPolicyAccessGranted,
            rationaleRes = R.string.perm_dnd_rationale,
            settingsAction = {
                ctx.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        ))

        // 4. BLUETOOTH_CONNECT — runtime, API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            add(PermissionStatus(
                labelRes = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    R.string.perm_bt_control else R.string.perm_bt_state,
                granted = granted,
                rationaleRes = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    R.string.perm_bt_control_rationale else R.string.perm_bt_state_rationale,
                runtimePermission = Manifest.permission.BLUETOOTH_CONNECT,
            ))
        }

        // 5. ACCESS_FINE_LOCATION — runtime (optional, for home-Wi-Fi exception)
        add(PermissionStatus(
            labelRes = R.string.perm_location,
            granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            rationaleRes = R.string.perm_location_rationale,
            runtimePermission = Manifest.permission.ACCESS_FINE_LOCATION,
        ))
    }
}
