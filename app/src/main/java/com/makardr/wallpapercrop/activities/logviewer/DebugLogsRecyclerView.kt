package com.makardr.wallpapercrop.activities.logviewer

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.makardr.wallpapercrop.R
import com.makardr.wallpapercrop.data.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Locale

class DebugLogsRecyclerView(private var logs: List<LogEntry>) :
    RecyclerView.Adapter<DebugLogsRecyclerView.LogViewHolder>() {

    class LogViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.debug_log_entry, parent, false) as TextView
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(entry.timestamp)
        holder.textView.text = "$time [${entry.tag}] ${entry.message}"
        holder.textView.setTextColor(
            when (entry.level) {
                Log.ERROR -> Color.RED
                Log.WARN -> Color.rgb(255, 152, 0)
                //TODO: theme aware color
                else -> Color.BLUE
            }
        )
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<LogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}