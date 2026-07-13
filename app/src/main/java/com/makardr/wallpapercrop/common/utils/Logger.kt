package com.makardr.wallpapercrop.common.utils

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.makardr.wallpapercrop.activities.main.viewmodels.ImageManagerViewModel
import com.makardr.wallpapercrop.data.model.LogTags

object Logger {
    private const val ENABLED: Boolean = true
    private const val DISABLED: Boolean = false

    fun logInfo(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.i(tag.toString(), message)
        }
    }

    fun logDebug(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.d(tag.toString(), message)
        }
    }

    fun logWarning(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.w(tag.toString(), message)
        }
    }

    fun logError(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.e(tag.toString(), message)
        }
    }

    fun logCurrentAppState(imageManager: ImageManagerViewModel, imagePreview: ImageView, tooltip: TextView) {
        if (ENABLED) {
            Log.d(LogTags.AppState.toString(), "--------------------------")
            Log.d(LogTags.AppState.toString(), "Image origin uri: ${imageManager.getOriginUri()}")
            Log.d(LogTags.AppState.toString(), "Image preview is empty: ${imagePreview.drawable == null}")
            Log.d(LogTags.AppState.toString(), "Tooltip visible: ${tooltip.isActivated}")
            Log.d(LogTags.AppState.toString(), "--------------------------")
        }
    }
}