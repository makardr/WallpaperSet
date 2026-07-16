package com.makardr.wallpapercrop.activities.logviewer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.common.utils.isTablet
import com.makardr.wallpapercrop.data.LogsRepository
import com.makardr.wallpapercrop.data.model.LogTags

class DebugLogsActivity : AppCompatActivity() {
    private val logRepository: LogsRepository = LogsRepository.getInstance()
    private lateinit var logAdapter: DebugLogsRecyclerView
    private val selectedTags = mutableSetOf<LogTags>()

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
        setContentView(R.layout.debug_logs_activity)

        logAdapter = DebugLogsRecyclerView(logRepository.logs)
        findViewById<RecyclerView>(R.id.recyclerLogs).apply {
            layoutManager = LinearLayoutManager(this@DebugLogsActivity)
            adapter = logAdapter
        }

        findViewById<Button>(R.id.btnFilterAll).setOnClickListener {
            logAdapter.updateLogs(logRepository.logs)
        }

        findViewById<Button>(R.id.btnFilterTags).setOnClickListener {
            showTagFilterDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun showTagFilterDialog() {
        val tags = LogTags.entries.toTypedArray()
        val tagNames = tags.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(tags.size) { selectedTags.contains(tags[it]) }

        MaterialAlertDialogBuilder(this)
            .setTitle("Filter by tag")
            .setMultiChoiceItems(tagNames, checkedItems) { _, which, isChecked ->
                if (isChecked) selectedTags.add(tags[which]) else selectedTags.remove(tags[which])
            }
            .setPositiveButton("Apply") { _, _ ->
                val filtered = if (selectedTags.isEmpty()) {
                    logRepository.logs
                } else {
                    selectedTags
                        .flatMap { logRepository.filterByTag(it) }
                        .distinct()
                        .sortedBy { it.timestamp }
                }
                logAdapter.updateLogs(filtered)
            }
            .setNeutralButton("Clear") { _, _ ->
                selectedTags.clear()
                logAdapter.updateLogs(logRepository.logs)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}