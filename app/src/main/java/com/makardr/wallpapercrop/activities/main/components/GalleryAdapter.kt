package com.makardr.wallpapercrop.activities.main.components

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.makardr.wallpapercrop.R

class GalleryAdapter(
    private val onImageTap: (Uri) -> Unit,
    private val onImageHold: (Uri) -> Unit
) : ListAdapter<GalleryAdapter.GalleryItem, GalleryAdapter.ViewHolder>(DIFF_CALLBACK) {
    data class GalleryItem(val uri: Uri, val isSelected: Boolean)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wallpaperThumbnail: ImageView = view.findViewById(R.id.wallpaperThumbnail)
        val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        val checkIcon: ImageView = view.findViewById(R.id.checkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_wallpaper, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = images[position]
        val isSelected = selectedUris.contains(uri)

        holder.wallpaperThumbnail.load(uri) {
            crossfade(true)
            placeholder(R.drawable.bg_placeholder)
        }

        // Selection state UI
        holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Animation
        val scale = if (isSelected) 0.9f else 1.0f
        holder.wallpaperThumbnail.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(200)
            .start()

        holder.itemView.setOnClickListener {
            onImageTap(uri)
        }

        holder.itemView.setOnLongClickListener {
            onImageHold(uri)
            true
        }
    }

    override fun getItemCount(): Int = images.size

    var images: List<Uri> = emptyList()
        set(value) {
            field = value
            submitCombinedList()
        }

    var selectedUris: Set<Uri> = emptySet()
        set(value) {
            field = value
            submitCombinedList()
        }

    private fun submitCombinedList() {
        submitList(images.map { GalleryItem(it, selectedUris.contains(it)) })
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GalleryItem>() {
            override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean =
                oldItem == newItem
        }
    }
}