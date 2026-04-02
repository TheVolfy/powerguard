package com.powerguard.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import com.powerguard.app.util.wrapWithSavedLocale
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.powerguard.app.PowerGuardApp
import com.powerguard.app.R
import com.powerguard.app.data.db.EventLogEntity
import com.powerguard.app.domain.model.FeatureRule
import com.powerguard.app.domain.model.FeatureType
import com.powerguard.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Foreground service that:
 *  1. Listens for screen-off / screen-on broadcasts via a dynamically registered receiver.
 *  2. Starts a coroutine loop once the screen turns off.
 *  3. Every [CHECK_INTERVAL_MS] ms, evaluates each enabled FeatureRule against elapsed
 *     screen-off time, checks exceptions, sends a warning notification before the timeout,
 *     then applies the action and writes an EventLog row.
 *  4. Resets state when the screen turns back on.
 *
 * The service is kept alive with startForeground(); Android will restart it (START_STICKY)
 * if the process is killed.
 */
class MonitoringService : Service() {

    // ---- Service-scoped coroutine dispatcher --------------------------------
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- Injected via Application class ------------------------------------
    private lateinit var app: PowerGuardApp

    // ---- Runtime state ------------------------------------------------------
    /** Epoch ms when the screen turned off; null while screen is on. */
    @Volatile private var screenOffTimestamp: Long? = null

    /** Features for which a warning notification has already been sent this cycle. */
    private val warnedFeatures = mutableSetOf<FeatureType>()

    /** Features already actioned this cycle (prevents re-firing on the same screen-off). */
    private val actionedFeatures = mutableSetOf<FeatureType>()

    /** Whether the user has temporarily paused automation. */
    @Volatile private var isPaused = false

    private var monitoringJob: Job? = null

    // ---- Screen state receiver ----------------------------------------------
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF   -> onScreenOff()
                Intent.ACTION_SCREEN_ON    -> onScreenOn()
                // ACTION_USER_PRESENT fires after the user unlocks the device
                Intent.ACTION_USER_PRESENT -> onUserPresent()
            }
        }
    }

    // ---- Lifecycle ----------------------------------------------------------

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.wrapWithSavedLocale())
    }

    override fun onCreate() {
        super.onCreate()
        app = application as PowerGuardApp
        createNotificationChannels()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                stopMonitoringJob()
                updateForegroundNotification()
            }
            ACTION_RESUME -> {
                isPaused = false
                updateForegroundNotification()
                // If screen is already off, resume monitoring immediately
                if (screenOffTimestamp != null) startMonitoringJob()
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Initial start or OS restart via START_STICKY
                startForeground(NOTIF_FOREGROUND, buildForegroundNotification())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
    }

    // ---- Screen events ------------------------------------------------------

    private fun onScreenOff() {
        if (isPaused) return
        screenOffTimestamp = System.currentTimeMillis()
        warnedFeatures.clear()
        actionedFeatures.clear()
        app.featureController.clearSavedStates()
        startMonitoringJob()
        updateForegroundNotification()
    }

    private fun onScreenOn() {
        screenOffTimestamp = null
        stopMonitoringJob()
        updateForegroundNotification()
    }

    /**
     * Called when the user has actually unlocked the device (ACTION_USER_PRESENT).
     * First performs normal screen-on cleanup, then restores any features that were
     * disabled by the app and have [restoreOnUnlock] enabled in their rule.
     */
    private fun onUserPresent() {
        val toRestore = actionedFeatures.toSet() // snapshot before onScreenOn may affect state
        onScreenOn()
        if (toRestore.isNotEmpty()) {
            serviceScope.launch { restoreActioned(toRestore) }
        }
    }

    private suspend fun restoreActioned(actioned: Set<FeatureType>) {
        val rules = app.settingsRepository.getAllFeatureRulesOnce()
        for (featureType in actioned) {
            val rule = rules.find { it.featureTypeKey == featureType.key } ?: continue
            if (!rule.restoreOnUnlock) continue
            val result = app.featureController.restoreFeature(featureType)
            if (result.success) {
                app.eventLogRepository.insert(
                    EventLogEntity(
                        timestamp = System.currentTimeMillis(),
                        featureTypeKey = featureType.key,
                        featureDisplayName = getString(featureType.nameRes),
                        action = getString(R.string.log_restored_on_unlock),
                        reason = getString(R.string.log_restored_reason),
                        wasDirectControl = result.wasDirectControl,
                    )
                )
            }
        }
    }

    // ---- Monitoring loop ----------------------------------------------------

    private fun startMonitoringJob() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkFeatureTimeouts()
            }
        }
    }

    private fun stopMonitoringJob() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun checkFeatureTimeouts() {
        val offTime = screenOffTimestamp ?: return
        val elapsedMinutes = (System.currentTimeMillis() - offTime) / 60_000L

        // Respect global pause-until timestamp set by the user
        val pauseUntil = app.settingsRepository.getPauseUntil().first()
        if (pauseUntil > System.currentTimeMillis()) return

        val rules = app.settingsRepository.getAllFeatureRulesOnce()

        for (rule in rules) {
            if (!rule.enabled) continue
            val featureType = FeatureType.fromKey(rule.featureTypeKey) ?: continue
            if (actionedFeatures.contains(featureType)) continue
            if (shouldSkipDueToException(rule)) continue

            // Warning notification
            if (rule.warnBeforeMinutes > 0 && featureType !in warnedFeatures) {
                val warnAtMinutes = (rule.timeoutMinutes - rule.warnBeforeMinutes).toLong()
                if (elapsedMinutes >= warnAtMinutes) {
                    val remaining = (rule.timeoutMinutes - elapsedMinutes).coerceAtLeast(1)
                    sendWarningNotification(featureType, remaining)
                    warnedFeatures.add(featureType)
                }
            }

            // Apply action at timeout
            if (elapsedMinutes >= rule.timeoutMinutes) {
                val result = app.featureController.applyAction(featureType)
                actionedFeatures.add(featureType)

                // Write event log
                app.eventLogRepository.insert(
                    EventLogEntity(
                        timestamp = System.currentTimeMillis(),
                        featureTypeKey = featureType.key,
                        featureDisplayName = getString(featureType.nameRes),
                        action = result.actionDescription,
                        reason = "Screen off ${elapsedMinutes}m (timeout ${rule.timeoutMinutes}m)",
                        wasDirectControl = result.wasDirectControl,
                    )
                )

                // Cancel the warning notification for this feature
                NotificationManagerCompat.from(this)
                    .cancel(NOTIF_WARNING_BASE + featureType.ordinal)
            }
        }
    }

    // ---- Exception checks ---------------------------------------------------

    private suspend fun shouldSkipDueToException(rule: FeatureRule): Boolean {
        if (rule.exceptWhenCharging && isCharging()) return true
        if (rule.exceptOnHomeWifi && isOnHomeWifi()) return true
        if (rule.exceptTimeWindowEnabled && isInTimeWindow(rule)) return true
        return false
    }

    private fun isCharging(): Boolean {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING
            || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @Suppress("MissingPermission")
    private suspend fun isOnHomeWifi(): Boolean {
        val homeSSID = app.settingsRepository.getHomeWifiSsid().first()
        if (homeSSID.isBlank()) return false

        // API 31+: read SSID via ConnectivityManager / NetworkCapabilities
        // API 29-30: fall back to deprecated WifiManager.connectionInfo (no replacement exists)
        val connected: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = applicationContext.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false)
            (caps?.transportInfo as? WifiInfo)?.ssid?.removeSurrounding("\"")
        } else {
            @Suppress("DEPRECATION")
            applicationContext.getSystemService(WifiManager::class.java)
                .connectionInfo?.ssid?.removeSurrounding("\"")
        }

        return connected == homeSSID
    }

    private fun isInTimeWindow(rule: FeatureRule): Boolean {
        val now = LocalTime.now()
        val start = LocalTime.of(rule.exceptTimeWindowStartHour, rule.exceptTimeWindowStartMin)
        val end = LocalTime.of(rule.exceptTimeWindowEndHour, rule.exceptTimeWindowEndMin)
        return if (start <= end) {
            now >= start && now <= end
        } else {
            // Overnight window: e.g., 22:00–07:00
            now >= start || now <= end
        }
    }

    // ---- Notifications ------------------------------------------------------

    private fun sendWarningNotification(featureType: FeatureType, remainingMinutes: Long) {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(this, CHANNEL_WARNINGS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.notif_warning_title, getString(featureType.nameRes)))
            .setContentText(getString(R.string.notif_warning_text, remainingMinutes))
            .setContentIntent(mainActivityPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIF_WARNING_BASE + featureType.ordinal, notification)
    }

    private fun buildForegroundNotification(): Notification {
        val statusText = when {
            isPaused -> getString(R.string.notif_status_paused)
            screenOffTimestamp != null -> getString(R.string.notif_status_monitoring)
            else -> getString(R.string.notif_status_waiting)
        }
        val toggleAction = if (isPaused) {
            NotificationCompat.Action(0, getString(R.string.notif_btn_resume), pendingServiceIntent(ACTION_RESUME))
        } else {
            NotificationCompat.Action(0, getString(R.string.notif_btn_pause), pendingServiceIntent(ACTION_PAUSE))
        }
        return NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(if (isPaused) getString(R.string.notif_paused_title) else getString(R.string.notif_active_title))
            .setContentText(statusText)
            .setContentIntent(mainActivityPendingIntent())
            .addAction(toggleAction)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateForegroundNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_FOREGROUND, buildForegroundNotification())
    }

    private fun mainActivityPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun pendingServiceIntent(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(),
        Intent(this, MonitoringService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canPostNotification(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    // ---- Notification channels (idempotent) ---------------------------------

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITORING, getString(R.string.notif_channel_monitoring), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.notif_channel_monitoring_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_WARNINGS, getString(R.string.notif_channel_warnings), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = getString(R.string.notif_channel_warnings_desc) }
        )
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    companion object {
        const val ACTION_PAUSE  = "com.powerguard.app.PAUSE"
        const val ACTION_RESUME = "com.powerguard.app.RESUME"
        const val ACTION_STOP   = "com.powerguard.app.STOP"

        const val CHANNEL_MONITORING = "powerguard_monitoring"
        const val CHANNEL_WARNINGS   = "powerguard_warnings"

        private const val NOTIF_FOREGROUND  = 1001
        private const val NOTIF_WARNING_BASE = 2000

        private const val CHECK_INTERVAL_MS = 30_000L  // 30 seconds

        fun startIntent(context: Context) = Intent(context, MonitoringService::class.java)

        fun pauseIntent(context: Context) =
            Intent(context, MonitoringService::class.java).setAction(ACTION_PAUSE)

        fun resumeIntent(context: Context) =
            Intent(context, MonitoringService::class.java).setAction(ACTION_RESUME)

        fun stopIntent(context: Context) =
            Intent(context, MonitoringService::class.java).setAction(ACTION_STOP)
    }
}
