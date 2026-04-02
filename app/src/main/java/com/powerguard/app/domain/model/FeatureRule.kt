package com.powerguard.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Per-feature automation rule.  Stored as a JSON blob in DataStore Preferences,
 * one entry per [FeatureType.key].
 *
 * Default values represent a safe, no-op starting state (enabled = false).
 */
@Serializable
data class FeatureRule(
    val featureTypeKey: String,

    /** Whether automation for this feature is active. */
    val enabled: Boolean = false,

    /** Minutes of screen-off time before the action is applied. */
    val timeoutMinutes: Int = 30,

    /**
     * Minutes *before* the timeout at which a warning notification is sent.
     * 0 = no warning.
     */
    val warnBeforeMinutes: Int = 5,

    // ---- Exceptions --------------------------------------------------------

    /** Skip action while the device is charging. */
    val exceptWhenCharging: Boolean = false,

    /** Skip action while connected to the home Wi-Fi network. */
    val exceptOnHomeWifi: Boolean = false,

    /** Skip action if the current time falls inside a defined window. */
    val exceptTimeWindowEnabled: Boolean = false,
    val exceptTimeWindowStartHour: Int = 22,
    val exceptTimeWindowStartMin: Int = 0,
    val exceptTimeWindowEndHour: Int = 7,
    val exceptTimeWindowEndMin: Int = 0,

    // ---- Restore on unlock -------------------------------------------------

    /**
     * If true, when the user unlocks the device after the app has applied this feature's
     * action, the app will restore the previous state.
     * Only meaningful for DIRECT-control features where the app can reverse its own change.
     */
    val restoreOnUnlock: Boolean = false,
)
