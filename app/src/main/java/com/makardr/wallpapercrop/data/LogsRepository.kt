package com.makardr.wallpapercrop.data

import com.makardr.wallpapercrop.data.model.LogEntry
import com.makardr.wallpapercrop.data.model.LogTags

class LogsRepository private constructor() {

    private val _logs = ArrayDeque<LogEntry>(MAX_LOGS)
    private val lock = Any()

    val logs: List<LogEntry>
        get() = synchronized(lock) { _logs.toList() }

    fun writeLog(logEntry: LogEntry) {
        synchronized(lock) {
            _logs.addLast(logEntry)
            if (_logs.size > MAX_LOGS) {
                _logs.removeFirst()
            }
        }
    }


    fun filterByTag(tag: LogTags): List<LogEntry> = logs.filter { it.tag == tag }

    companion object {
        @Volatile
        private var INSTANCE: LogsRepository? = null
        private const val MAX_LOGS = 200

        fun getInstance(): LogsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogsRepository().also { INSTANCE = it }
            }
        }
    }
}