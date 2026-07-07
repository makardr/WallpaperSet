package com.makardr.wallpapercrop.activities.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.WallpaperFlag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.activities.main.components.GalleryAdapter
import com.makardr.wallpapercrop.activities.main.viewmodels.GalleryAdapterViewModel
import com.makardr.wallpapercrop.activities.main.viewmodels.ImageManagerViewModel
import com.makardr.wallpapercrop.activities.settings.SettingsActivity
import com.makardr.wallpapercrop.activities.uCrop.UCropActivity
import com.makardr.wallpapercrop.common.utils.available
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.ImageRepository
import com.makardr.wallpapercrop.data.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity() {

    private val imageManager: ImageManagerViewModel by viewModels()
    private val galleryAdapterViewModel: GalleryAdapterViewModel by viewModels()

    private lateinit var uCropActivity: UCropActivity
    private lateinit var imageRepository: ImageRepository
    private lateinit var preferencesRepository: PreferencesRepository

    //Interface elements
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaperSystem: Button
    private lateinit var setWallpaperLock: Button
    private lateinit var setWallpaperAll: Button
    private lateinit var setWallpaper: MaterialButton
    private lateinit var cropImageButton: ImageButton
    private lateinit var openFileExplorer: ImageButton
    private lateinit var openGalleryButton: ImageButton
    private lateinit var openPreferencesButton: ImageButton
    private lateinit var tooltip: TextView
    private lateinit var dialog: Dialog
    private lateinit var setWallpaperLayout: View

    private lateinit var galleryDialog: BottomSheetDialog
    private lateinit var galleryLayout: View
    private lateinit var galleryRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var galleryDeleteButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.logInfo(Tags.Lifecycle, "onCreate")
        preferencesRepository = PreferencesRepository.getInstance(this)
        imageRepository = ImageRepository.getInstance(this)
        setupInterface()
        uCropActivity = UCropActivity(this, imageManager)
        collectEvents()

        if (savedInstanceState != null) {
            if (imageManager.getImageUri() != null) {
                refreshPreviewImage(imageManager.getImageUri()!!)
                enableInterface()
            }
        } else {
            handleIncomingIntent(intent)
        }


        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)

    }

    private fun collectEvents() {
        lifecycleScope.launch {
            Logger.logDebug(Tags.Lifecycle, "Starting event listening")
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                imageManager.refreshImageEventChannel.collect {
                    if (imageManager.getImageUri() != null) {
                        refreshPreviewImage(imageManager.getImageUri()!!)
                    } else {
                        Logger.logInfo(Tags.Uri, "Image refresh failed, uri is null")
                    }
                    Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.logDebug(Tags.Lifecycle, "onDestroy")
        dialog.dismiss()
        galleryDialog.dismiss()
        if (isFinishing) {
            pickMediaLauncher.unregister()
        }
    }

    override fun onStart() {
        super.onStart()
        Logger.logDebug(Tags.Lifecycle, "onStart")
        // Activity becomes visible (not yet interactive)
    }

    override fun onResume() {
        super.onResume()
        Logger.logDebug(Tags.Lifecycle, "onResume")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        galleryAdapterViewModel.refreshGallery()
        // Activity is in foreground and interactive
        // Register listeners, start camera, resume animations
    }

    override fun onPause() {
        super.onPause()
        Logger.logDebug(Tags.Lifecycle, "onPause")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Losing focus
        // Unregister sensors, pause animations
    }

    override fun onStop() {
        super.onStop()
        Logger.logDebug(Tags.Lifecycle, "onStop")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Activity fully hidden/backgrounded
        // Save data, release heavy resources
    }

    override fun onRestart() {
        super.onRestart()
        Logger.logDebug(Tags.Lifecycle, "onRestart")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Called after onStop() when user navigates back to activity
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Logger.logInfo(Tags.IncomingIntent, "Received image uri on newIntent: $intent")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        Logger.logInfo(Tags.IncomingIntent, "Handling incoming intent ${intent.action}")
        when (intent.action) {
            Intent.ACTION_SEND -> handleActionSend(intent)
            else -> Logger.logInfo(Tags.IncomingIntent, "Ignoring intent ${intent.action}")
        }
    }

    private fun handleActionSend(intent: Intent) {
        val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (sharedUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    sharedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Logger.logInfo(
                    Tags.IncomingIntent, "Success granting FLAG_GRANT_READ_URI_PERMISSION"
                )
            } catch (e: SecurityException) {
                Logger.logWarning(Tags.IncomingIntent, e.toString())
            }
            Logger.logInfo(Tags.IncomingIntent, sharedUri.toString())
            imageManager.updateOriginUri(sharedUri)
            Logger.logInfo(
                Tags.IncomingIntent, "handleImageGeneric set uri as $sharedUri"
            )
        } else {
            Logger.logError(Tags.IncomingIntent, "Shared image uri is null, ${intent.data}")
            throw NullPointerException("Received image uri is null")
        }
    }

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            imageManager.updateOriginUri(uri)
        }
    }


    @SuppressLint("InflateParams", "SourceLockedOrientationActivity")
    private fun setupInterface() {
        setContentView(R.layout.main_activity)
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()

        dialog = BottomSheetDialog(this)
        setWallpaperLayout = layoutInflater.inflate(R.layout.main_set_wallpaper_bottom_sheet, null)

        dialog.setContentView(setWallpaperLayout)

        galleryDialog = BottomSheetDialog(this)
        galleryLayout = layoutInflater.inflate(R.layout.gallery_bottom_sheet, null)
        galleryDialog.setContentView(galleryLayout)

        val emptyGalleryText: View = galleryLayout.findViewById(R.id.emptyGalleryText)
        galleryAdapterViewModel.galleryImages.observe(this) { uriList ->
            galleryAdapter.images = uriList
            emptyGalleryText.visibility = if (uriList.isEmpty()) View.VISIBLE else View.GONE
        }
        galleryRecyclerView = galleryLayout.findViewById(R.id.galleryRecyclerView)


        galleryDialog.setOnShowListener {
            val bottomSheet =
                galleryDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val displayMetrics = resources.displayMetrics
                if (isTablet()) {
                    it.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    (it.parent as? View)?.layoutParams?.let { parentParams ->
                        parentParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        parentParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    behavior.maxWidth = displayMetrics.widthPixels
                    behavior.peekHeight = displayMetrics.heightPixels
                    galleryDialog.window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    it.requestLayout()
                } else {
                    it.layoutParams.height = (displayMetrics.heightPixels * 0.75).toInt()
                    it.requestLayout()
                }
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        galleryAdapter = GalleryAdapter(
            { uri ->
                if (galleryAdapterViewModel.selectedImages.value.orEmpty().isNotEmpty()) {
                    galleryAdapterViewModel.toggleSelection(uri)
                } else {
                    imageManager.updateOriginUri(uri)
                    imageManager.disableImageSave()
                    galleryDialog.dismiss()
                }
            },
            { uri ->
                galleryAdapterViewModel.toggleSelection(uri)
                Logger.logInfo(Tags.UserInteraction, "Selected image: $uri")
            })



        galleryAdapterViewModel.selectedImages.observe(this) { selectedSet ->
            galleryAdapter.selectedUris = selectedSet
            galleryDeleteButton.visibility =
                if (selectedSet.isNotEmpty()) View.VISIBLE else View.GONE
        }


        galleryDialog.setOnDismissListener {
            galleryAdapterViewModel.clearSelectedImagesList()
        }

        galleryDeleteButton = galleryLayout.findViewById(R.id.deleteButton)
        galleryDeleteButton.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "Delete button pressed")
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        if (galleryAdapterViewModel.deleteSelectedImages()) {
                            sendToast(getString(R.string.toast_delete_success))
                        } else {
                            sendToast(getString(R.string.toast_delete_failure))
                        }
                    }
                }
                .show()
        }

        galleryRecyclerView.adapter = galleryAdapter

        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        tooltip = findViewById(R.id.tooltip)

        setWallpaperSystem = setWallpaperLayout.findViewById(R.id.optionHome)
        setWallpaperLock = setWallpaperLayout.findViewById(R.id.optionLock)
        setWallpaperAll = setWallpaperLayout.findViewById(R.id.optionBoth)

        setWallpaper = findViewById(R.id.setWallpaperButton)

        cropImageButton = findViewById(R.id.cropImage)
        openFileExplorer = findViewById(R.id.openExplorer)
        openGalleryButton = findViewById(R.id.openGallery)
        openPreferencesButton = findViewById(R.id.preferencesButton)

        setWallpaperSystem.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "setWallpaperSystem button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM)
        }

        setWallpaperLock.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "setWallpaperLock button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_LOCK)
        }

        setWallpaperAll.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "setWallpaperAll button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }

        setWallpaper.setOnClickListener {
            dialog.show()
            if (isTablet()) {
                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        cropImageButton.setOnClickListener {
            imageManager.getOriginUri()?.let {
                uCropActivity.launchUCropActivity(it)
            }
        }

        openFileExplorer.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        openGalleryButton.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "Open Album button pressed")
            galleryDialog.show()
        }

        openPreferencesButton.setOnClickListener {
            Logger.logInfo(Tags.UserInteraction, "Open Settings button pressed")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(setWallpaper) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }

        listOf(
            cropImageButton,
            openFileExplorer,
            setWallpaper,
            openGalleryButton,
            openPreferencesButton
        ).forEach { button ->
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

        disableInterface()
        if (preferencesRepository.galleryEnabled) {
            openGalleryButton.visibility = View.VISIBLE
        } else {
            openGalleryButton.visibility = View.INVISIBLE
        }
    }

    private fun enableInterface() {
        Logger.logInfo(Tags.SetupInterface, "Interface enabled")
        setWallpaper.isEnabled = true
        tooltip.visibility = View.INVISIBLE
    }

    private fun disableInterface() {
        Logger.logInfo(Tags.SetupInterface, "Interface disabled")
        setWallpaper.isEnabled = false
        tooltip.visibility = View.VISIBLE
    }

    private fun refreshPreviewImage(uri: Uri) {
        if (!uri.available(this@MainActivity)) {
            Logger.logError(Tags.Uri, "File does not exist, resetting uri: $uri")
            imageManager.triggerFailState()
            disableInterface()
        } else {
            Logger.logInfo(Tags.Uri, "Refreshing preview image: $uri")
            wallpaperPreview.load(uri) {
                crossfade(true)
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
            enableInterface()
        }
    }

    private fun setOnClickWallpaper(@WallpaperFlag flag: Int) {
        imageManager.setWallpaper(flag)
        dialog.hide()
        lifecycleScope.launch {
            Logger.logInfo(Tags.SetWallpaper, "Exit delay started")
            sendToast(getString(R.string.toast_wallpaper_applied))
            delay(1000.milliseconds)
            Logger.logInfo(Tags.SetWallpaper, "Exit delay finished, exiting to main screen")
            exitToTheMainScreen()
        }
    }

    private fun exitToTheMainScreen() {
        moveTaskToBack(true)
    }

    private fun sendToast(message: String) {
        Toast.makeText(
            this@MainActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}