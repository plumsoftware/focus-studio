package ru.plumsoftware.focusstudio.data

import android.content.Context

object AppPrefs {
    private const val PREFS_NAME = "focus_studio_prefs"
    private const val KEY_HAS_LAUNCHED_BEFORE = "has_launched_before"

    fun shouldShowLaunchAd(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_LAUNCHED_BEFORE, false)
    }

    fun markFirstLaunchComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_LAUNCHED_BEFORE, true)
            .apply()
    }
}
