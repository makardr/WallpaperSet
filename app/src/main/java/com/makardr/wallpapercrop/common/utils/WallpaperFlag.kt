package com.makardr.wallpapercrop.common.utils

import android.app.WallpaperManager
import androidx.annotation.IntDef

@IntDef(
    flag = true,
    value = [
        WallpaperManager.FLAG_SYSTEM,
        WallpaperManager.FLAG_LOCK
    ]
)
@Retention(AnnotationRetention.SOURCE)
annotation class WallpaperFlag