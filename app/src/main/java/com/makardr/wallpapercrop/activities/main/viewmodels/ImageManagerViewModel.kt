package com.makardr.wallpapercrop.activities.main.viewmodels

import android.app.Application
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.WallpaperFlag
import com.makardr.wallpapercrop.data.ImageRepository
import com.makardr.wallpapercrop.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    private val screenHeight: Int = context.resources.displayMetrics.heightPixels
    private val _refreshChannel = Channel<Unit>(capacity = 2)
    val refreshImageEventChannel: Flow<Unit> = _refreshChannel.receiveAsFlow()
    private var imageOriginUri: Uri? = null
    private var imageIsCropped = false
    private var croppedImageUri: Uri = AppConstants.imageCacheOutputUri(context)
    private var imageRepository: ImageRepository = ImageRepository.getInstance(context)
    private var preferencesRepository: PreferencesRepository =
        PreferencesRepository.getInstance(context)
    private var saveWallpaperEnabled = true

    private fun notifyImageUpdated() {
        Logger.logDebug(
            LogTags.Uri,
            "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $imageOriginUri "
        )
        _refreshChannel.trySend(Unit)
    }

    fun getImageUri(): Uri? {
        return if (!imageIsCropped) {
            imageOriginUri
        } else {
            croppedImageUri
        }
    }

    //Used exclusively to crop only the original shared image, should not be used otherwise
    fun getOriginUri(): Uri? {
        return imageOriginUri
    }

    fun updateOriginUri(uri: Uri?) {
        imageOriginUri = uri
        imageIsCropped = false
        enableImageSave()
        Logger.logInfo(LogTags.Uri, "Uri updated: $imageOriginUri, imageIsCropped: $imageIsCropped")
        imageOriginUri?.let {
            notifyImageUpdated()
        }
    }

    fun updateIsCropped() {
        Logger.logInfo(LogTags.Uri, "imageIsCropped updated")
        imageIsCropped = true
        enableImageSave()
        notifyImageUpdated()
    }

    fun resetCrop() {
        Logger.logInfo(LogTags.Uri, "Reset image crop")
        imageIsCropped = false
        imageOriginUri?.let {
            notifyImageUpdated()
        }
    }

    fun triggerFailState() {
        imageIsCropped = false
        imageOriginUri = null
        notifyImageUpdated()
    }

    fun setWallpaper(@WallpaperFlag flag: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val (currentUri, cropHint) = if (imageIsCropped) {
                        croppedImageUri to calculateCropHint(croppedImageUri)
                    } else {
                        imageOriginUri to imageOriginUri?.let { calculateCropHint(it) }
                    }

                    currentUri?.let { uri ->
                        val wallpaperManager = WallpaperManager.getInstance(context)

                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            wallpaperManager.setStream(stream, cropHint, true, flag)
                        }
                        if (preferencesRepository.galleryEnabled && saveWallpaperEnabled) {
                            imageRepository.saveImage(uri)
                        }
                        Logger.logInfo(LogTags.SetWallpaper, "Wallpaper applied")
                    }
                }
            } catch (e: IOException) {
                Logger.logError(LogTags.SetWallpaper, e.toString())
            }
        }
    }

    private fun calculateCropHint(uri: Uri): Rect {
        Logger.logDebug(LogTags.DimensionCrop, "========================================")
        val (imageWidth, imageHeight) = getImageDimensions(uri)
        Logger.logDebug(
            LogTags.DimensionCrop,
            "screenWidth $screenWidth, screenHeight $screenHeight, imageWidth $imageWidth, imageHeight $imageHeight"
        )

        val scale = maxOf(
            screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight
        )
        Logger.logDebug(LogTags.DimensionCrop, "scale $scale")

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        Logger.logDebug(LogTags.DimensionCrop, "scaledWidth $scaledWidth, scaledHeight $scaledHeight")

        val offsetX = (scaledWidth - screenWidth) / 2f
        val offsetY = (scaledHeight - screenHeight) / 2f

        Logger.logDebug(LogTags.DimensionCrop, "offsetX $offsetX, offsetY $offsetY")


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

    fun enableImageSave() {
        saveWallpaperEnabled = true
    }

    fun disableImageSave() {
        saveWallpaperEnabled = false
    }
}