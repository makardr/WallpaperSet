package com.makardr.wallpapercrop.activities.main.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.data.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryAdapterViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private var selectedImages: MutableSet<Uri> = mutableSetOf()
    private var imageRepository = ImageRepository.getInstance(context)

    //Adapter lifecycle
    private val _galleryImages = MutableLiveData<List<Uri>>()
    val galleryImages: LiveData<List<Uri>> = _galleryImages

    //Refresh contained images list inside adapter gallery
    fun refreshGallery() {
        viewModelScope.launch {
            _galleryImages.value = withContext(Dispatchers.IO) {
                imageRepository.listSavedImageUris()
            }
        }
    }

    //Clear selected images list inside this
    fun clearSelectedImagesList() {
        selectedImages.clear()
    }

    fun addSelectedImage(uri: Uri) {
        if (!selectedImages.add(uri)) {
            Logger.logInfo(Tags.GalleryAdapterViewModel, "Duplicate image skipped: $uri")
        } else {
            Logger.logInfo(Tags.GalleryAdapterViewModel, "Successfully added selected image: $uri")
        }
    }

    suspend fun deleteSelectedImages() : Boolean {
        return withContext(Dispatchers.IO) {
            val result = imageRepository.deleteImages(selectedImages)
            clearSelectedImagesList()
            refreshGallery()
            result
        }
    }


}