package com.makardr.wallpapercrop.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesRepository private constructor(context: Context) {
    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var galleryEnabled: Boolean
        get() = prefs.getBoolean(GALLERY_ENABLED_KEY, GALLERY_ENABLED_DEFAULT_VALUE)
        set(value) = prefs.edit { putBoolean(GALLERY_ENABLED_KEY, value).apply() }

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val GALLERY_ENABLED_KEY = "gallery_enabled"
        private const val GALLERY_ENABLED_DEFAULT_VALUE = true

        @Volatile
        private var INSTANCE: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }
}