package com.makardr.wallpapercrop.activities.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.data.ImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GalleryAdapter(
    //TODO: Why does item count is being passed in here? Amount of images should be determined from the repository inside GalleryAdapter
    //Try to remove it
    private val itemCount: Int,
    //TODO: update image using .updateOriginUri in the MainActivity once the user has selected it
    private val imageManager: ImageManagerViewModel,
    //TODO: get list of saved images to display with Coil
    private val imageRepository: ImageRepository
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        refresh()
    }


    //TODO: Call it when the screen should reappear
    fun refresh(){

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Just showing the placeholder for now
    }

    override fun getItemCount(): Int = itemCount
}