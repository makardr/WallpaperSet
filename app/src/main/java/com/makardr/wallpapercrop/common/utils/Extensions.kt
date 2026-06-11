package com.makardr.wallpapercrop.common.utils

import android.content.Context

fun Context.isTablet(): Boolean {
    return resources.configuration.smallestScreenWidthDp >= 600
}