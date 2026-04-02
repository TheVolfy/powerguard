package com.powerguard.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object PrefKeys {
    val MONITORING_ENABLED      = booleanPreferencesKey("monitoring_enabled")
    val PAUSE_UNTIL_TIMESTAMP   = longPreferencesKey("pause_until_timestamp")
    val HOME_WIFI_SSID          = stringPreferencesKey("home_wifi_ssid")
    val LOG_RETENTION_DAYS      = longPreferencesKey("log_retention_days")
    val BOOT_AUTOSTART          = booleanPreferencesKey("boot_autostart")
    /** BCP-47 language tag, e.g. "ru" or "en". Observed reactively by the UI. */
    val LANGUAGE_CODE           = stringPreferencesKey("language_code")

    fun featureRule(featureKey: String): Preferences.Key<String> =
        stringPreferencesKey("rule_$featureKey")
}

/**
 * SharedPreferences used exclusively for the language code.
 *
 * DataStore cannot be read synchronously, but [attachBaseContext] in Activity/Service/Application
 * must apply the locale before [super.attachBaseContext] returns — so we mirror the language
 * to SharedPreferences on every write and read it back synchronously here.
 *
 * Default: "ru" (Russian).
 */
object LangPrefs {
    private const val PREFS_NAME = "lang_prefs"
    private const val KEY_LANG   = "language"
    const val DEFAULT_LANG       = "ru"

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG

    fun write(context: Context, code: String) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, code).apply()
}
