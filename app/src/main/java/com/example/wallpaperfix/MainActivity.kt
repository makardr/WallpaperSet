package com.example.wallpaperfix

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var imageManager: ImageManager
    private val wallpaperCacheFileName = "cropped_wallpaper_cache.jpg"
    private lateinit var imageCacheOutputUri: Uri
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaperSystem: Button
    private lateinit var setWallpaperLock: Button
    private lateinit var setWallpaperAll: Button
    private lateinit var setWallpaper: MaterialButton
    private lateinit var cropImageButton: ImageButton
    private lateinit var openFileExplorer: ImageButton
    private lateinit var tooltip: TextView
    private lateinit var dialog: Dialog
    private lateinit var setWallpaperLayout: View
    private var parcelableUri = "imageUri"
    private var parcelableCroppedUri = "croppedImageUri"

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            imageManager.updateUri(uri, false)
            imageManager.refreshPreviewImage()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInterface(savedInstanceState)
        imageCacheOutputUri = Uri.fromFile(File(cacheDir, wallpaperCacheFileName))
        if (savedInstanceState == null) {
            Logger.log(Tags.Generic, "Handling incoming intent from fresh start")
            handleIncomingIntent(intent)
        }
        Logger.log(Tags.Generic, "App created")
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //TODO: figure out what exactly am I saving here and why imageUri is hardcoded
        outState.putParcelable(parcelableUri, imageManager.getOriginUri())
        outState.putParcelable(parcelableCroppedUri, imageManager.getCroppedUri())
    }


    //TODO: Capturing intent does not work on physical phone
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Logger.log(Tags.NewIntent, "Handling incoming intent, app already opened")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        Logger.log(Tags.IncomingIntent, "handleIncomingIntent")
        when (intent.action) {
            Intent.ACTION_VIEW -> handleImageGeneric(intent)
            Intent.ACTION_SEND -> handleImageGeneric(intent)
            else -> Logger.log(Tags.IncomingIntent, "Ignoring intent ${intent.action}")
        }
    }

    private fun handleImageGeneric(intent: Intent) {
        val sharedUri: Uri? = intent.data
        Logger.log(Tags.HandleImageGeneric, sharedUri.toString())
        imageManager.updateUri(sharedUri, false)
        imageManager.refreshPreviewImage()
        Logger.log(Tags.UriDebug, "handleImageGeneric set uri as ${imageManager.getOriginUri()}")
    }

    private fun launchUCropActivity(uri: Uri) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        //TODO: Play with ucrop options, possibly reduce ucrop unnecessary functionality (image size enhancer scale)
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(100)
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
                imageManager.updateUri(croppedUri, true)
                imageManager.refreshPreviewImage()
                Logger.log(
                    Tags.UriDebug,
                    "cropResultLauncher set imageUri as ${imageManager.getOriginUri()}"
                )
            }

            RESULT_CANCELED -> {
                Logger.log(Tags.CropResult, "User cancelled crop")
            }

            UCrop.RESULT_ERROR -> {
                val error = UCrop.getError(result.data!!)
                Logger.log(Tags.CropResult, "Crop error: $error")
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun setupInterface(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()

        dialog = BottomSheetDialog(this)
        setWallpaperLayout = layoutInflater.inflate(R.layout.set_wallpaper_bottom_sheet, null)
        dialog.setContentView(setWallpaperLayout)

        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        tooltip = findViewById(R.id.tooltip)

        setWallpaperSystem = setWallpaperLayout.findViewById(R.id.optionHome)
        setWallpaperLock = setWallpaperLayout.findViewById(R.id.optionLock)
        setWallpaperAll = setWallpaperLayout.findViewById(R.id.optionBoth)

        setWallpaper = findViewById(R.id.setWallpaperButton)

        cropImageButton = findViewById(R.id.cropImage)
        openFileExplorer = findViewById(R.id.openExplorer)

        imageManager = ImageManager(this, wallpaperPreview, lifecycleScope, setWallpaper, tooltip)

        setWallpaperSystem.setOnClickListener {
            imageManager.setWallpaper(WallpaperManager.FLAG_SYSTEM)
        }

        setWallpaperLock.setOnClickListener {
            imageManager.setWallpaper(WallpaperManager.FLAG_LOCK)
        }

        setWallpaperAll.setOnClickListener {
            imageManager.setWallpaper(
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )
            Logger.log(Tags.Generic, "Set wallpaper clicked")
        }

        setWallpaper.setOnClickListener {
            //TODO: After wallpaper is applied dialog should hide itself
            dialog.setContentView(setWallpaperLayout)
            dialog.show()
        }

        cropImageButton.setOnClickListener {
            imageManager.getOriginUri()?.let {
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

        listOf(cropImageButton, openFileExplorer, setWallpaper).forEach { button ->
            val xmlMarginTopRecord = button.marginTop
            val xmlMarginBottomRecord = button.marginBottom
            ViewCompat.setOnApplyWindowInsetsListener(button) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = systemBars.top + xmlMarginTopRecord
                    bottomMargin = systemBars.bottom + xmlMarginBottomRecord
                }
                insets
            }
        }


        if (savedInstanceState != null) {
            val savedImageUri =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelable(parcelableUri, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    savedInstanceState.getParcelable(parcelableUri)
                }
            val croppedImageUri =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelable(parcelableCroppedUri, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    savedInstanceState.getParcelable(parcelableCroppedUri)
                }

            if (croppedImageUri == null) {
                imageManager.updateUri(savedImageUri, false)
                imageManager.refreshPreviewImage()
                Logger.log(
                    Tags.UriDebug,
                    "setupInterface onCreate savedImageUri as ${imageManager.getOriginUri()}"
                )
            } else {
                imageManager.updateUri(savedImageUri, false)
                imageManager.updateUri(croppedImageUri, true)
                imageManager.refreshPreviewImage()
                Logger.log(
                    Tags.UriDebug,
                    "Found croppedImageUri and restored image uris ${imageManager.getOriginUri()} and cropped image uri ${imageManager.getCroppedUri()}"
                )
            }
        }
    }
}