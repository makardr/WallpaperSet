package com.makardr.wallpapercrop.common.utils

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.makardr.wallpapercrop.BuildConfig
import com.makardr.wallpapercrop.activities.main.viewmodels.ImageManagerViewModel
import com.makardr.wallpapercrop.data.LogsRepository
import com.makardr.wallpapercrop.data.model.LogEntry
import com.makardr.wallpapercrop.data.model.LogTags

object Logger {
    private val ENABLED: Boolean = BuildConfig.DEBUG
    private val logRepository: LogsRepository = LogsRepository.getInstance()

    fun logInfo(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.i(tag.toString(), message)
            logRepository.writeLog(LogEntry(Log.INFO, tag, message))
        }
    }

    fun logDebug(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.d(tag.toString(), message)
            logRepository.writeLog(LogEntry(Log.DEBUG, tag, message))
        }
    }

    fun logWarning(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.w(tag.toString(), message)
            logRepository.writeLog(LogEntry(Log.WARN, tag, message))
        }
    }

    fun logError(tag: LogTags, message: String) {
        if (ENABLED) {
            Log.e(tag.toString(), message)
            logRepository.writeLog(LogEntry(Log.ERROR, tag, message))
        }
    }

    fun logCurrentAppState(
        imageManager: ImageManagerViewModel,
        imagePreview: ImageView,
        tooltip: TextView
    ) {
        if (ENABLED) {
            Log.d(LogTags.AppState.toString(), "--------------------------")
            Log.d(LogTags.AppState.toString(), "Image origin uri: ${imageManager.getOriginUri()}")
            Log.d(
                LogTags.AppState.toString(),
                "Image preview is empty: ${imagePreview.drawable == null}"
            )
            Log.d(LogTags.AppState.toString(), "Tooltip visible: ${tooltip.isActivated}")
            Log.d(LogTags.AppState.toString(), "--------------------------")
        }
    }
}