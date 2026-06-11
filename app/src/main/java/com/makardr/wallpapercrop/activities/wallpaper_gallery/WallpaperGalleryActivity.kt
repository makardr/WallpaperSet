package com.makardr.wallpapercrop.activities.wallpaper_gallery

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.utils.isTablet

class WallpaperGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInterface()

    }

    private fun setupInterface() {
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallpaper_gallery)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}