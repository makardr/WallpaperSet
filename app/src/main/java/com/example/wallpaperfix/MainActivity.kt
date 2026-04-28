package com.example.wallpaperfix

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : AppCompatActivity() {
    private val debugTag = "WallpaperFixDebug"
    private val imageUriDebug = "imageUriDebug"
    private val wallpaperCacheFileName = "cropped_wallpaper_cache.jpg"
    private lateinit var imageCacheOutputUri: Uri
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaperSystem: Button
    private lateinit var setWallpaperLock: Button
    private lateinit var setWallpaperAll: Button

    private lateinit var cropImageButton: Button

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Log.d(debugTag, "App created")
        setupInterface(savedInstanceState)

        imageCacheOutputUri = Uri.fromFile(File(cacheDir, wallpaperCacheFileName))

        if (savedInstanceState == null) {
            Log.d(debugTag, "Handling incoming intent from fresh start")
            handleIncomingIntent(intent)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("imageUri", imageUri)
    }

    private fun launchUCropActivity(uri: Uri) {

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
//
        val options = UCrop.Options().apply {
//            setToolbarTitle("Crop Wallpaper")
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(100)
//            setCropGridRowCount(3)
//            setCropGridColumnCount(3)
//            setShowCropGrid(true)
//            setFreeStyleCropEnabled(false)
        }

        UCrop.of(uri, imageCacheOutputUri)
            .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
            .withMaxResultSize(screenWidth, screenHeight)
            .withOptions(options)
            .start(this, cropResultLauncher)
    }

    private val cropResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        var tag = "cropResultLauncher"
        when (result.resultCode) {
            RESULT_OK -> {
                val croppedUri = UCrop.getOutput(result.data!!)
                imageUri = croppedUri
                wallpaperPreview.setImageURI(null)
                wallpaperPreview.setImageURI(imageUri)
                Log.d(imageUriDebug, "cropResultLauncher set imageUri as $imageUri")
//                croppedUri?.let { setWallpaper(it, WallpaperManager.FLAG_SYSTEM) }
            }

            RESULT_CANCELED -> {
                Log.d(tag, "User cancelled crop")
            }

            UCrop.RESULT_ERROR -> {
                val error = UCrop.getError(result.data!!)
                Log.e(tag, "Crop error: $error")
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        val tag = "onNewIntent"
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(tag, "Handling incoming intent, app already opened")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        val tag = "handleIncomingIntent"
        Log.i(tag, "handleIncomingIntent")
        when (intent.action) {
            Intent.ACTION_VIEW -> handleImageGeneric(intent)
            Intent.ACTION_SEND -> handleImageGeneric(intent)
            else -> Log.d(tag, "Ignoring intent ${intent.action}")
        }
    }

    private fun handleImageGeneric(intent: Intent) {
        val tag = "handleImageGeneric"
        val sharedUri: Uri? = intent.data
        Log.d(tag, sharedUri.toString())
        imageUri = sharedUri
        wallpaperPreview.setImageURI(imageUri)
        Log.d(imageUriDebug, "handleImageGeneric set uri as $imageUri")
    }


//    private fun handleViewImage(intent: Intent) {
//        Log.d(debugTag, "handleViewImage")
//        val uri: Uri? = intent.data
//        Log.d(debugTag, uri.toString())
//    }
//
//    private fun handleSendImage(intent: Intent) {
//        Log.d(debugTag, "handleSendImage")
//        val uri: Uri? = intent.data
//        Log.d(debugTag, uri.toString())
//    }


    private fun setWallpaper(uri: Uri, which: Int) {
        val tag = "setWallpaper"
        val wallpaperManager = WallpaperManager.getInstance(this)

        contentResolver.openInputStream(uri)?.use { stream ->
            wallpaperManager.setStream(stream, null, true, which)
        }
        Log.d(tag, "Wallpaper applied")
    }

    private fun setupInterface(savedInstanceState: Bundle?) {
        val tag = "setupInterface"
        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        setWallpaperSystem = findViewById(R.id.setWallpaperSystem)
        setWallpaperLock = findViewById(R.id.setWallpaperLock)
        setWallpaperAll = findViewById(R.id.setWallpaperAll)
        cropImageButton = findViewById(R.id.cropImage)

        setWallpaperSystem.setOnClickListener {
            imageUri?.let {
                setWallpaper(it, WallpaperManager.FLAG_SYSTEM)
            }
        }

        setWallpaperLock.setOnClickListener {
            imageUri?.let {
                setWallpaper(it, WallpaperManager.FLAG_LOCK)
            }
        }

        setWallpaperAll.setOnClickListener {
            imageUri?.let {
                setWallpaper(it, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            }
        }

        cropImageButton.setOnClickListener {
            imageUri?.let {
                launchUCropActivity(it)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(setWallpaperSystem) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(setWallpaperLock) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(setWallpaperAll) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(cropImageButton) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            insets
        }




        if (savedInstanceState != null) {
            imageUri =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelable("imageUri", Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    savedInstanceState.getParcelable("imageUri")
                }
            imageUri?.let {
                wallpaperPreview.setImageURI(it)
            }
            Log.d(imageUriDebug, "setupInterface onCreate savedImageUri as $imageUri")
        }
    }
}