package com.tokoreader.data.local.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, WARN, ERROR, DEBUG
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

object AppLogger {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private const val MAX_LOGS = 500

    private fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_LOGS) {
                updated.drop(updated.size - MAX_LOGS)
            } else {
                updated
            }
        }
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        addLog(LogLevel.INFO, tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        addLog(LogLevel.WARN, tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$msg\n${Log.getStackTraceString(throwable)}" else msg
        Log.e(tag, fullMsg)
        addLog(LogLevel.ERROR, tag, fullMsg)
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        addLog(LogLevel.DEBUG, tag, msg)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getLogsAsText(): String {
        return _logs.value.joinToString("\n") { "[${it.timestamp}] [${it.level}] [${it.tag}] ${it.message}" }
    }
}
