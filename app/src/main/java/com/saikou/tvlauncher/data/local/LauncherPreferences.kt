package com.saikou.tvlauncher.data.local

import android.content.Context
import android.content.SharedPreferences

class LauncherPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    fun saveLauncherState(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    fun saveLauncherState(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    fun saveLauncherState(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun saveLauncherState(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getLauncherState(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getLauncherState(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getLauncherState(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun clearLauncherState() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        const val KEY_LAST_SCROLL_POSITION = "last_scroll_position"
        const val KEY_LAUNCHER_OPENED_COUNT = "launcher_opened_count"
        const val KEY_LAST_OPENED_TIME = "last_opened_time"
        const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        const val KEY_SELECTED_WALLPAPER = "selected_wallpaper"
        const val KEY_SELECTED_THEME = "selected_theme"
    }
}
