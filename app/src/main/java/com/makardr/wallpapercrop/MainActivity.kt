package com.makardr.wallpapercrop

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.utils.Logger
import com.makardr.wallpapercrop.utils.WallpaperFlag
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity() {
    private lateinit var imageManager: ImageManager
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
    private lateinit var saveStateManager: SaveStateManager
    private lateinit var uCropManager: UCropManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.logDebug(Tags.Lifecycle, "onCreate")
        setupInterface()
        saveStateManager = SaveStateManager(imageManager)
        uCropManager = UCropManager(this, imageManager)

        if (savedInstanceState != null) {
            saveStateManager.loadState(savedInstanceState)
        } else {
            handleIncomingIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveStateManager.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.logDebug(Tags.Lifecycle, "onDestroy")
        dialog.dismiss()
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
        // Activity is in foreground and interactive
        // Register listeners, start camera, resume animations
    }

    override fun onPause() {
        super.onPause()
        Logger.logDebug(Tags.Lifecycle, "onPause")
        // Losing focus
        // Unregister sensors, pause animations
    }

    override fun onStop() {
        super.onStop()
        Logger.logDebug(Tags.Lifecycle, "onStop")
        // Activity fully hidden/backgrounded
        // Save data, release heavy resources
    }

    override fun onRestart() {
        super.onRestart()
        Logger.logDebug(Tags.Lifecycle, "onRestart")
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
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (sharedUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    sharedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Logger.logInfo(
                    Tags.IncomingIntent,
                    "Success granting FLAG_GRANT_READ_URI_PERMISSION"
                )
            } catch (e: SecurityException) {
                Logger.logWarning(Tags.IncomingIntent, e.toString())
            }
            Logger.logInfo(Tags.IncomingIntent, sharedUri.toString())
            imageManager.updateOriginUri(sharedUri)
            Logger.logInfo(
                Tags.IncomingIntent,
                "handleImageGeneric set uri as ${imageManager.getOriginUri()}"
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


    @SuppressLint("InflateParams")
    private fun setupInterface() {
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
            Logger.logInfo(Tags.SetWallpaper, "setWallpaperSystem button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM)
        }

        setWallpaperLock.setOnClickListener {
            Logger.logInfo(Tags.SetWallpaper, "setWallpaperLock button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_LOCK)
        }

        setWallpaperAll.setOnClickListener {
            Logger.logInfo(Tags.SetWallpaper, "setWallpaperAll button pressed")
            setOnClickWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }

        setWallpaper.setOnClickListener {
            dialog.setContentView(setWallpaperLayout)
            dialog.show()
        }

        cropImageButton.setOnClickListener {
            imageManager.getOriginUri()?.let {
                uCropManager.launchUCropActivity(it)
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
    }

    private fun setOnClickWallpaper(@WallpaperFlag flag: Int) {
        imageManager.setWallpaper(flag)
        dialog.hide()
        lifecycleScope.launch {
            Logger.logInfo(Tags.SetWallpaper, "Exit delay started")
            delay(500.milliseconds)
            Logger.logInfo(Tags.SetWallpaper, "Exit delay finished, exiting to main screen")
            exitToTheMainScreen()
        }
    }

    private fun exitToTheMainScreen() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}