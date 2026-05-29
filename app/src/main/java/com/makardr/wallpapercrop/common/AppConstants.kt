package com.makardr.wallpapercrop.common

import android.content.Context
import android.net.Uri
import java.io.File

object AppConstants {
    private const val WALLPAPER_CACHE_FILE_NAME = "cropped_wallpaper_cache.jpg"
    const val SAVED_INSTANCE_URI = "parcelableUri"
    const val SAVED_INSTANCE_IS_CROPPED =  "imageCropped"

    fun imageCacheOutputUri(context: Context): Uri =
        Uri.fromFile(File(context.filesDir, WALLPAPER_CACHE_FILE_NAME))
}