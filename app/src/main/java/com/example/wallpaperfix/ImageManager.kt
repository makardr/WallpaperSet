package com.example.wallpaperfix

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.wallpaperfix.common.AppConstants
import com.example.wallpaperfix.common.Tags
import com.example.wallpaperfix.utils.Logger
import com.example.wallpaperfix.utils.WallpaperFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        setWallpaper.isEnabled = imageUri != null
        croppedImageUri = AppConstants.imageCacheOutputUri(context)
    }

    fun updateOriginUri(uri: Uri?) {
        imageUri = uri
        imageIsCropped = false

        imageUri?.let { refreshPreviewImage(it) }

        setWallpaper.isEnabled = true
        tooltip.visibility = View.INVISIBLE
    }

    fun updateIsCropped() {
        imageIsCropped = true
        refreshPreviewImage(croppedImageUri)

        setWallpaper.isEnabled = true
        tooltip.visibility = View.INVISIBLE
    }

    fun getOriginUri(): Uri? {
        return imageUri
    }

    fun imageIsCropped(): Boolean {
        return imageIsCropped
    }

    private fun refreshPreviewImage(imageUri: Uri) {
        if (!imageUri.exists(context)) {
            throw NullPointerException("Uri $imageUri does not exist")
        }


        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
            }
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                imageUri
            )
        }

        imagePreview.setImageURI(null)
        imagePreview.setImageBitmap(bitmap)
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