package com.makardr.wallpapercrop.activities.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.core.net.toUri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.activities.logviewer.DebugLogsActivity
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.ImageRepository
import com.makardr.wallpapercrop.data.PreferencesRepository
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.BuildConfig
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var imageRepository: ImageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository.getInstance(this)
        imageRepository = ImageRepository.getInstance(this)

        setupInterface()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupInterface() {
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        setContentView(R.layout.settings_activity)

        val root = findViewById<View>(R.id.main)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        val gallerySwitch = findViewById<SwitchMaterial>(R.id.gallerySwitch)
        gallerySwitch.isChecked = preferencesRepository.galleryEnabled

        findViewById<LinearLayout>(R.id.galleryContainer).setOnClickListener {
            val newState = !preferencesRepository.galleryEnabled
            preferencesRepository.galleryEnabled = newState
            gallerySwitch.isChecked = newState
            Logger.logInfo(
                LogTags.UserInteraction,
                "Gallery preference changed $newState"
            )
        }

        findViewById<LinearLayout>(R.id.wipeData).setOnClickListener {
            lifecycleScope.launch {
                imageRepository.deleteAllFiles()
                Logger.logInfo(LogTags.UserInteraction, "Wipe data button clicked")
            }
        }

        val debugLogsButton = findViewById<LinearLayout>(R.id.debugLogs)

        debugLogsButton.setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "Debug logs button clicked")
            startActivity(Intent(this, DebugLogsActivity::class.java))
        }

        if (!BuildConfig.DEBUG) {
            debugLogsButton.visibility = View.GONE
        }

        findViewById<LinearLayout>(R.id.authorMakar).setOnClickListener {
            openUrl("https://github.com/makardr")
        }

        findViewById<LinearLayout>(R.id.authorAleksandr).setOnClickListener {
            openUrl("https://github.com/skaller-hub")
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updatePadding(top = systemBars.top)
            v.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}