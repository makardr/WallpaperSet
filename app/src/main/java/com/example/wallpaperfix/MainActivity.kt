package com.example.wallpaperfix

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : AppCompatActivity() {
    private val debugTag = "WallpaperFixDebug";
    private val wallpaperCacheFileName = "cropped_wallpaper_cache.jpg"
    private lateinit var imageCacheOutputUri: Uri
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaperSystem: Button
    private lateinit var setWallpaperLock: Button
    private lateinit var setWallpaperAll: Button

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupInterface()

        imageCacheOutputUri = Uri.fromFile(File(cacheDir, wallpaperCacheFileName))

        if (savedInstanceState == null) {
            Log.d(debugTag, "Handling incoming intent from fresh start")
            handleIncomingIntent(intent)
        }

        imageUri?.let {
            launchUCropActivity(it)
        }

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
                Log.d(tag, imageUri.toString())
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
        val tag = "handleImageGeneric";
        val sharedUri: Uri? = intent.data
        Log.d(tag, sharedUri.toString())
        6
        imageUri = sharedUri
        imageUri?.let {
            launchUCropActivity(it)
        }
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

    private fun setupInterface() {
        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        setWallpaperSystem = findViewById(R.id.setWallpaperSystem)
        setWallpaperLock = findViewById(R.id.setWallpaperLock)
        setWallpaperAll = findViewById(R.id.setWallpaperAll)

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
    }
}