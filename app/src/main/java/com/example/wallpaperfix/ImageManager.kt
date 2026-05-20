package com.example.wallpaperfix

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
import com.example.wallpaperfix.common.AppConstants
import com.example.wallpaperfix.common.Tags
import com.example.wallpaperfix.utils.Logger
import com.example.wallpaperfix.utils.WallpaperFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
        Logger.logInfo(
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
        Logger.logInfo(Tags.UriDebug, "Uri updated: ${imageUri ?: "uri = null"}")
        imageUri = uri
        imageIsCropped = false

        imageUri?.let {
            refreshPreviewImage(it)
            enableInterface()
        }
    }

    fun updateIsCropped() {
        Logger.logInfo(Tags.UriDebug, "imageIsCropped updated")
        imageIsCropped = true
        refreshPreviewImage(croppedImageUri)

        enableInterface()
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
            val exist = withContext(Dispatchers.IO) { waitForUri(uri) }
            if (!exist) {
                Logger.logError(Tags.UriDebug, "File does not exist, resetting uri: $uri")
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

    private suspend fun waitForUri(uri: Uri, retries: Int = 5, delayMs: Long = 1000): Boolean {
        var attempt = 1
        repeat(retries) {
            Logger.logInfo(Tags.UriDebug, "Waiting for uri attempt: $attempt")
            if (uri.exists(context)) return true
            delay(delayMs)
            attempt++
        }
        return false
    }

    private fun Uri.exists(context: Context): Boolean {
        return when (scheme) {
            "file" -> path?.let { File(it).exists() } ?: false
            "content" -> try {
                context.contentResolver.openInputStream(this)?.use { true } ?: false
            } catch (e: Exception) {
                false
            }

            else -> false
        }
    }

    fun setWallpaper(@WallpaperFlag flag: Int) {
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