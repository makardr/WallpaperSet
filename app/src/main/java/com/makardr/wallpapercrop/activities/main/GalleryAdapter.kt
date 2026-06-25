package com.makardr.wallpapercrop.activities.main

import android.app.AlertDialog
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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
        holder.itemView.setOnLongClickListener {
//            AlertDialog.Builder(holder.itemView.context)
//                .setTitle("Delete image")
//                .setMessage("Remove this image from your gallery?")
//                .setPositiveButton("Delete") { _, _ -> deleteFile(uri) }
//                .setNegativeButton("Cancel", null)
//                .show()
//            true
            val popup = PopupMenu(holder.itemView.context, holder.itemView)
            popup.menu.add("Delete")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Delete" -> onImageDelete(uri)
                }
                true
            }
            popup.show()
            true
        }
    }

    private fun onImageDelete(uri: Uri){
        imageRepository.deleteImage(listOf(uri))
        refresh()
    }

    override fun getItemCount(): Int = images.size
}
