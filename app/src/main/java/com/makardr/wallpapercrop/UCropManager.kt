package com.makardr.wallpapercrop

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.utils.Logger
import com.yalantis.ucrop.UCrop

class UCropManager(
    caller: ActivityResultCaller,
    private val imageManager: ImageManager
) {
    private val context = caller as Context

    fun launchUCropActivity(uri: Uri) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.PNG)
        }

        UCrop.of(uri, AppConstants.imageCacheOutputUri(context))
            .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
            .withOptions(options)
            .start(context, cropResultLauncher)
    }

    private val cropResultLauncher = caller.registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                //val croppedUri = UCrop.getOutput(result.data!!)
                imageManager.updateIsCropped()
                Logger.logInfo(
                    Tags.CropResult,
                    "Crop result set imageUri as ${imageManager.getOriginUri()}"
                )
            }

            RESULT_CANCELED -> {
                Logger.logInfo(Tags.CropResult, "User cancelled crop")
                imageManager.resetCrop()
            }

            UCrop.RESULT_ERROR -> {
                val error = UCrop.getError(result.data!!)
                Logger.logError(Tags.CropResult, error.toString())
            }
        }
    }
}