package com.powerguard.app.service.controller

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.powerguard.app.R
import com.powerguard.app.domain.model.ControlMode
import com.powerguard.app.domain.model.FeatureType
import com.powerguard.app.service.MonitoringService

/**
 * Result of attempting to apply a feature action.
 *
 * @param success       False only on hard failures (missing critical system service, etc.)
 * @param actionDescription Human-readable summary written to the event log.
 * @param wasDirectControl  True if the system API was called directly; false for assisted.
 */
data class ActionResult(
    val success: Boolean,
    val actionDescription: String,
    val wasDirectControl: Boolean,
)

/**
 * Encapsulates all per-feature apply / query logic.
 *
 * Each [applyAction] call either:
 *  - Directly mutates a system setting (DIRECT features), or
 *  - Posts a notification with a PendingIntent to the correct Settings page (ASSISTED features).
 *
 * No business logic (timeouts, exceptions) lives here — that stays in [MonitoringService].
 */
class FeatureController(private val context: Context) {

    /**
     * Stores the original value of a setting before the app modifies it, so it can be
     * restored later if [restoreFeature] is called (e.g. on device unlock).
     * Keyed by FeatureType; cleared implicitly when a new screen-off cycle begins.
     */
    private val savedIntStates = mutableMapOf<FeatureType, Int>()

    fun applyAction(featureType: FeatureType): ActionResult = when (featureType) {
        FeatureType.AUTO_ROTATE       -> applyAutoRotate()
        FeatureType.SCREEN_BRIGHTNESS -> applyScreenBrightness()
        FeatureType.AUTO_BRIGHTNESS   -> applyAutoBrightness()
        FeatureType.MEDIA_VOLUME      -> applyMediaVolume()
        FeatureType.RINGER_MODE       -> applyRingerMode()
        FeatureType.BLUETOOTH         -> applyBluetooth()
        FeatureType.WIFI -> applyAssisted(context.getString(FeatureType.WIFI.nameRes), Settings.ACTION_WIFI_SETTINGS)
        FeatureType.NFC  -> applyAssisted(context.getString(FeatureType.NFC.nameRes), Settings.ACTION_NFC_SETTINGS)
    }

    // ---- DIRECT controllers ------------------------------------------------

    private fun applyAutoRotate(): ActionResult {
        if (!Settings.System.canWrite(context)) {
            return ActionResult(false, "Auto-Rotate skipped — WRITE_SETTINGS not granted", false)
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0,
        )
        return ActionResult(true, "Auto-Rotate disabled", true)
    }

    private fun applyScreenBrightness(): ActionResult {
        if (!Settings.System.canWrite(context)) {
            return ActionResult(false, "Brightness skipped — WRITE_SETTINGS not granted", false)
        }
        // Save original brightness before changing it
        savedIntStates[FeatureType.SCREEN_BRIGHTNESS] = Settings.System.getInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128
        )
        // First switch to manual mode so our value sticks
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        // 30 / 255 ≈ 12% — visible but clearly dimmed
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 30)
        return ActionResult(true, "Brightness reduced to 30/255", true)
    }

    private fun applyAutoBrightness(): ActionResult {
        if (!Settings.System.canWrite(context)) {
            return ActionResult(false, "Auto-Brightness skipped — WRITE_SETTINGS not granted", false)
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        return ActionResult(true, "Auto-Brightness disabled (manual mode)", true)
    }

    private fun applyMediaVolume(): ActionResult {
        val audio = context.getSystemService(AudioManager::class.java)
        savedIntStates[FeatureType.MEDIA_VOLUME] = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        return ActionResult(true, "Media volume set to 0", true)
    }

    private fun applyRingerMode(): ActionResult {
        val audio = context.getSystemService(AudioManager::class.java)
        savedIntStates[FeatureType.RINGER_MODE] = audio.ringerMode
        val nm = context.getSystemService(NotificationManager::class.java)
        return if (nm.isNotificationPolicyAccessGranted) {
            audio.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            ActionResult(true, "Ringer set to silent (DND)", true)
        } else {
            audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            ActionResult(true, "Ringer set to vibrate (DND not granted)", true)
        }
    }

    /**
     * Bluetooth is DIRECT on API 29–32 and ASSISTED on API 33+ where
     * [android.bluetooth.BluetoothAdapter.disable] requires BLUETOOTH_PRIVILEGED (signature).
     */
    @SuppressLint("MissingPermission")
    private fun applyBluetooth(): ActionResult {
        val bt = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return ActionResult(false, "Bluetooth hardware not available", false)

        if (!bt.isEnabled) {
            return ActionResult(true, "Bluetooth already off", true)
        }

        val effectiveMode = FeatureType.BLUETOOTH.resolvedControlMode()

        return if (effectiveMode == ControlMode.DIRECT) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // BLUETOOTH_ADMIN is a normal permission on API 29-30
            }
            if (hasPermission) {
                @Suppress("DEPRECATION")
                bt.disable()
                ActionResult(true, "Bluetooth disabled (API ${Build.VERSION.SDK_INT})", true)
            } else {
                sendAssistedNotification(context.getString(FeatureType.BLUETOOTH.nameRes), Settings.ACTION_BLUETOOTH_SETTINGS)
                ActionResult(true, "Bluetooth: BLUETOOTH_CONNECT not granted — notification sent", false)
            }
        } else {
            // API 33+ — direct toggle blocked for third-party apps
            sendAssistedNotification(context.getString(FeatureType.BLUETOOTH.nameRes), Settings.ACTION_BLUETOOTH_SETTINGS)
            ActionResult(true, "Bluetooth: assisted notification sent (API 33+)", false)
        }
    }

    // ---- ASSISTED controller -----------------------------------------------

    private fun applyAssisted(featureName: String, settingsAction: String): ActionResult {
        sendAssistedNotification(featureName, settingsAction)
        return ActionResult(true, "$featureName: notification with settings link sent", false)
    }

    /**
     * Posts a persistent notification whose tap target opens the relevant Settings page.
     * This is the only mechanism available for features Android restricts from third-party control.
     */
    private fun sendAssistedNotification(featureName: String, settingsAction: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val settingsIntent = Intent(settingsAction).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            settingsAction.hashCode(),
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, MonitoringService.CHANNEL_WARNINGS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.notif_assisted_title, featureName))
            .setContentText(context.getString(R.string.notif_assisted_text, featureName))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ASSISTED_NOTIF_BASE + settingsAction.hashCode(), notification)
    }

    // ---- Restore on unlock -------------------------------------------------

    /** Called at the start of each new screen-off cycle so stale saved values don't persist. */
    fun clearSavedStates() = savedIntStates.clear()

    /**
     * Attempts to reverse the change previously applied by [applyAction] for the given feature.
     * Only DIRECT-control features are supported; others return a failed result immediately.
     */
    @SuppressLint("MissingPermission")
    fun restoreFeature(featureType: FeatureType): ActionResult = when (featureType) {

        FeatureType.AUTO_ROTATE -> {
            if (!Settings.System.canWrite(context))
                ActionResult(false, "Auto-Rotate restore skipped — WRITE_SETTINGS not granted", false)
            else {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                ActionResult(true, "Auto-Rotate restored (enabled)", true)
            }
        }

        FeatureType.AUTO_BRIGHTNESS -> {
            if (!Settings.System.canWrite(context))
                ActionResult(false, "Auto-Brightness restore skipped — WRITE_SETTINGS not granted", false)
            else {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                )
                ActionResult(true, "Auto-Brightness restored (automatic mode)", true)
            }
        }

        FeatureType.SCREEN_BRIGHTNESS -> {
            if (!Settings.System.canWrite(context))
                ActionResult(false, "Brightness restore skipped — WRITE_SETTINGS not granted", false)
            else {
                val saved = savedIntStates[FeatureType.SCREEN_BRIGHTNESS] ?: 128
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, saved)
                ActionResult(true, "Brightness restored to $saved/255", true)
            }
        }

        FeatureType.MEDIA_VOLUME -> {
            val audio = context.getSystemService(AudioManager::class.java)
            val saved = savedIntStates[FeatureType.MEDIA_VOLUME]
                ?: (audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, saved, 0)
            ActionResult(true, "Media volume restored to $saved", true)
        }

        FeatureType.RINGER_MODE -> {
            val audio = context.getSystemService(AudioManager::class.java)
            val nm = context.getSystemService(NotificationManager::class.java)
            val saved = savedIntStates[FeatureType.RINGER_MODE] ?: AudioManager.RINGER_MODE_NORMAL
            // Only restore to SILENT if DND access is still granted
            val target = if (saved == AudioManager.RINGER_MODE_SILENT && !nm.isNotificationPolicyAccessGranted)
                AudioManager.RINGER_MODE_NORMAL else saved
            audio.setRingerMode(target)
            ActionResult(true, "Ringer mode restored", true)
        }

        FeatureType.BLUETOOTH -> {
            if (FeatureType.BLUETOOTH.resolvedControlMode() != ControlMode.DIRECT) {
                ActionResult(false, "Bluetooth restore skipped — ASSISTED mode on this API level", false)
            } else {
                val bt = context.getSystemService(BluetoothManager::class.java)?.adapter
                if (bt == null) {
                    ActionResult(false, "Bluetooth hardware not available", false)
                } else if (bt.isEnabled) {
                    ActionResult(true, "Bluetooth already on", true)
                } else {
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    else true
                    if (hasPermission) {
                        @Suppress("DEPRECATION")
                        bt.enable()
                        ActionResult(true, "Bluetooth re-enabled on unlock", true)
                    } else {
                        ActionResult(false, "Bluetooth restore skipped — BLUETOOTH_CONNECT not granted", false)
                    }
                }
            }
        }

        // ASSISTED features — the app never modified system state directly, nothing to restore
        FeatureType.WIFI, FeatureType.NFC ->
            ActionResult(false, "${featureType.key} restore not applicable (ASSISTED)", false)
    }

    // ---- Current state queries (used by UI) --------------------------------

    fun isFeatureOn(featureType: FeatureType): Boolean? = runCatching {
        when (featureType) {
            FeatureType.AUTO_ROTATE -> Settings.System.getInt(
                context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1
            ) == 1
            FeatureType.AUTO_BRIGHTNESS -> Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            FeatureType.MEDIA_VOLUME -> {
                val audio = context.getSystemService(AudioManager::class.java)
                audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
            }
            FeatureType.RINGER_MODE -> {
                val audio = context.getSystemService(AudioManager::class.java)
                audio.ringerMode != AudioManager.RINGER_MODE_SILENT
            }
            @SuppressLint("MissingPermission")
            FeatureType.BLUETOOTH -> {
                context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
            }
            // For assisted features we can only infer; return null to show "unknown"
            FeatureType.WIFI, FeatureType.NFC, FeatureType.SCREEN_BRIGHTNESS -> null
        }
    }.getOrNull()

    companion object {
        private const val ASSISTED_NOTIF_BASE = 3000
    }
}
