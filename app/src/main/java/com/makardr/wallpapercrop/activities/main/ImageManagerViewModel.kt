package com.makardr.wallpapercrop.activities.main

import android.app.Application
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.WallpaperFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()

    //TODO: Retire MainStateManager for LiveData
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri
    private var imageIsCropped: Boolean = false
    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    private val screenHeight: Int = context.resources.displayMetrics.heightPixels

    private val _refreshChannel = Channel<Uri?>(capacity = 2)
    val refreshImageEventChannel: Flow<Uri?> = _refreshChannel.receiveAsFlow()

    init {
        Logger.logDebug(
            Tags.Lifecycle,
            "ImageManager init, ${imageUri ?: "uri = null"}, imageIsCropped $imageIsCropped"
        )
        croppedImageUri = AppConstants.imageCacheOutputUri(context)
    }

    fun notifyImageUpdated() {
        if (!imageIsCropped()) {
            Logger.logDebug(
                Tags.Uri,
                "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $imageUri "
            )
            _refreshChannel.trySend(imageUri)
        } else {
            Logger.logDebug(
                Tags.Uri,
                "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $croppedImageUri "
            )
            _refreshChannel.trySend(croppedImageUri)
        }

    }

    fun updateOriginUri(uri: Uri?) {
        imageUri = uri
        imageIsCropped = false
        Logger.logInfo(Tags.Uri, "Uri updated: $imageUri, imageIsCropped: $imageIsCropped")
        imageUri?.let {
            notifyImageUpdated()
        }
    }

    fun updateIsCropped() {
        Logger.logInfo(Tags.Uri, "imageIsCropped updated")
        imageIsCropped = true
        notifyImageUpdated()
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
        viewModelScope.launch {
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