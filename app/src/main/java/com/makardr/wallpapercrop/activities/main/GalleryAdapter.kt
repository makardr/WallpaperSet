package com.makardr.wallpapercrop.activities.main

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.data.ImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryAdapter(
    private val imageManager: ImageManagerViewModel,
    private val imageRepository: ImageRepository,
    private val onImageSelected: () -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var images: List<Uri> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        adapterScope.launch {
            val loadedImages = withContext(Dispatchers.IO) {
                imageRepository.listSavedImageUris()
            }
            images = loadedImages
            notifyDataSetChanged()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wallpaperThumbnail: ImageView = view.findViewById(R.id.wallpaperThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = images[position]
        holder.wallpaperThumbnail.load(uri) {
            crossfade(true)
            placeholder(R.drawable.bg_placeholder)
        }
        holder.itemView.setOnClickListener {
            imageManager.updateOriginUri(uri)
            onImageSelected()
        }
    }

    override fun getItemCount(): Int = images.size
}
