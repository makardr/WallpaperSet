package com.example.wallpaperfix.utils

import android.util.Log
import com.example.wallpaperfix.model.Tags

object Logger {
    private const val ENABLED: Boolean = true
    private const val DISABLED: Boolean = false

    fun logInfo(tag: Tags, message: String) {
        if (ENABLED) {
            Log.i(tag.toString(), message)
        }
    }

    fun logDebug(tag: Tags, message: String) {
        if (DISABLED) {
            Log.d(tag.toString(), message)
        }
    }

    fun logWarning(tag: Tags, message: String) {
        if (ENABLED) {
            Log.w(tag.toString(), message)
        }
    }

    fun logError(tag: Tags, message: String) {
        if (ENABLED) {
            Log.e(tag.toString(), message)
        }
    }
}