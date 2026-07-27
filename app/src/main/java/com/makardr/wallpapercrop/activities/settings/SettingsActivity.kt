package com.makardr.wallpapercrop.activities.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.activities.logviewer.DebugLogsActivity
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.ImageRepository
import com.makardr.wallpapercrop.data.PreferencesRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private var preferencesRepository = PreferencesRepository.getInstance(this)
    private var imageRepository = ImageRepository.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository.getInstance(this)

        setupInterface()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun setupInterface() {
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        setContentView(R.layout.settings_activity)

        findViewById<Button>(R.id.galleryPreference).setOnClickListener {
            preferencesRepository.galleryEnabled = !preferencesRepository.galleryEnabled
            Logger.logInfo(
                LogTags.UserInteraction,
                "Gallery preference changed ${preferencesRepository.galleryEnabled}"
            )
        }


        findViewById<Button>(R.id.wipeData).setOnClickListener {
            lifecycleScope.launch {
                imageRepository.deleteAllFiles()
            }
        }

        findViewById<Button>(R.id.debugLogs).setOnClickListener {
            Logger.logInfo(LogTags.UserInteraction, "Debug logs button clicked")
            startActivity(Intent(this, DebugLogsActivity::class.java))
        }

        findViewById<Button>(R.id.returnButton).setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}