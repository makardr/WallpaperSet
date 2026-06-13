package com.makardr.wallpapercrop.common.utils

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.makardr.wallpapercrop.activities.main.ImageManagerViewModel
import com.makardr.wallpapercrop.common.Tags

object Logger {
    private const val ENABLED: Boolean = true
    private const val DISABLED: Boolean = false

    fun logInfo(tag: Tags, message: String) {
        if (ENABLED) {
            Log.i(tag.toString(), message)
        }
    }

    fun logDebug(tag: Tags, message: String) {
        if (ENABLED) {
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

    fun logCurrentAppState(imageManager: ImageManagerViewModel, imagePreview: ImageView, tooltip: TextView) {
        if (ENABLED) {
            Log.d(Tags.AppState.toString(), "--------------------------")
            Log.d(Tags.AppState.toString(), "Image origin uri: ${imageManager.getOriginUri()}")
            Log.d(Tags.AppState.toString(), "Is cropped: ${imageManager.imageIsCropped()}")
            Log.d(Tags.AppState.toString(), "Image preview is empty: ${imagePreview.drawable == null}")
            Log.d(Tags.AppState.toString(), "Tooltip visible: ${tooltip.isActivated}")
            Log.d(Tags.AppState.toString(), "--------------------------")
        }
    }
}