package com.makardr.wallpapercrop.activities.main.components

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger

class GalleryAdapter(
    private val onImageTap: (Uri) -> Unit,
    private val onImageHold: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    var images: List<Uri> = emptyList()
        set(value) {
            field = value
            Logger.logInfo(Tags.Gallery, "Gallery image list updated")
            notifyDataSetChanged()
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
            onImageTap(uri)
        }

        holder.itemView.setOnLongClickListener {
            onImageHold(uri)
            true
        }
    }

    override fun getItemCount(): Int = images.size
}