package me.nukrs.root.installernext.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for saving installation logs to Download folder
 */
class LogSaver(private val context: Context) {
    
    companion object {
        private const val TAG = "LogSaver"
        private const val LOG_FOLDER_NAME = "InstallerNEXT_Logs"
    }
    
    /**
     * Save installation log to Download folder
     */
    suspend fun saveInstallationLog(
        packageName: String,
        errorMessage: String,
        detailedLog: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Get Download directory
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logDir = File(downloadDir, LOG_FOLDER_NAME)
            
            // Create log directory if it doesn't exist
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // Generate log file name with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFileName = "install_error_${packageName}_$timestamp.log"
            val logFile = File(logDir, logFileName)
            
            // Prepare log content
            val logContent = buildString {
                appendLine("=== InstallerNEXT Installation Error Log ===")
                appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Package: $packageName")
                appendLine("Error: $errorMessage")
                appendLine()
                appendLine(detailedLog)
                appendLine()
                appendLine("=== End of Log ===")
            }
            
            // Write log to file
            logFile.writeText(logContent)
            
            android.util.Log.d(TAG, "Installation log saved to: ${logFile.absolutePath}")
            logFile.absolutePath
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save installation log", e)
            null
        }
    }
    
    /**
     * Get the log directory path
     */
    fun getLogDirectoryPath(): String {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, LOG_FOLDER_NAME).absolutePath
    }
    
    /**
     * Clean up old log files (keep only last 10 files)
     */
    suspend fun cleanupOldLogs() = withContext(Dispatchers.IO) {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logDir = File(downloadDir, LOG_FOLDER_NAME)
            
            if (logDir.exists() && logDir.isDirectory) {
                val logFiles = logDir.listFiles { file ->
                    file.isFile && file.name.startsWith("install_error_") && file.name.endsWith(".log")
                }
                
                if (logFiles != null && logFiles.size > 10) {
                    // Sort by last modified time (oldest first)
                    val sortedFiles = logFiles.sortedBy { it.lastModified() }
                    
                    // Delete oldest files, keep only last 10
                    val filesToDelete = sortedFiles.dropLast(10)
                    filesToDelete.forEach { file ->
                        try {
                            file.delete()
                            android.util.Log.d(TAG, "Deleted old log file: ${file.name}")
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Failed to delete log file: ${file.name}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
}