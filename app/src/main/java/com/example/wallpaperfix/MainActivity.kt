package com.example.wallpaperfix

import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private val wallpaperCacheFileName = "cropped_wallpaper_cache.jpg"
    private lateinit var imageCacheOutputUri: Uri
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaperSystem: Button
    private lateinit var setWallpaperLock: Button
    private lateinit var setWallpaperAll: Button
    private lateinit var setWallpaper: Button
    private lateinit var cropImageButton: Button
    private lateinit var openFileExplorer: Button
    private lateinit var dialog: Dialog
    private lateinit var setWallpaperLayout: View


    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = uri
            wallpaperPreview.setImageURI(null)
            wallpaperPreview.setImageURI(it)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInterface(savedInstanceState)
        imageCacheOutputUri = Uri.fromFile(File(cacheDir, wallpaperCacheFileName))
        if (savedInstanceState == null) {
            Log.d(Tags.GENERIC, "Handling incoming intent from fresh start")
            handleIncomingIntent(intent)
        }
        Log.d(Tags.GENERIC, "App created")
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
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
        when (result.resultCode) {
            RESULT_OK -> {
                val croppedUri = UCrop.getOutput(result.data!!)
                imageUri = croppedUri
                wallpaperPreview.setImageURI(null)
                wallpaperPreview.setImageURI(imageUri)
                Log.d(Tags.URIDEBUG, "cropResultLauncher set imageUri as $imageUri")
            }

            RESULT_CANCELED -> {
                Log.d(Tags.CROPRESULT, "User cancelled crop")
            }

            UCrop.RESULT_ERROR -> {
                val error = UCrop.getError(result.data!!)
                Log.e(Tags.CROPRESULT, "Crop error: $error")
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(Tags.NEWINTENT, "Handling incoming intent, app already opened")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        Log.i(Tags.INCOMINGINTENT, "handleIncomingIntent")
        when (intent.action) {
            Intent.ACTION_VIEW -> handleImageGeneric(intent)
            Intent.ACTION_SEND -> handleImageGeneric(intent)
            else -> Log.d(Tags.INCOMINGINTENT, "Ignoring intent ${intent.action}")
        }
    }

    private fun handleImageGeneric(intent: Intent) {
        val sharedUri: Uri? = intent.data
        Log.d(Tags.HANDLEIMAGEGENERIC, sharedUri.toString())
        imageUri = sharedUri
        wallpaperPreview.setImageURI(imageUri)
        Log.d(Tags.URIDEBUG, "handleImageGeneric set uri as $imageUri")
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
        val wallpaperManager = WallpaperManager.getInstance(this)

        contentResolver.openInputStream(uri)?.use { stream ->
            wallpaperManager.setStream(stream, null, true, which)
        }
        Log.d(Tags.SETWALLPAPER, "Wallpaper applied")
    }

    private fun setupInterface(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()

        dialog = BottomSheetDialog(this)
        setWallpaperLayout = layoutInflater.inflate(R.layout.set_wallpaper_bottom_sheet, null)


        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        setWallpaperSystem = setWallpaperLayout.findViewById(R.id.optionHome)
        setWallpaperLock = setWallpaperLayout.findViewById(R.id.optionLock)
        setWallpaperAll = setWallpaperLayout.findViewById(R.id.optionBoth)

        setWallpaper = findViewById(R.id.setWallpaperButton)


        cropImageButton = findViewById(R.id.cropImage)
        openFileExplorer = findViewById(R.id.openExplorer)

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

        setWallpaper.setOnClickListener {
            dialog.setContentView(setWallpaperLayout)
            dialog.show()
        }

        cropImageButton.setOnClickListener {
            imageUri?.let {
                launchUCropActivity(it)
            }
        }

        openFileExplorer.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(setWallpaper) { v, insets ->
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
            Log.d(Tags.URIDEBUG, "setupInterface onCreate savedImageUri as $imageUri")
        }
    }
}