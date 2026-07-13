package com.makardr.wallpapercrop.activities.main.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.data.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryAdapterViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private var imageRepository = ImageRepository.getInstance(context)

    //Adapter lifecycle
    private val _galleryImages = MutableLiveData<List<Uri>>()
    val galleryImages: LiveData<List<Uri>> = _galleryImages

    private val _selectedImages = MutableLiveData<Set<Uri>>(emptySet())
    val selectedImages: LiveData<Set<Uri>> = _selectedImages

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
        _selectedImages.value = emptySet()
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedImages.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(uri)) {
            current.remove(uri)
            Logger.logInfo(LogTags.UserInteraction, "Removed selected image: $uri")
        } else {
            current.add(uri)
            Logger.logInfo(LogTags.UserInteraction, "Added selected image: $uri")
        }
        _selectedImages.value = current
    }

    suspend fun deleteSelectedImages(): Boolean {
        val imagesToDelete = _selectedImages.value ?: return true
        if (imagesToDelete.isEmpty()) return true

        return withContext(Dispatchers.IO) {
            val result = imageRepository.deleteImages(imagesToDelete)
            withContext(Dispatchers.Main) {
                clearSelectedImagesList()
                refreshGallery()
            }
            result
        }
    }
}