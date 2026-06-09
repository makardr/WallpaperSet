package com.makardr.wallpapercrop

import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.utils.Logger

class SaveStateManager(private val imageManager: ImageManager) {
    private val saveInstanceUri = "parcelableUri"
    private val savedInstanceIsCropped = "imageCropped"

    fun saveState(outState: Bundle) {
        Logger.logDebug(
            Tags.Uri,
            "Saving origin image uri ${imageManager.getOriginUri()} and is cropped ${imageManager.imageIsCropped()}"
        )
        outState.putParcelable(saveInstanceUri, imageManager.getOriginUri())
        outState.putBoolean(savedInstanceIsCropped, imageManager.imageIsCropped())
    }

    fun loadState(savedBundle: Bundle?) {
        if (savedBundle != null) {
            val savedImageUri =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedBundle.getParcelable(
                        saveInstanceUri,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    savedBundle.getParcelable(saveInstanceUri)
                }
            val isCropped =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedBundle.getBoolean(savedInstanceIsCropped, false)
                } else {
                    @Suppress("DEPRECATION")
                    savedBundle.getBoolean(savedInstanceIsCropped)
                }
            Logger.logDebug(
                Tags.Uri,
                "Restoring saved instance image uri $savedImageUri and is cropped $isCropped"
            )

            if (isCropped) {
                imageManager.updateOriginUri(savedImageUri)
                imageManager.updateIsCropped()
            } else {
                imageManager.updateOriginUri(savedImageUri)
            }

        } else {
            Logger.logDebug(Tags.Uri, "savedBundle is null")
            Logger.logInfo(Tags.IncomingIntent, "Handling incoming intent from fresh start")
        }
    }
}