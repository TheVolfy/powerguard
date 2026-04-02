package com.powerguard.app.util

import android.content.Context
import java.util.Calendar

object BirthdayPrefs {
    private const val PREFS       = "birthday_prefs"
    private const val KEY_YEAR    = "last_shown_year"

    private const val BIRTHDAY_MONTH = Calendar.APRIL  // 0-based month constant
    private const val BIRTHDAY_DAY   = 3

    /** Returns true if today is April 3rd and the overlay hasn't been shown yet this year. */
    fun shouldShow(context: Context): Boolean {
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH)       != BIRTHDAY_MONTH) return false
        if (today.get(Calendar.DAY_OF_MONTH) != BIRTHDAY_DAY)  return false

        val lastShownYear = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_YEAR, -1)
        return lastShownYear != today.get(Calendar.YEAR)
    }

    /** Saves the current year so the overlay won't show again until next April 3rd. */
    fun markShown(context: Context) {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_YEAR, year).commit()
    }
}
