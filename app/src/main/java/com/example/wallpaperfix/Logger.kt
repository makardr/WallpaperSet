package com.example.wallpaperfix

import android.util.Log

object Logger {
    private const val ENABLED: Boolean = true

    fun log(tag: Tags, message: String) {
        if (ENABLED) {
            Log.d(tag.toString(), message)
        }
    }
}