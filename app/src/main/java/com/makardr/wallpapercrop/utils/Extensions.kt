package com.makardr.wallpapercrop.utils

import android.content.Context

fun Context.isTablet(): Boolean {
    return resources.configuration.smallestScreenWidthDp >= 600
}