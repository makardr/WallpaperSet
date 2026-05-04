package com.example.wallpaperfix

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageManager(
    private val context: Context,
    private val imagePreview: ImageView,
    private val scope: CoroutineScope
) {
    private var imageUri: Uri? = null
    private var cropHint: Rect? = null

    fun updateUri(uri: Uri?) {
        imageUri = uri
        cropHint = uri?.let { calculateCropHint(it) }
        refreshPreviewImage()
    }

    fun getUri(): Uri? {
        return imageUri
    }

    fun refreshPreviewImage() {
        imageUri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)

            val cropped = Bitmap.createBitmap(
                bitmap,
                cropHint!!.left,
                cropHint!!.top,
                cropHint!!.width(),
                cropHint!!.height()
            )


            imagePreview.setImageURI(null)
            imagePreview.setImageBitmap(cropped)
        }
    }

    fun setWallpaper(which: Int) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    imageUri?.let {
                        val wallpaperManager = WallpaperManager.getInstance(context)

                        context.contentResolver.openInputStream(it)?.use { stream ->
                            wallpaperManager.setStream(stream, cropHint, true, which)
                        }
                        Log.d(Tags.SETWALLPAPER, "Wallpaper applied")
                    }
                }
            } catch (e: IOException) {
                Log.e(Tags.SETWALLPAPER, e.toString())
            }

        }
    }

    fun calculateCropHint(uri: Uri): Rect {
        Log.d(Tags.DIMENSIONSCROP, "========================================")
        val (screenWidth, screenHeight) = getScreenDimensions()
        val (imageWidth, imageHeight) = getImageDimensions(uri)
        Log.d(
            Tags.DIMENSIONSCROP,
            "screenWidth $screenWidth, screenHeight $screenHeight, imageWidth $imageWidth, imageHeight $imageHeight"
        )

        val scale = maxOf(
            screenWidth.toFloat() / imageWidth,
            screenHeight.toFloat() / imageHeight
        )
        Log.d(Tags.DIMENSIONSCROP, "scale $scale")

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        Log.d(Tags.DIMENSIONSCROP, "scaledWidth $scaledWidth, scaledHeight $scaledHeight")

        val offsetX = (scaledWidth - screenWidth) / 2f
        val offsetY = (scaledHeight - screenHeight) / 2f

        Log.d(Tags.DIMENSIONSCROP, "offsetX $offsetX, offsetY $offsetY")


        val left = (offsetX / scale).toInt().coerceIn(0, imageWidth)
        val top = (offsetY / scale).toInt().coerceIn(0, imageHeight)
        val right = ((offsetX + screenWidth) / scale).toInt().coerceIn(left, imageWidth)
        val bottom = ((offsetY + screenHeight) / scale).toInt().coerceIn(top, imageHeight)

        return Rect(left, top, right, bottom)
    }

    fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return Pair(options.outWidth, options.outHeight)
    }

    fun getScreenDimensions(): Pair<Int, Int> {
        return Pair(
            context.resources.displayMetrics.widthPixels,
            context.resources.displayMetrics.heightPixels
        )
    }
}