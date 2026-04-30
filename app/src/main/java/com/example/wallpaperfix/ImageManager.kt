package com.example.wallpaperfix

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageManager(
    //TODO: image preview gotten from context without crop is not representative to how this image will actually apply on wallpaper set
    private val context: Context,
    private val imagePreview: ImageView,
    private val scope: CoroutineScope
) {
    private var imageUri: Uri? = null

    fun updateUri(uri: Uri?) {
        imageUri = uri
        refreshPreviewImage()
    }

    fun getUri(): Uri? {
        return imageUri
    }

    fun refreshPreviewImage() {
        imagePreview.setImageURI(null)
        imagePreview.setImageURI(imageUri)
    }

    fun setWallpaper(which: Int) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    imageUri?.let {
                        val wallpaperManager = WallpaperManager.getInstance(context)

                        context.contentResolver.openInputStream(it)?.use { stream ->
                            wallpaperManager.setStream(stream, null, true, which)
                        }
                        Log.d(Tags.SETWALLPAPER, "Wallpaper applied")
                    }
                }
            } catch (e: IOException) {
                Log.e(Tags.SETWALLPAPER, e.toString())
            }

        }
    }
}