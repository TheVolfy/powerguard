package com.powerguard.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.powerguard.app.data.datastore.LangPrefs
import com.powerguard.app.data.datastore.PrefKeys
import com.powerguard.app.data.datastore.dataStore
import com.powerguard.app.domain.model.FeatureRule
import com.powerguard.app.domain.model.FeatureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    // ---- Global ----------------------------------------------------------------

    fun isMonitoringEnabled(): Flow<Boolean> =
        dataStore.data.map { it[PrefKeys.MONITORING_ENABLED] ?: false }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.MONITORING_ENABLED] = enabled }
    }

    fun getPauseUntil(): Flow<Long> =
        dataStore.data.map { it[PrefKeys.PAUSE_UNTIL_TIMESTAMP] ?: 0L }

    suspend fun setPauseUntil(timestampMs: Long) {
        dataStore.edit { it[PrefKeys.PAUSE_UNTIL_TIMESTAMP] = timestampMs }
    }

    fun getHomeWifiSsid(): Flow<String> =
        dataStore.data.map { it[PrefKeys.HOME_WIFI_SSID] ?: "" }

    suspend fun setHomeWifiSsid(ssid: String) {
        dataStore.edit { it[PrefKeys.HOME_WIFI_SSID] = ssid }
    }

    fun getLogRetentionDays(): Flow<Long> =
        dataStore.data.map { it[PrefKeys.LOG_RETENTION_DAYS] ?: 30L }

    suspend fun setLogRetentionDays(days: Long) {
        dataStore.edit { it[PrefKeys.LOG_RETENTION_DAYS] = days }
    }

    fun isBootAutostartEnabled(): Flow<Boolean> =
        dataStore.data.map { it[PrefKeys.BOOT_AUTOSTART] ?: true }

    suspend fun setBootAutostart(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.BOOT_AUTOSTART] = enabled }
    }

    // ---- Language --------------------------------------------------------------

    /**
     * Observe the language code reactively — used by SettingsScreen to reflect current selection.
     * The source of truth for the synchronous read (used in attachBaseContext) is [LangPrefs].
     */
    fun getLanguage(): Flow<String> =
        dataStore.data.map { it[PrefKeys.LANGUAGE_CODE] ?: LangPrefs.DEFAULT_LANG }

    /**
     * Persists language to both DataStore (UI observation) and SharedPreferences
     * (synchronous read in [attachBaseContext]).
     */
    suspend fun setLanguage(code: String) {
        dataStore.edit { it[PrefKeys.LANGUAGE_CODE] = code }
        LangPrefs.write(context, code)
    }

    // ---- Feature rules ---------------------------------------------------------

    fun getFeatureRule(featureType: FeatureType): Flow<FeatureRule> =
        dataStore.data.map { prefs ->
            prefs[PrefKeys.featureRule(featureType.key)]?.decodeRule()
                ?: FeatureRule(featureTypeKey = featureType.key)
        }

    fun getAllFeatureRules(): Flow<List<FeatureRule>> =
        dataStore.data.map { prefs ->
            FeatureType.entries.map { ft ->
                prefs[PrefKeys.featureRule(ft.key)]?.decodeRule()
                    ?: FeatureRule(featureTypeKey = ft.key)
            }
        }

    suspend fun getAllFeatureRulesOnce(): List<FeatureRule> = getAllFeatureRules().first()

    suspend fun saveFeatureRule(rule: FeatureRule) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.featureRule(rule.featureTypeKey)] = Json.encodeToString(rule)
        }
    }

    private fun String.decodeRule(): FeatureRule? = try {
        Json.decodeFromString<FeatureRule>(this)
    } catch (_: Exception) {
        null
    }
}
