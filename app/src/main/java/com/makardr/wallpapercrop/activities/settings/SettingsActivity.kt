package com.makardr.wallpapercrop.activities.settings

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.ImageRepository
import com.makardr.wallpapercrop.data.PreferencesRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private var preferencesRepository = PreferencesRepository.getInstance(this)
    private var imageRepository = ImageRepository.getInstance(this)
    private lateinit var galleryPreference: Button
    private lateinit var wipeData: Button

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
        galleryPreference = findViewById(R.id.galleryPreference)
        wipeData = findViewById(R.id.wipeData)

        //TODO: switching gallery preference is unreliably updates gallery button state
        galleryPreference.setOnClickListener {
            preferencesRepository.galleryEnabled = !preferencesRepository.galleryEnabled
            Logger.logInfo(
                LogTags.UserInteraction,
                "Gallery preference changed ${preferencesRepository.galleryEnabled}"
            )
        }
        wipeData.setOnClickListener {
            lifecycleScope.launch {
                imageRepository.deleteAllFiles()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}