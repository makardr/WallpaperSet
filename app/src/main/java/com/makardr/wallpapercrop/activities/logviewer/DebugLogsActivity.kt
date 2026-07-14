package com.makardr.wallpapercrop.activities.logviewer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.LogsRepository

class DebugLogsActivity : AppCompatActivity() {
    private val logRepository: LogsRepository = LogsRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInterface()

    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun setupInterface() {
        if (!isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_debug_logs)

        findViewById<RecyclerView>(R.id.recyclerLogs).apply {
            layoutManager = LinearLayoutManager(this@DebugLogsActivity)
            adapter = LogAdapter(logRepository.logs)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}