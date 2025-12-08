package us.leaf3stones.hy2droid.proxy

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized log management for Hysteria
 */
object LogManager {
    private const val MAX_LOGS = 1000 // Maximum number of logs to keep in memory
    private const val TAG = "HysteriaVPN"
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val message: String
    )
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun addLog(level: LogLevel, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, level, message)
        
        // Output to Android logcat
        when (level) {
            LogLevel.VERBOSE -> Log.v(TAG, message)
            LogLevel.DEBUG -> Log.d(TAG, message)
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.WARN -> Log.w(TAG, message)
            LogLevel.ERROR -> Log.e(TAG, message)
        }
        
        _logs.value = (_logs.value + entry).takeLast(MAX_LOGS)
    }
    
    fun log(message: String) {
        addLog(LogLevel.INFO, message)
    }
    
    fun debug(message: String) {
        addLog(LogLevel.DEBUG, message)
    }
    
    fun warn(message: String) {
        addLog(LogLevel.WARN, message)
    }
    
    fun error(message: String) {
        addLog(LogLevel.ERROR, message)
    }
    
    fun verbose(message: String) {
        addLog(LogLevel.VERBOSE, message)
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}
