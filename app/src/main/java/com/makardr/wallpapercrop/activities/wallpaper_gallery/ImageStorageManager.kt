package com.makardr.wallpapercrop.activities.wallpaper_gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel

class ImageStorageManager(application: Application) : AndroidViewModel(application) {

    fun getAllImages() {}

    fun getAllImagesPart() {}

    fun saveImage(uri: Uri) {

    }

    fun getImageUri(): Uri {
        throw Exception("Not implemented")
    }

    fun deleteImage(uri: Uri) {}

}