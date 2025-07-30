package me.nukrs.root.installernext.utils

import android.content.Context
import com.topjohnwu.superuser.Shell
import me.nukrs.root.installernext.data.ApkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Root installer for APK files using libsu
 */
class RootInstaller(private val context: Context) {
    
    companion object {
        private const val TAG = "RootInstaller"
    }
    
    /**
     * Check if root access is available
     */
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request root access
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Install APK using root privileges
     */
    suspend fun installApk(
        apkInfo: ApkInfo,
        installationStatus: InstallationStatus? = null,
        onProgress: (String) -> Unit = {}
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "installApk called for package: ${apkInfo.packageName}")
            if (!checkRootAccess()) {
                android.util.Log.e(TAG, "Root access not available")
                return@withContext InstallResult.Error("Root access not available")
            }
            
            onProgress("Preparing installation...")
            
            val apkFile = apkInfo.file
            val packageName = apkInfo.packageName
            
            // Ensure we're running with su privileges
            val shell = Shell.getShell()
            if (!shell.isRoot) {
                return@withContext InstallResult.Error("Failed to obtain root privileges")
            }
            
            // Copy APK to temporary location accessible by system
            val tempDir = "/data/local/tmp"
            val tempApkPath = "$tempDir/${apkFile.name}"
            
            onProgress("Copying APK file...")
            
            // Copy file to temp location using su
            val copyResult = shell.newJob().add("cp '${apkFile.absolutePath}' '$tempApkPath'").exec()
            if (!copyResult.isSuccess) {
                return@withContext InstallResult.Error("Failed to copy APK file")
            }
            
            // Set proper permissions using su
            shell.newJob().add("chmod 644 '$tempApkPath'").exec()
            
            onProgress("Installing application...")
            
            // Pre-installation diagnostic checks
            android.util.Log.d(TAG, "Performing pre-installation diagnostics...")
            
            // Check device storage space
            val storageCheckResult = shell.newJob().add("df /data").exec()
            val storageOutput = storageCheckResult.out.joinToString("\n")
            android.util.Log.d(TAG, "Storage check: $storageOutput")
            
            // Check if APK file exists and is readable
            val fileCheckResult = shell.newJob().add("ls -la '$tempApkPath'").exec()
            val fileCheckOutput = fileCheckResult.out.joinToString("\n")
            android.util.Log.d(TAG, "File check: $fileCheckOutput")
            
            // Check package manager status
            val pmCheckResult = shell.newJob().add("pm list packages | head -5").exec()
            val pmCheckOutput = pmCheckResult.out.joinToString("\n")
            android.util.Log.d(TAG, "Package manager check: $pmCheckOutput")
            
            // Determine if this is a risky installation
            val isRiskyInstall = installationStatus?.hasRiskyIssues == true
            android.util.Log.d(TAG, "isRiskyInstall: $isRiskyInstall")
            
            // Install using pm install command with appropriate flags
            val installCommand = when {
                isRiskyInstall -> {
                    // For risky installations, try uninstall with data preservation first, then force install if needed
                    if (apkInfo.isUpdate && (apkInfo.isSignatureChanged || isDowngrade(apkInfo))) {
                        android.util.Log.d(TAG, "Detected signature change or downgrade, trying uninstall with data preservation first")
                        onProgress("Preparing for signature change or downgrade installation...")
                        
                        // First try uninstall keeping data and cache directories (-k flag)
                        val uninstallKeepDataCommand = "pm uninstall -k ${apkInfo.packageName}"
                        val uninstallKeepDataResult = shell.newJob().add(uninstallKeepDataCommand).exec()
                        
                        if (uninstallKeepDataResult.isSuccess) {
                            android.util.Log.d(TAG, "Successfully uninstalled existing app keeping data, now installing new version")
                            onProgress("Installing new version...")
                            "pm install '$tempApkPath'"
                        } else {
                            // If uninstall with data preservation failed, try normal uninstall
                            android.util.Log.d(TAG, "Uninstall with data preservation failed, trying normal uninstall")
                            onProgress("Trying normal uninstall...")
                            
                            val uninstallCommand = "pm uninstall ${apkInfo.packageName}"
                            val uninstallResult = shell.newJob().add(uninstallCommand).exec()
                            
                            if (uninstallResult.isSuccess) {
                                android.util.Log.d(TAG, "Successfully uninstalled existing app, now installing new version")
                                onProgress("Installing new version...")
                                "pm install '$tempApkPath'"
                            } else {
                                // If both uninstall methods failed, try force install
                                android.util.Log.d(TAG, "All uninstall methods failed, trying force install")
                                onProgress("Trying force install...")
                                "pm install -r -d '$tempApkPath'"
                            }
                        }
                    } else {
                        // For other risky installations, use simplified command
                        android.util.Log.d(TAG, "Using simplified risky install command")
                        "pm install -r '$tempApkPath'"
                    }
                }
                apkInfo.isUpdate -> {
                    android.util.Log.d(TAG, "Using update install command with -r flag")
                    "pm install -r '$tempApkPath'"
                }
                else -> {
                    android.util.Log.d(TAG, "Using normal install command")
                    "pm install '$tempApkPath'"
                }
            }
            
            android.util.Log.d(TAG, "Executing install command: $installCommand")
            
            val installResult = shell.newJob().add(installCommand).exec()
            
            // Get output and error information for judgment
            val outputLog = installResult.out.joinToString("\n")
            val errorLog = installResult.err.joinToString("\n")
            
            android.util.Log.d(TAG, "Installation result - Success: ${installResult.isSuccess}, Exit Code: ${installResult.code}, Output: $outputLog")
            
            // Clean up temporary file using su
            shell.newJob().add("rm '$tempApkPath'").exec()
            
            // Determine if installation was successful
            if (installResult.isSuccess && installResult.code == 0) {
                // Installation successful: exit code is 0 and isSuccess is true
                onProgress("Installation completed successfully")
                android.util.Log.d(TAG, "Installation successful - Exit code: ${installResult.code}")
                InstallResult.Success("Success")
            } else if (installResult.isSuccess && outputLog.contains("Success")) {
                // Alternative success judgment: even if exit code is not 0, but output contains Success
                onProgress("Installation completed successfully")
                android.util.Log.d(TAG, "Installation successful - Output contains Success")
                InstallResult.Success("Success")
            } else {
                // Installation failed, generate detailed log
                val errorMessage = if (errorLog.isNotEmpty()) {
                    "Installation failed: $errorLog"
                } else if (outputLog.isNotEmpty()) {
                    "Installation failed: $outputLog"
                } else {
                    "Installation failed with exit code: ${installResult.code}"
                }
                
                // Only generate detailed log when failed
                val detailedLog = buildString {
                    appendLine("=== Installation Command ===")
                    appendLine(installCommand)
                    appendLine("\n=== Installation Output ===")
                    appendLine(outputLog.ifEmpty { "No output" })
                    if (errorLog.isNotEmpty()) {
                        appendLine("\n=== Installation Errors ===")
                        appendLine(errorLog)
                    }
                    appendLine("\n=== Installation Status ===")
                    appendLine("Success: ${installResult.isSuccess}")
                    appendLine("Exit Code: ${installResult.code}")
                    appendLine("\n=== APK Information ===")
                    appendLine("Package: ${apkInfo.packageName}")
                    appendLine("Version: ${apkInfo.versionName} (${apkInfo.versionCode})")
                    appendLine("File: ${apkInfo.file.absolutePath}")
                    appendLine("Size: ${apkInfo.getFormattedSize()}")
                    appendLine("Is Update: ${apkInfo.isUpdate}")
                    appendLine("Is Risky Install: $isRiskyInstall")
                    if (apkInfo.isUpdate && apkInfo.existingPackageInfo != null) {
                        appendLine("Existing Version: ${apkInfo.existingPackageInfo.versionName} (${apkInfo.existingPackageInfo.longVersionCode})")
                    }
                }
                
                android.util.Log.e(TAG, errorMessage)
                InstallResult.Error(errorMessage, detailedLog)
            }
            
        } catch (e: Exception) {
            val errorMessage = "Installation error: ${e.message}"
            val detailedLog = buildString {
                appendLine("=== Exception Details ===")
                appendLine("Error: ${e.message}")
                appendLine("Stack Trace:")
                appendLine(e.stackTraceToString())
                appendLine("\n=== APK Information ===")
                appendLine("Package: ${apkInfo.packageName}")
                appendLine("Version: ${apkInfo.versionName} (${apkInfo.versionCode})")
                appendLine("File: ${apkInfo.file.absolutePath}")
                appendLine("Size: ${apkInfo.getFormattedSize()}")
                appendLine("Is Update: ${apkInfo.isUpdate}")
            }
            android.util.Log.e(TAG, errorMessage, e)
            InstallResult.Error(errorMessage, detailedLog)
        }
    }
    
    private fun isDowngrade(apkInfo: ApkInfo): Boolean {
        return apkInfo.isUpdate && 
               apkInfo.existingPackageInfo != null && 
               apkInfo.versionCode < apkInfo.existingPackageInfo.longVersionCode
    }
    
    /**
     * Uninstall an application using root
     */
    suspend fun uninstallApp(
        packageName: String,
        onProgress: (String) -> Unit = {}
    ): InstallResult = withContext(Dispatchers.IO) {
        try {
            if (!checkRootAccess()) {
                return@withContext InstallResult.Error("Root access not available")
            }
            
            // Ensure we're running with su privileges
            val shell = Shell.getShell()
            if (!shell.isRoot) {
                return@withContext InstallResult.Error("Failed to obtain root privileges")
            }
            
            onProgress("Uninstalling application...")
            
            val uninstallResult = shell.newJob().add("pm uninstall '$packageName'").exec()
            
            if (uninstallResult.isSuccess) {
                val output = uninstallResult.out.joinToString("\n")
                if (output.contains("Success")) {
                    onProgress("Uninstallation completed successfully")
                    InstallResult.Success("Uninstall successful")
                } else {
                    InstallResult.Error("Uninstallation failed: $output")
                }
            } else {
                val error = uninstallResult.err.joinToString("\n")
                InstallResult.Error("Uninstallation failed: $error")
            }
            
        } catch (e: Exception) {
            InstallResult.Error("Uninstallation error: ${e.message}")
        }
    }
}

/**
 * Result of installation operation
 */
sealed class InstallResult {
    data class Success(val message: String = "Installation completed successfully") : InstallResult()
    data class Error(val message: String, val detailedLog: String = "") : InstallResult()
}