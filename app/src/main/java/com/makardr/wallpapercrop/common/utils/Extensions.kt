package com.makardr.wallpapercrop.common.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.data.model.LogTags
import java.io.File
import java.io.FileNotFoundException

fun Context.isTablet(): Boolean {
    return resources.configuration.smallestScreenWidthDp >= 600
}

@Suppress("DEPRECATION")
fun Activity.startActivitySlide(intent: Intent) {
    startActivity(intent)
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
}

fun Uri.available(context: Context): Boolean {
    return when (scheme) {
        "file" -> {
            val file = path?.let { File(it) }
            when {
                file == null -> {
                    Logger.logError(LogTags.Uri, "File URI has null path: $this")
                    false
                }

                !file.exists() -> {
                    Logger.logError(LogTags.Uri, "File does not exist: ${file.absolutePath}")
                    false
                }

                !file.canRead() -> {
                    Logger.logError(
                        LogTags.Uri,
                        "File exists but is not readable (permissions?): ${file.absolutePath}"
                    )
                    false
                }

                file.length() == 0L -> {
                    Logger.logError(
                        LogTags.Uri,
                        "File exists and is readable but is empty: ${file.absolutePath}"
                    )
                    false
                }

                else -> true
            }
        }

        "content" -> {
            try {
                context.contentResolver.openInputStream(this)?.use {
                    val hasBytes = it.read() != -1
                    if (!hasBytes) Logger.logError(
                        LogTags.Uri,
                        "Content URI opened successfully but stream is empty: $this"
                    )
                    hasBytes
                } ?: run {
                    Logger.logError(
                        LogTags.Uri,
                        "ContentResolver.openInputStream returned null for: $this"
                    )
                    false
                }
            } catch (e: SecurityException) {
                Logger.logError(
                    LogTags.Uri,
                    "Permission denied for URI: $this — grant may have expired or was never acquired. ${e.message}"
                )
                false
            } catch (e: FileNotFoundException) {
                Logger.logError(
                    LogTags.Uri,
                    "File not found via content provider: $this — provider registered but file missing. ${e.message}"
                )
                false
            } catch (e: Exception) {
                Logger.logError(
                    LogTags.Uri,
                    "Unexpected error reading URI: $this — ${e.javaClass.simpleName}: ${e.message}"
                )
                false
            }
        }

        null -> {
            Logger.logError(LogTags.Uri, "URI has null scheme: $this")
            false
        }

        else -> {
            Logger.logError(LogTags.Uri, "Unsupported URI scheme '${scheme}': $this")
            false
        }
    }
}