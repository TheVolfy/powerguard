package com.powerguard.app.domain.model

import android.os.Build
import androidx.annotation.StringRes
import com.powerguard.app.R

/**
 * Canonical list of features PowerGuard can monitor.
 *
 * [nameRes] and [descriptionRes] are string resource IDs — use [stringResource] in Compose
 * or [Context.getString] in service/ViewModel code so they respect the active locale.
 *
 * [key] is a stable, locale-independent identifier used as a DataStore key and for DB entries.
 */
enum class FeatureType(
    val key: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val controlMode: ControlMode,
) {
    AUTO_ROTATE(
        key = "auto_rotate",
        nameRes = R.string.feature_auto_rotate,
        descriptionRes = R.string.feature_auto_rotate_desc,
        controlMode = ControlMode.DIRECT,
    ),
    SCREEN_BRIGHTNESS(
        key = "screen_brightness",
        nameRes = R.string.feature_brightness,
        descriptionRes = R.string.feature_brightness_desc,
        controlMode = ControlMode.DIRECT,
    ),
    AUTO_BRIGHTNESS(
        key = "auto_brightness",
        nameRes = R.string.feature_auto_brightness,
        descriptionRes = R.string.feature_auto_brightness_desc,
        controlMode = ControlMode.DIRECT,
    ),
    MEDIA_VOLUME(
        key = "media_volume",
        nameRes = R.string.feature_media_volume,
        descriptionRes = R.string.feature_media_volume_desc,
        controlMode = ControlMode.DIRECT,
    ),
    RINGER_MODE(
        key = "ringer_mode",
        nameRes = R.string.feature_ringer,
        descriptionRes = R.string.feature_ringer_desc,
        controlMode = ControlMode.DIRECT,
    ),
    BLUETOOTH(
        key = "bluetooth",
        nameRes = R.string.feature_bluetooth,
        descriptionRes = R.string.feature_bluetooth_desc,
        controlMode = ControlMode.CONDITIONAL,
    ),
    WIFI(
        key = "wifi",
        nameRes = R.string.feature_wifi,
        descriptionRes = R.string.feature_wifi_desc,
        controlMode = ControlMode.ASSISTED,
    ),
    NFC(
        key = "nfc",
        nameRes = R.string.feature_nfc,
        descriptionRes = R.string.feature_nfc_desc,
        controlMode = ControlMode.ASSISTED,
    );

    /** Resolve the effective control mode at runtime, accounting for API level. */
    fun resolvedControlMode(): ControlMode = when (this) {
        BLUETOOTH -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) ControlMode.DIRECT
                     else ControlMode.ASSISTED
        else -> controlMode
    }

    companion object {
        fun fromKey(key: String): FeatureType? = entries.find { it.key == key }
    }
}

enum class ControlMode {
    DIRECT,
    ASSISTED,
    CONDITIONAL,
}
