package com.powerguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.service.MonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Starts the monitoring service after a device reboot, but only if:
 *  - The user has previously enabled monitoring, AND
 *  - The "boot autostart" setting is on (default: true).
 *
 * [goAsync] keeps the receiver's process window open while the DataStore
 * reads complete; [PendingResult.finish] is called when done.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        val app = context.applicationContext as PowerGuardApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val monitoringEnabled = app.settingsRepository.isMonitoringEnabled().first()
                val bootAutostart = app.settingsRepository.isBootAutostartEnabled().first()

                if (monitoringEnabled && bootAutostart) {
                    val serviceIntent = MonitoringService.startIntent(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
