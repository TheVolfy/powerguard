package com.powerguard.app.util

import android.content.Context
import android.content.res.Configuration
import com.powerguard.app.data.datastore.LangPrefs
import java.util.Locale

/**
 * Wraps [base] in a configuration context whose locale is the user's saved language.
 * Called from [attachBaseContext] in Application, Activity, and Service.
 *
 * The language is read synchronously from SharedPreferences via [LangPrefs] — safe here
 * because this runs before any coroutines are started and SharedPreferences is an in-process,
 * synchronous store backed by a file already loaded by the OS.
 */
fun Context.wrapWithSavedLocale(): Context {
    val code = LangPrefs.read(this)
    val locale = Locale(code)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
