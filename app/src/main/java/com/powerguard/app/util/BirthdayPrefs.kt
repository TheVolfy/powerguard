package com.powerguard.app.util

import android.content.Context

object BirthdayPrefs {
    private const val PREFS = "birthday_prefs"
    private const val KEY_SHOWN = "splash_shown"

    fun isShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOWN, false)

    /** commit() (synchronous) so the flag is persisted before Activity can be recreated. */
    fun markShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOWN, true).commit()
    }
}
