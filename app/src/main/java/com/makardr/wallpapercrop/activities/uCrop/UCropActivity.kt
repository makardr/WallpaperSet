package com.makardr.wallpapercrop.activities.uCrop

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import com.makardr.wallpapercrop.activities.main.ImageManagerViewModel
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.isTablet
import com.yalantis.ucrop.UCrop

class UCropActivity(
    caller: ActivityResultCaller,
    private val imageManager: ImageManagerViewModel
) {
    private val context = caller as Context

    fun launchUCropActivity(uri: Uri) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight: Int = if (!context.isTablet()){
            context.resources.displayMetrics.heightPixels
        } else {
            context.resources.displayMetrics.widthPixels
        }

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.PNG)
        }

        val intent = UCrop.of(uri, AppConstants.imageCacheOutputUri(context))
            .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
            .withOptions(options)
            .getIntent(context)
            .apply {
                setClass(context, PortraitUCropActivity::class.java)
            }

        cropResultLauncher.launch(intent)
    }

    private val cropResultLauncher = caller.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                //val croppedUri = UCrop.getOutput(result.data!!)
                imageManager.updateIsCropped()
                Logger.logInfo(
                    Tags.CropResult,
                    "Crop result set imageUri as ${imageManager.getOriginUri()}"
                )
            }

            Activity.RESULT_CANCELED -> {
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