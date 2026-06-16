package com.makardr.wallpapercrop.common.utils

import android.content.Context
import android.net.Uri
import com.makardr.wallpapercrop.common.Tags
import java.io.File
import java.io.FileNotFoundException

fun Context.isTablet(): Boolean {
    return resources.configuration.smallestScreenWidthDp >= 600
}


fun Uri.available(context: Context): Boolean {
    return when (scheme) {
        "file" -> {
            val file = path?.let { File(it) }
            when {
                file == null -> {
                    Logger.logError(Tags.Uri, "File URI has null path: $this")
                    false
                }

                !file.exists() -> {
                    Logger.logError(Tags.Uri, "File does not exist: ${file.absolutePath}")
                    false
                }

                !file.canRead() -> {
                    Logger.logError(
                        Tags.Uri,
                        "File exists but is not readable (permissions?): ${file.absolutePath}"
                    )
                    false
                }

                file.length() == 0L -> {
                    Logger.logError(
                        Tags.Uri,
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
                        Tags.Uri,
                        "Content URI opened successfully but stream is empty: $this"
                    )
                    hasBytes
                } ?: run {
                    Logger.logError(
                        Tags.Uri,
                        "ContentResolver.openInputStream returned null for: $this"
                    )
                    false
                }
            } catch (e: SecurityException) {
                Logger.logError(
                    Tags.Uri,
                    "Permission denied for URI: $this — grant may have expired or was never acquired. ${e.message}"
                )
                false
            } catch (e: FileNotFoundException) {
                Logger.logError(
                    Tags.Uri,
                    "File not found via content provider: $this — provider registered but file missing. ${e.message}"
                )
                false
            } catch (e: Exception) {
                Logger.logError(
                    Tags.Uri,
                    "Unexpected error reading URI: $this — ${e.javaClass.simpleName}: ${e.message}"
                )
                false
            }
        }

        null -> {
            Logger.logError(Tags.Uri, "URI has null scheme: $this")
            false
        }

        else -> {
            Logger.logError(Tags.Uri, "Unsupported URI scheme '${scheme}': $this")
            false
        }
    }
}