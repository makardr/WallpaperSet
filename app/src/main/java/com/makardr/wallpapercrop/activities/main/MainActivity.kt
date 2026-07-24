package com.makardr.wallpapercrop.activities.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Rect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity() {
    //Init components
    private val imageManager: ImageManagerViewModel by viewModels()
    private val galleryAdapterViewModel: GalleryAdapterViewModel by viewModels()

    private lateinit var uCropActivity: UCropActivity
    private lateinit var preferencesRepository: PreferencesRepository

    //Interface state
    private var isPanelExpanded = true
    private val keyPanelExpanded = "key_panel_expanded"

    //Interface elements
    private lateinit var wallpaperPreview: ImageView
    private lateinit var setWallpaper: MaterialButton
    private lateinit var tooltip: TextView
    private lateinit var applyWallpaperDialog: Dialog
    private lateinit var galleryDialog: BottomSheetDialog
    private lateinit var topControlsInner: View
    private lateinit var panelToggle: ImageView

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.logInfo(LogTags.Lifecycle, "onCreate")
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        preferencesRepository = PreferencesRepository.getInstance(this)
        setupInterface()
        setInterfaceEnabled(false)
        uCropActivity = UCropActivity(this, imageManager)
        collectEvents()

        if (savedInstanceState != null) {
            if (imageManager.getDisplayedImageUri() != null) {
                refreshPreviewImage(imageManager.getDisplayedImageUri())
                setInterfaceEnabled(true)
            }
            isPanelExpanded = savedInstanceState.getBoolean(keyPanelExpanded)
        } else {
            handleIncomingIntent(intent)
        }
        applyPanelState()
        Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
    }

    private fun collectEvents() {
        lifecycleScope.launch {
            Logger.logDebug(LogTags.Lifecycle, "Starting event listening")
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                imageManager.refreshImageEventChannel.collect {
                    refreshPreviewImage(imageManager.getDisplayedImageUri())
                    Logger.logCurrentAppState(imageManager, wallpaperPreview, tooltip)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(keyPanelExpanded, isPanelExpanded)
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
        galleryAdapterViewModel.refreshGallery()
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
                    imageManager.saveWallpaperEnabled = false
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
                            imageManager.getOriginUri()?.let { uri ->
                                if (!uri.available(this@MainActivity)) {
                                    Logger.logInfo(
                                        LogTags.Uri,
                                        "Uri is not available on delete, resetting image"
                                    )
                                    imageManager.resetImage()
                                }
                            }
                            setInterfaceEnabled(false)
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
                if (selectedSet.isNotEmpty()) View.VISIBLE else View.INVISIBLE
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
                (applyWallpaperDialog as? BottomSheetDialog)?.behavior?.state =
                    BottomSheetBehavior.STATE_EXPANDED
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

        val topPanelContainer = findViewById<View>(R.id.topPanelContainer)
        panelToggle = findViewById(R.id.panelToggle)
        topControlsInner = findViewById(R.id.topControls)
        panelToggle.setOnClickListener {
            togglePanelState()
        }

        listOf(
            topPanelContainer,
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

        if (preferencesRepository.galleryEnabled) {
            openGalleryButton.visibility = View.VISIBLE
        } else {
            openGalleryButton.visibility = View.INVISIBLE
        }
    }

    private fun applyPanelState() {
        Logger.logInfo(LogTags.SetupInterface, "Current panel state $isPanelExpanded")
        if (isPanelExpanded) {
            topControlsInner.visibility = View.VISIBLE
            topControlsInner.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .setListener(null)
                .start()
            panelToggle.setBackgroundResource(R.drawable.bg_semicircle_filled)
            panelToggle.setImageResource(R.drawable.ic_arrow_left)
        } else {
            val distance = -(topControlsInner.left + topControlsInner.width).toFloat()
            topControlsInner.animate()
                .translationX(distance)
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    topControlsInner.visibility = View.GONE
                }
                .start()
            panelToggle.setBackgroundResource(R.drawable.bg_semicircle_empty)
            panelToggle.setImageResource(R.drawable.ic_arrow_right)
        }
    }

    private fun togglePanelState() {
        isPanelExpanded = !isPanelExpanded
        applyPanelState()
        Logger.logInfo(LogTags.UserInteraction, "Set current panel state to $isPanelExpanded")
    }

    private fun setInterfaceEnabled(enabled: Boolean) {
        Logger.logInfo(
            LogTags.SetupInterface,
            "Interface ${if (enabled) "enabled" else "disabled"}"
        )
        setWallpaper.isEnabled = enabled
        tooltip.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }

    private fun refreshPreviewImage(uri: Uri?) {
        when {
            uri == null -> {
                Logger.logInfo(LogTags.Uri, "Refreshing preview image, uri is null")
                wallpaperPreview.load(uri) {
                    crossfade(true)
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
                setInterfaceEnabled(false)
            }

            !uri.available(this@MainActivity) -> {
                Logger.logError(LogTags.Uri, "File does not exist, resetting uri: $uri")
                imageManager.resetImage()
                setInterfaceEnabled(false)
            }

            else -> {
                Logger.logInfo(LogTags.Uri, "Refreshing preview image: $uri")
                wallpaperPreview.load(uri) {
                    crossfade(true)
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
                setInterfaceEnabled(true)
            }
        }

    }

    private fun setOnClickWallpaper(@WallpaperFlag flag: Int) {
        val uri = imageManager.getDisplayedImageUri() ?: return
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        wallpaperManager.setStream(stream, calculateCropHint(uri), true, flag)
                    } ?: error("Could not open input stream")
                }
            }
            if (success.isFailure) {
                Logger.logError(
                    LogTags.SetWallpaper,
                    "Failed to apply wallpaper ${success.exceptionOrNull()}"
                )
                sendToast("Failed to apply wallpaper ${success.exceptionOrNull()?.message.toString()}")
            } else {
                if (imageManager.saveWallpaperEnabled && preferencesRepository.galleryEnabled) {
                    galleryAdapterViewModel.saveImage(uri)
                }
                imageManager.saveWallpaperEnabled = false
                applyWallpaperDialog.hide()
                sendToast(getString(R.string.toast_wallpaper_applied))

                Logger.logInfo(LogTags.SetWallpaper, "Exit delay started")

                delay(1000.milliseconds)
                Logger.logInfo(
                    LogTags.SetWallpaper,
                    "Exit delay finished, exiting to main screen"
                )
                exitToTheMainScreen()
            }
        }

    }

    private fun calculateCropHint(uri: Uri): Rect {
        Logger.logDebug(LogTags.DimensionCrop, "========================================")
        val (imageWidth, imageHeight) = getImageDimensions(uri)
        Logger.logDebug(
            LogTags.DimensionCrop,
            "screenWidth $screenWidth, screenHeight $screenHeight, imageWidth $imageWidth, imageHeight $imageHeight"
        )

        val scale = maxOf(
            screenWidth.toFloat() / imageWidth, screenHeight.toFloat() / imageHeight
        )
        Logger.logDebug(LogTags.DimensionCrop, "scale $scale")

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        Logger.logDebug(
            LogTags.DimensionCrop,
            "scaledWidth $scaledWidth, scaledHeight $scaledHeight"
        )

        val offsetX = (scaledWidth - screenWidth) / 2f
        val offsetY = (scaledHeight - screenHeight) / 2f

        Logger.logDebug(LogTags.DimensionCrop, "offsetX $offsetX, offsetY $offsetY")


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
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return Pair(options.outWidth, options.outHeight)
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