package me.nukrs.root.installernext.ui.activity

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.nukrs.root.installernext.data.ApkInfo
import me.nukrs.root.installernext.utils.*
import java.io.File

class InstallationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val apkParser = ApkParser(context)
    private val rootInstaller = RootInstaller(context)
    private val logSaver = LogSaver(context)
    
    // APK Information
    var apkInfo by mutableStateOf<ApkInfo?>(null)
        private set
    
    var signatureInfo by mutableStateOf<SignatureInfo?>(null)
        private set
    
    var installationStatus by mutableStateOf<InstallationStatus?>(null)
        private set
    
    // Installation State
    var isInstalling by mutableStateOf(false)
        private set
    
    var installationProgress by mutableStateOf<String?>(null)
        private set
    
    var installationResult by mutableStateOf<InstallResult?>(null)
        private set
    
    // Root Access
    var isRootAvailable by mutableStateOf(false)
        private set
    
    // Dialog States
    var showSuccessDialog by mutableStateOf(false)
        private set
    
    var showErrorDialog by mutableStateOf(false)
        private set
    
    var successMessage by mutableStateOf<String?>(null)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var detailedLog by mutableStateOf<String?>(null)
        private set
    
    var logFilePath by mutableStateOf<String?>(null)
        private set
    
    init {
        checkRootAccess()
    }
    
    fun initializeWithApk(apkFile: File) {
        viewModelScope.launch {
            try {
                val parsedApkInfo = apkParser.parseApk(apkFile)
                apkInfo = parsedApkInfo
                
                parsedApkInfo?.let { info ->
                    signatureInfo = apkParser.getSignatureInfo(info)
                    installationStatus = apkParser.getInstallationStatus(info)
                }
            } catch (e: Exception) {
                android.util.Log.e("InstallationViewModel", "Failed to parse APK", e)
                errorMessage = "Failed to parse APK: ${e.message}"
                showErrorDialog = true
            }
        }
    }
    
    fun installApk() {
        val currentApkInfo = apkInfo ?: return
        android.util.Log.d("InstallationViewModel", "installApk called for package: ${currentApkInfo.packageName}")
        
        viewModelScope.launch {
            android.util.Log.d("InstallationViewModel", "Starting installation coroutine")
            isInstalling = true
            installationProgress = "Starting installation..."
            
            try {
                val result = rootInstaller.installApk(currentApkInfo, installationStatus) { progress ->
                    installationProgress = progress
                }
                
                installationResult = result
                
                when (result) {
                    is InstallResult.Success -> {
                        android.util.Log.d("InstallationViewModel", "Installation successful: ${result.message}")
                        successMessage = result.message
                        showSuccessDialog = true
                        installationProgress = null
                    }
                    is InstallResult.Error -> {
                        // Save detailed log to file
                        val savedLogPath = logSaver.saveInstallationLog(
                            packageName = currentApkInfo.packageName,
                            errorMessage = result.message,
                            detailedLog = result.detailedLog
                        )
                        
                        errorMessage = result.message
                        detailedLog = result.detailedLog
                        logFilePath = savedLogPath
                        showErrorDialog = true
                        installationProgress = null
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Installation failed: ${e.message}"
                val detailedLogContent = """
                    Exception occurred during installation:
                    Error: ${e.message}
                    Stack trace: ${e.stackTraceToString()}
                    
                    APK Information:
                    Package: ${currentApkInfo.packageName}
                    Version: ${currentApkInfo.versionName} (${currentApkInfo.versionCode})
                    File: ${currentApkInfo.file.absolutePath}
                    Size: ${currentApkInfo.fileSize} bytes
                    Is Update: ${currentApkInfo.isUpdate}
                """.trimIndent()
                
                // Save detailed log to file
                val savedLogPath = logSaver.saveInstallationLog(
                    packageName = currentApkInfo.packageName,
                    errorMessage = errorMsg,
                    detailedLog = detailedLogContent
                )
                
                errorMessage = errorMsg
                detailedLog = detailedLogContent
                logFilePath = savedLogPath
                showErrorDialog = true
                installationProgress = null
            } finally {
                isInstalling = false
            }
        }
    }
    
    fun uninstallExistingApp() {
        val packageName = apkInfo?.packageName ?: return
        
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
                        apkInfo?.let { currentApkInfo ->
                            val refreshedInfo = apkParser.parseApk(currentApkInfo.file)
                            apkInfo = refreshedInfo
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
                        showErrorDialog = true
                        installationProgress = null
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Uninstallation failed: ${e.message}"
                showErrorDialog = true
                installationProgress = null
            } finally {
                isInstalling = false
            }
        }
    }
    
    fun isDowngrade(apkInfo: ApkInfo): Boolean {
        return apkInfo.isUpdate && 
               apkInfo.existingPackageInfo != null && 
               apkInfo.versionCode < apkInfo.existingPackageInfo.longVersionCode
    }
    
    private fun checkRootAccess() {
        viewModelScope.launch {
            isRootAvailable = rootInstaller.checkRootAccess()
        }
    }
    
    fun showSuccessDialog(message: String) {
        successMessage = message
        showSuccessDialog = true
    }
    
    fun showErrorDialog(message: String, log: String) {
        errorMessage = message
        detailedLog = log
        showErrorDialog = true
    }
    
    fun dismissSuccessDialog() {
        showSuccessDialog = false
        successMessage = null
    }
    
    fun dismissErrorDialog() {
        showErrorDialog = false
        errorMessage = null
        detailedLog = null
        logFilePath = null
    }
}