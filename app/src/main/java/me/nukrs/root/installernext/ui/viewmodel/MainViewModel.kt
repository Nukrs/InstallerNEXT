package me.nukrs.root.installernext.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.nukrs.root.installernext.data.ApkInfo
import me.nukrs.root.installernext.utils.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(private val context: Context) : ViewModel() {
    
    private val apkParser = ApkParser(context)
    private val rootInstaller = RootInstaller(context)
    private val logSaver = LogSaver(context)
    
    // UI State
    var selectedApkInfo by mutableStateOf<ApkInfo?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var showInstallDialog by mutableStateOf(false)
        private set
    
    var isRootAvailable by mutableStateOf(false)
        private set
    
    var installationProgress by mutableStateOf<String?>(null)
        private set
    
    var isInstalling by mutableStateOf(false)
        private set
    
    var signatureInfo by mutableStateOf<SignatureInfo?>(null)
        private set
    
    var installationStatus by mutableStateOf<InstallationStatus?>(null)
        private set
    
    var showConfirmationDialog by mutableStateOf(false)
        private set
    
    // Error dialog state
    var showErrorDialog by mutableStateOf(false)
        private set
    
    var errorDialogData by mutableStateOf<InstallResult.Error?>(null)
        private set
    
    var logFilePath by mutableStateOf<String?>(null)
        private set
    
    // Success dialog state
    var showSuccessDialog by mutableStateOf(false)
        private set
    
    var successDialogData by mutableStateOf<InstallResult.Success?>(null)
        private set
    
    init {
        checkRootAccess()
    }
    
    fun selectApkFile(file: File) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val apkInfo = apkParser.parseApk(file)
                if (apkInfo != null) {
                    selectedApkInfo = apkInfo
                    signatureInfo = apkParser.getSignatureInfo(apkInfo)
                    installationStatus = apkParser.getInstallationStatus(apkInfo)
                    showInstallDialog = true
                } else {
                    errorMessage = "Failed to parse APK file"
                }
            } catch (e: Exception) {
                errorMessage = "Error parsing APK: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun dismissInstallDialog() {
        showInstallDialog = false
        selectedApkInfo = null
        signatureInfo = null
        installationStatus = null
    }
    
    fun installApk() {
        val apkInfo = selectedApkInfo ?: return
        
        // Check if we need confirmation for signature change or downgrade
        if (apkInfo.isUpdate && (apkInfo.isSignatureChanged || isDowngrade(apkInfo))) {
            showConfirmationDialog = true
            return
        }
        
        performInstallation()
    }
    
    fun confirmInstallation() {
        android.util.Log.d("MainViewModel", "confirmInstallation called")
        showConfirmationDialog = false
        performInstallation()
    }
    
    fun dismissConfirmationDialog() {
        showConfirmationDialog = false
    }
    
    fun isDowngrade(apkInfo: ApkInfo): Boolean {
        return apkInfo.isUpdate && 
               apkInfo.existingPackageInfo != null && 
               apkInfo.versionCode < apkInfo.existingPackageInfo.longVersionCode
    }
    
    private fun performInstallation() {
        val apkInfo = selectedApkInfo ?: return
        android.util.Log.d("MainViewModel", "performInstallation called for package: ${apkInfo.packageName}")
        
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Starting installation coroutine")
            isInstalling = true
            installationProgress = "Starting installation..."
            
            try {
                val result = rootInstaller.installApk(apkInfo, installationStatus) { progress ->
                    installationProgress = progress
                }
                
                when (result) {
                    is InstallResult.Success -> {
                        android.util.Log.d("MainViewModel", "Installation successful: ${result.message}")
                        // Show success dialog
                        successDialogData = result
                        showSuccessDialog = true
                        installationProgress = null
                        // Close the installation dialog
                        showInstallDialog = false
                    }
                    is InstallResult.Error -> {
                        // Save detailed log to file
                        val savedLogPath = logSaver.saveInstallationLog(
                            packageName = apkInfo.packageName,
                            errorMessage = result.message,
                            detailedLog = result.detailedLog
                        )
                        
                        // Set error dialog data
                        errorDialogData = result
                        logFilePath = savedLogPath
                        showErrorDialog = true
                        
                        // Also set the simple error message for backward compatibility
                        errorMessage = result.message
                        installationProgress = null
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Installation failed: ${e.message}"
                val detailedLog = """
                    Exception occurred during installation:
                    Error: ${e.message}
                    Stack trace: ${e.stackTraceToString()}
                    
                    APK Information:
                    Package: ${apkInfo.packageName}
                    Version: ${apkInfo.versionName} (${apkInfo.versionCode})
                    File: ${apkInfo.file.absolutePath}
                    Size: ${apkInfo.fileSize} bytes
                    Is Update: ${apkInfo.isUpdate}
                """.trimIndent()
                
                // Save detailed log to file
                val savedLogPath = logSaver.saveInstallationLog(
                    packageName = apkInfo.packageName,
                    errorMessage = errorMsg,
                    detailedLog = detailedLog
                )
                
                // Create error result for dialog
                val errorResult = InstallResult.Error(errorMsg, detailedLog)
                errorDialogData = errorResult
                logFilePath = savedLogPath
                showErrorDialog = true
                
                errorMessage = errorMsg
                installationProgress = null
            } finally {
                isInstalling = false
            }
        }
    }
    
    fun uninstallExistingApp() {
        val packageName = selectedApkInfo?.packageName ?: return
        
        viewModelScope.launch {
            isInstalling = true
            installationProgress = "Uninstalling existing app..."
            
            try {
                val result = rootInstaller.uninstallApp(packageName) { progress ->
                    installationProgress = progress
                }
                
                when (result) {
                    is InstallResult.Success -> {
                        installationProgress = result.message
                        // Refresh APK info after uninstall
                        selectedApkInfo?.let { apkInfo ->
                            val refreshedInfo = apkParser.parseApk(apkInfo.file)
                            selectedApkInfo = refreshedInfo
                            refreshedInfo?.let {
                                signatureInfo = apkParser.getSignatureInfo(it)
                                installationStatus = apkParser.getInstallationStatus(it)
                            }
                        }
                        kotlinx.coroutines.delay(1000)
                        installationProgress = null
                    }
                    is InstallResult.Error -> {
                        errorMessage = result.message
                        installationProgress = null
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Uninstallation failed: ${e.message}"
                installationProgress = null
            } finally {
                isInstalling = false
            }
        }
    }
    
    private fun checkRootAccess() {
        viewModelScope.launch {
            isRootAvailable = rootInstaller.checkRootAccess()
        }
    }
    
    fun requestRootAccess() {
        viewModelScope.launch {
            isRootAvailable = rootInstaller.requestRootAccess()
        }
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    fun dismissErrorDialog() {
        showErrorDialog = false
        errorDialogData = null
        logFilePath = null
    }
    
    fun dismissSuccessDialog() {
        showSuccessDialog = false
        successDialogData = null
    }
}