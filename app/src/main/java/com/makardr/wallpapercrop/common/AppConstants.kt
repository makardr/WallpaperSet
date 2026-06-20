package com.makardr.wallpapercrop.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object AppConstants {
    const val WALLPAPER_CACHE_FILE_NAME = "cropped_wallpaper_cache.jpg"

    fun imageCacheOutputUri(context: Context): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(context.cacheDir, WALLPAPER_CACHE_FILE_NAME)
        )
}