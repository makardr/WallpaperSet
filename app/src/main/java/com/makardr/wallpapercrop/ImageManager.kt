package com.makardr.wallpapercrop

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.makardr.wallpapercrop.common.AppConstants
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.utils.Logger
import com.makardr.wallpapercrop.utils.WallpaperFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class ImageManager(
    private val context: Context,
    private val imagePreview: ImageView,
    private val scope: CoroutineScope,
    private val setWallpaper: Button,
    private val tooltip: TextView
) {
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri
    private var imageIsCropped: Boolean = false
    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    private val screenHeight: Int = context.resources.displayMetrics.heightPixels

    init {
        Logger.logDebug(
            Tags.Lifecycle,
            "ImageManager init, ${imageUri ?: "uri = null"}, imageIsCropped $imageIsCropped"
        )
        if (imageUri != null) {
            enableInterface()
        } else {
            disableInterface()
        }
        croppedImageUri = AppConstants.imageCacheOutputUri(context)
    }

    fun updateOriginUri(uri: Uri?) {
        Logger.logInfo(Tags.Uri, "Uri updated: $uri")
        imageUri = uri
        imageIsCropped = false

        imageUri?.let {
            refreshPreviewImage(it)
            enableInterface()
        }
    }

    fun updateIsCropped() {
        Logger.logInfo(Tags.Uri, "imageIsCropped updated")
        imageIsCropped = true
        refreshPreviewImage(croppedImageUri)

        enableInterface()
    }

    fun resetCrop() {
        Logger.logInfo(Tags.Uri, "Reset image crop")
        imageIsCropped = false
        imageUri?.let { refreshPreviewImage(it) }
    }

    fun enableInterface() {
        Logger.logInfo(Tags.SetupInterface, "Interface enabled")
        setWallpaper.isEnabled = true
        tooltip.visibility = View.INVISIBLE
    }

    fun disableInterface() {
        Logger.logInfo(Tags.SetupInterface, "Interface disabled")
        setWallpaper.isEnabled = false
        tooltip.visibility = View.VISIBLE
    }

    fun getOriginUri(): Uri? {
        return imageUri
    }

    fun imageIsCropped(): Boolean {
        return imageIsCropped
    }

    private fun refreshPreviewImage(uri: Uri) {
        scope.launch {
            if (!uri.available(context)) {
                Logger.logError(Tags.Uri, "File does not exist, resetting uri: $uri")
                disableInterface()
                imageIsCropped = false
                imageUri = null
            } else {
                imagePreview.load(uri) {
                    crossfade(true)
                }
            }
        }
    }

    private fun Uri.available(context: Context): Boolean {
        return when (scheme) {
            "file" -> {
                val file = path?.let { File(it) }
                when {
                    file == null -> {
                        Logger.logError(Tags.Uri, "File URI has null path: $this")
                        false
                    }
                    !file.exists() -> {
                        Logger.logError(Tags.Uri, "File does not exist: ${file.absolutePath}")
                        false
                    }
                    !file.canRead() -> {
                        Logger.logError(Tags.Uri, "File exists but is not readable (permissions?): ${file.absolutePath}")
                        false
                    }
                    file.length() == 0L -> {
                        Logger.logError(Tags.Uri, "File exists and is readable but is empty: ${file.absolutePath}")
                        false
                    }
                    else -> true
                }
            }

            "content" -> {
                try {
                    context.contentResolver.openInputStream(this)?.use {
                        val hasBytes = it.read() != -1
                        if (!hasBytes) Logger.logError(Tags.Uri, "Content URI opened successfully but stream is empty: $this")
                        hasBytes
                    } ?: run {
                        Logger.logError(Tags.Uri, "ContentResolver.openInputStream returned null for: $this")
                        false
                    }
                } catch (e: SecurityException) {
                    Logger.logError(Tags.Uri, "Permission denied for URI: $this — grant may have expired or was never acquired. ${e.message}")
                    false
                } catch (e: FileNotFoundException) {
                    Logger.logError(Tags.Uri, "File not found via content provider: $this — provider registered but file missing. ${e.message}")
                    false
                } catch (e: Exception) {
                    Logger.logError(Tags.Uri, "Unexpected error reading URI: $this — ${e.javaClass.simpleName}: ${e.message}")
                    false
                }
            }

            null -> {
                Logger.logError(Tags.Uri, "URI has null scheme: $this")
                false
            }
            else -> {
                Logger.logError(Tags.Uri, "Unsupported URI scheme '${scheme}': $this")
                false
            }
        }
    }

    fun setWallpaper(@WallpaperFlag flag: Int) {
        //TODO: Write a toast "Wallpaper applied!"
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val (currentUri, cropHint) = if (imageIsCropped) {
                        croppedImageUri to calculateCropHint(croppedImageUri)
                    } else {
                        imageUri to imageUri?.let { calculateCropHint(it) }
                    }

                    currentUri?.let {
                        val wallpaperManager = WallpaperManager.getInstance(context)

                        context.contentResolver.openInputStream(it)?.use { stream ->
                            wallpaperManager.setStream(stream, cropHint, true, flag)
                        }
                        Logger.logInfo(Tags.SetWallpaper, "Wallpaper applied")
                    }
                }
            } catch (e: IOException) {
                Logger.logError(Tags.SetWallpaper, e.toString())
            }
        }
    }

    private fun calculateCropHint(uri: Uri): Rect {
        Logger.logDebug(Tags.DimensionCrop, "========================================")
        val (imageWidth, imageHeight) = getImageDimensions(uri)
        Logger.logDebug(
            Tags.DimensionCrop,
            "screenWidth $screenWidth, screenHeight $screenHeight, imageWidth $imageWidth, imageHeight $imageHeight"
        )

        val scale = maxOf(
            screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight
        )
        Logger.logDebug(Tags.DimensionCrop, "scale $scale")

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        Logger.logDebug(Tags.DimensionCrop, "scaledWidth $scaledWidth, scaledHeight $scaledHeight")

        val offsetX = (scaledWidth - screenWidth) / 2f
        val offsetY = (scaledHeight - screenHeight) / 2f

        Logger.logDebug(Tags.DimensionCrop, "offsetX $offsetX, offsetY $offsetY")


        val left = (offsetX / scale).toInt().coerceIn(0, imageWidth)
        val top = (offsetY / scale).toInt().coerceIn(0, imageHeight)
        val right = ((offsetX + screenWidth) / scale).toInt().coerceIn(left, imageWidth)
        val bottom = ((offsetY + screenHeight) / scale).toInt().coerceIn(top, imageHeight)

        return Rect(left, top, right, bottom)
    }

    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return Pair(options.outWidth, options.outHeight)
    }
}