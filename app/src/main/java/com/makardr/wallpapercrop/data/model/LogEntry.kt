package com.makardr.wallpapercrop.data.model

import android.util.Log
import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR, Log.ASSERT)
annotation class LogLevel

data class LogEntry(
    @property:LogLevel @param:LogLevel val level: Int,
    val tag: LogTags,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
