package com.makardr.wallpapercrop.activities.main.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class ImageManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val _refreshChannel = Channel<Unit>(capacity = 2)
    val refreshImageEventChannel: Flow<Unit> = _refreshChannel.receiveAsFlow()
    private var originImageUri: Uri? = null
    private var imageIsCropped = false
    private var croppedImageUri: Uri = AppConstants.imageCacheOutputUri(context)
    var saveWallpaperEnabled = true

    private fun notifyImageUpdated() {
        Logger.logDebug(
            LogTags.Uri,
            "Notify image updated: imageIsCropped: $imageIsCropped, imageUri: $originImageUri "
        )
        _refreshChannel.trySend(Unit)
    }

    fun getDisplayedImageUri(): Uri? {
        return if (!imageIsCropped) {
            originImageUri
        } else {
            croppedImageUri
        }
    }

    fun getOriginUri(): Uri? {
        return originImageUri
    }

    fun updateOriginUri(uri: Uri?) {
        originImageUri = uri
        imageIsCropped = false
        saveWallpaperEnabled = true
        Logger.logInfo(LogTags.Uri, "Uri updated: $originImageUri, imageIsCropped: $imageIsCropped")
        originImageUri?.let {
            notifyImageUpdated()
        }
    }

    fun setIsCropped() {
        Logger.logInfo(LogTags.Uri, "imageIsCropped updated")
        imageIsCropped = true
        saveWallpaperEnabled = true
        notifyImageUpdated()
    }

    fun resetImage() {
        Logger.logInfo(LogTags.Uri, "Reset image")
        saveWallpaperEnabled = true
        imageIsCropped = false
        originImageUri = null
        notifyImageUpdated()
    }
}