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
import com.makardr.wallpapercrop.data.model.LogTags
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
import com.makardr.wallpapercrop.data.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity() {

    private val imageManager: ImageManagerViewModel by viewModels()
    private val galleryAdapterViewModel: GalleryAdapterViewModel by viewModels()

    private lateinit var uCropActivity: UCropActivity
    private lateinit var preferencesRepository: PreferencesRepository

    //Interface elements
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaper: MaterialButton
    private lateinit var tooltip: TextView
    private lateinit var applyWallpaperDialog: Dialog
    private lateinit var galleryDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.logInfo(LogTags.Lifecycle, "onCreate")
        preferencesRepository = PreferencesRepository.getInstance(this)
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
            Logger.logDebug(LogTags.Lifecycle, "Starting event listening")
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                imageManager.refreshImageEventChannel.collect {
                    if (imageManager.getImageUri() != null) {
                        refreshPreviewImage(imageManager.getImageUri()!!)
                    } else {
                        Logger.logInfo(LogTags.Uri, "Image refresh failed, uri is null")
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
        Logger.logDebug(LogTags.Lifecycle, "onDestroy")
        applyWallpaperDialog.dismiss()
        galleryDialog.dismiss()
        if (isFinishing) {
            pickMediaLauncher.unregister()
        }
    }

    override fun onStart() {
        super.onStart()
        Logger.logDebug(LogTags.Lifecycle, "onStart")
        // Activity becomes visible (not yet interactive)
    }

    override fun onResume() {
        super.onResume()
        Logger.logDebug(LogTags.Lifecycle, "onResume")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        galleryAdapterViewModel.refreshGallery()
        // Activity is in foreground and interactive
        // Register listeners, start camera, resume animations
    }

    override fun onPause() {
        super.onPause()
        Logger.logDebug(LogTags.Lifecycle, "onPause")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Losing focus
        // Unregister sensors, pause animations
    }

    override fun onStop() {
        super.onStop()
        Logger.logDebug(LogTags.Lifecycle, "onStop")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Activity fully hidden/backgrounded
        // Save data, release heavy resources
    }

    override fun onRestart() {
        super.onRestart()
        Logger.logDebug(LogTags.Lifecycle, "onRestart")
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
        // Called after onStop() when user navigates back to activity
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Logger.logInfo(LogTags.IncomingIntent, "Received image uri on newIntent: $intent")
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        Logger.logInfo(LogTags.IncomingIntent, "Handling incoming intent ${intent.action}")
        when (intent.action) {
            Intent.ACTION_SEND -> handleActionSend(intent)
            else -> Logger.logInfo(LogTags.IncomingIntent, "Ignoring intent ${intent.action}")
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
                    LogTags.IncomingIntent, "Success granting FLAG_GRANT_READ_URI_PERMISSION"
                )
            } catch (e: SecurityException) {
                Logger.logWarning(LogTags.IncomingIntent, e.toString())
            }
            Logger.logInfo(LogTags.IncomingIntent, sharedUri.toString())
            imageManager.updateOriginUri(sharedUri)
            Logger.logInfo(
                LogTags.IncomingIntent, "handleImageGeneric set uri as $sharedUri"
            )
        } else {
            Logger.logError(LogTags.IncomingIntent, "Shared image uri is null, ${intent.data}")
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

        applyWallpaperDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        val setWallpaperLayout =
            layoutInflater.inflate(R.layout.main_set_wallpaper_bottom_sheet, null)

        applyWallpaperDialog.setContentView(setWallpaperLayout)

        galleryDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        val galleryLayout = layoutInflater.inflate(R.layout.gallery_bottom_sheet, null)
        galleryDialog.setContentView(galleryLayout)

        val galleryAdapter = GalleryAdapter(
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
                Logger.logInfo(LogTags.UserInteraction, "Selected image: $uri")
            })

        val emptyGalleryText: View = galleryLayout.findViewById(R.id.emptyGalleryText)
        galleryAdapterViewModel.galleryImages.observe(this) { uriList ->
            galleryAdapter.images = uriList
            emptyGalleryText.visibility = if (uriList.isEmpty()) View.VISIBLE else View.GONE
        }

        galleryDialog.setOnShowListener {
            val bottomSheet = galleryDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)
            val displayMetrics = resources.displayMetrics
            val params = bottomSheet.layoutParams

            if (isTablet()) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT

                (bottomSheet.parent as? View)?.layoutParams?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }

                behavior.maxWidth = displayMetrics.widthPixels
                behavior.peekHeight = displayMetrics.heightPixels

                galleryDialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else {
                params.height = (displayMetrics.heightPixels * 0.75).toInt()
            }

            bottomSheet.requestLayout()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        val galleryDeleteButton = galleryLayout.findViewById<ImageView>(R.id.deleteButton)
        galleryDeleteButton.setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "Delete button pressed")
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

        galleryAdapterViewModel.selectedImages.observe(this) { selectedSet ->
            galleryAdapter.selectedUris = selectedSet
            galleryDeleteButton.visibility =
                if (selectedSet.isNotEmpty()) View.VISIBLE else View.GONE
        }


        galleryDialog.setOnDismissListener {
            galleryAdapterViewModel.clearSelectedImagesList()
        }



        galleryLayout.findViewById<RecyclerView>(R.id.galleryRecyclerView).adapter = galleryAdapter

        wallpaperPreview = findViewById(R.id.wallpaperPreview)

        tooltip = findViewById(R.id.tooltip)

        setWallpaper = findViewById(R.id.setWallpaperButton)

        val openGalleryButton = findViewById<View>(R.id.openGallery)

        setWallpaperLayout.findViewById<Button>(R.id.optionHome).setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "setWallpaperSystem button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM)
        }

        setWallpaperLayout.findViewById<Button>(R.id.optionLock).setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "setWallpaperLock button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_LOCK)
        }

        setWallpaperLayout.findViewById<Button>(R.id.optionBoth).setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "setWallpaperAll button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }

        setWallpaper.setOnClickListener {
            applyWallpaperDialog.show()
            if (isTablet()) {
                (applyWallpaperDialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        findViewById<View>(R.id.cropImage).setOnClickListener {
            imageManager.getOriginUri()?.let {
                uCropActivity.launchUCropActivity(it)
            }
        }

        findViewById<View>(R.id.openExplorer).setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        openGalleryButton.setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "Open Album button pressed")
            galleryDialog.show()
        }

        findViewById<View>(R.id.preferencesButton).setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "Open Settings button pressed")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val topStartControls = findViewById<View>(R.id.topStartControls)
        val topEndControls = findViewById<View>(R.id.topEndControls)

        listOf(
            topStartControls,
            topEndControls,
            setWallpaper,
        ).forEach { view ->
            val xmlMarginTopRecord = view.marginTop
            val xmlMarginBottomRecord = view.marginBottom
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    if (v.id == R.id.setWallpaperButton) {
                        bottomMargin = systemBars.bottom + xmlMarginBottomRecord
                    } else {
                        topMargin = systemBars.top + xmlMarginTopRecord
                    }
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
        Logger.logInfo(LogTags.SetupInterface, "Interface enabled")
        setWallpaper.isEnabled = true
        tooltip.visibility = View.INVISIBLE
    }

    private fun disableInterface() {
        Logger.logInfo(LogTags.SetupInterface, "Interface disabled")
        setWallpaper.isEnabled = false
        tooltip.visibility = View.VISIBLE
    }

    private fun refreshPreviewImage(uri: Uri) {
        if (!uri.available(this@MainActivity)) {
            Logger.logError(LogTags.Uri, "File does not exist, resetting uri: $uri")
            imageManager.triggerFailState()
            disableInterface()
        } else {
            Logger.logInfo(LogTags.Uri, "Refreshing preview image: $uri")
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
        applyWallpaperDialog.hide()
        lifecycleScope.launch {
            Logger.logInfo(LogTags.SetWallpaper, "Exit delay started")
            sendToast(getString(R.string.toast_wallpaper_applied))
            delay(1000.milliseconds)
            Logger.logInfo(LogTags.SetWallpaper, "Exit delay finished, exiting to main screen")
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