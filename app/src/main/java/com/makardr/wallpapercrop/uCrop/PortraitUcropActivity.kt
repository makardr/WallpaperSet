package com.makardr.wallpapercrop.uCrop

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.makardr.wallpapercrop.utils.isTablet
import com.yalantis.ucrop.UCropActivity

class PortraitUCropActivity : UCropActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
    }
}