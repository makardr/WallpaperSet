package com.makardr.wallpapercrop.activities.main

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.WallpaperFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri
    private var imageIsCropped: Boolean = false
    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    private val screenHeight: Int = context.resources.displayMetrics.heightPixels

    private val _refreshImage = MutableSharedFlow<Uri?>(replay = 1)
    val refreshImage: SharedFlow<Uri?> = _refreshImage


    init {
        Logger.logDebug(
            Tags.Lifecycle,
            "ImageManager init, ${imageUri ?: "uri = null"}, imageIsCropped $imageIsCropped"
        )
        croppedImageUri = AppConstants.imageCacheOutputUri(context)
    }

    fun notifyImageUpdated() {
        scope.launch {
            Logger.logDebug(Tags.Lifecycle, "Emitting refreshImage, subscribers: ${refreshImage.replayCache}")
            if (!imageIsCropped()) {
                Logger.logDebug(
                    Tags.Uri,
                    "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $imageUri "
                )
                _refreshImage.emit(imageUri)
            } else {
                Logger.logDebug(
                    Tags.Uri,
                    "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $croppedImageUri "
                )
                _refreshImage.emit(croppedImageUri)
            }

        }
    }
    fun updateOriginUri(uri: Uri?) {
        imageUri = uri
        imageIsCropped = false
        Logger.logInfo(Tags.Uri, "Uri updated: $imageUri, imageIsCropped: $imageIsCropped")
        imageUri?.let {
            notifyImageUpdated()
//            enableInterface()
        }
    }

    fun updateIsCropped() {
        Logger.logInfo(Tags.Uri, "imageIsCropped updated")
        imageIsCropped = true
        notifyImageUpdated()

//        enableInterface()
    }

    fun resetCrop() {
        Logger.logInfo(Tags.Uri, "Reset image crop")
        imageIsCropped = false
        imageUri?.let {
            notifyImageUpdated()
        }
    }

    fun getOriginUri(): Uri? {
        return imageUri
    }

    fun imageIsCropped(): Boolean {
        return imageIsCropped
    }

    fun triggerFailState() {
        imageIsCropped = false
        imageUri = null
        notifyImageUpdated()
    }

    fun setWallpaper(@WallpaperFlag flag: Int) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val (currentUri, cropHint) = if (imageIsCropped) {
                        croppedImageUri to calculateCropHint(croppedImageUri)
                    } else {
                        imageUri to imageUri?.let { calculateCropHint(it) }
                    }

                    currentUri?.let {
                        val wallpaperManager = WallpaperManager.getInstance(context)

                        context.contentResolver.openInputStream(it)?.use { stream ->
                            wallpaperManager.setStream(stream, cropHint, true, flag)
                        }
                        Logger.logInfo(Tags.SetWallpaper, "Wallpaper applied")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_notification),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: IOException) {
                Logger.logError(Tags.SetWallpaper, e.toString())
            }
        }
    }

    private fun calculateCropHint(uri: Uri): Rect {
        Logger.logDebug(Tags.DimensionCrop, "========================================")
        val (imageWidth, imageHeight) = getImageDimensions(uri)
        Logger.logDebug(
            Tags.DimensionCrop,
            "screenWidth $screenWidth, screenHeight $screenHeight, imageWidth $imageWidth, imageHeight $imageHeight"
        )

        val scale = maxOf(
            screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight
        )
        Logger.logDebug(Tags.DimensionCrop, "scale $scale")

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        Logger.logDebug(Tags.DimensionCrop, "scaledWidth $scaledWidth, scaledHeight $scaledHeight")

        val offsetX = (scaledWidth - screenWidth) / 2f
        val offsetY = (scaledHeight - screenHeight) / 2f

        Logger.logDebug(Tags.DimensionCrop, "offsetX $offsetX, offsetY $offsetY")


        val left = (offsetX / scale).toInt().coerceIn(0, imageWidth)
        val top = (offsetY / scale).toInt().coerceIn(0, imageHeight)
        val right = ((offsetX + screenWidth) / scale).toInt().coerceIn(left, imageWidth)
        val bottom = ((offsetY + screenHeight) / scale).toInt().coerceIn(top, imageHeight)

        return Rect(left, top, right, bottom)
    }

    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return Pair(options.outWidth, options.outHeight)
    }
}