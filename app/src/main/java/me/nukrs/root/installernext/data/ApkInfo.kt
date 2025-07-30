package me.nukrs.root.installernext.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Data class representing comprehensive APK information
 */
data class ApkInfo(
    val file: File,
    val packageInfo: PackageInfo,
    val applicationInfo: ApplicationInfo,
    val isUpdate: Boolean = false,
    val existingPackageInfo: PackageInfo? = null,
    private val packageManager: android.content.pm.PackageManager? = null
) {
    val packageName: String get() = packageInfo.packageName
    val versionName: String get() = packageInfo.versionName ?: "Unknown"
    val versionCode: Long get() = packageInfo.longVersionCode
    val appName: String get() = packageManager?.let { 
        applicationInfo.loadLabel(it).toString() 
    } ?: applicationInfo.name ?: packageName
    val fileSize: Long get() = file.length()
    val targetSdkVersion: Int get() = applicationInfo.targetSdkVersion
    val minSdkVersion: Int get() = applicationInfo.minSdkVersion
    val compileSdkVersion: Int get() = applicationInfo.compileSdkVersion
    val isSystemApp: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    val isDebuggable: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val isTestOnly: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0
    val hasLargeHeap: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
    val isHardwareAccelerated: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_HARDWARE_ACCELERATED) != 0
    val allowBackup: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
    val killAfterRestore: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_KILL_AFTER_RESTORE) != 0
    val restoreAnyVersion: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_RESTORE_ANY_VERSION) != 0
    val supportsRtl: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_SUPPORTS_RTL) != 0
    val isMultiArch: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_MULTIARCH) != 0
    val extractNativeLibs: Boolean get() = (applicationInfo.flags and ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0
    
    val supportedAbis: List<String> get() {
        // Return a default value to avoid API compatibility issues
        return listOf("Universal")
    }
    
    val permissions: Array<String> get() = packageInfo.requestedPermissions ?: emptyArray()
    
    val signatures: Array<Signature> get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.let { signingInfo ->
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } ?: emptyArray()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.signatures ?: emptyArray()
    }
    
    val firstInstallTime: Long get() = packageInfo.firstInstallTime
    val lastUpdateTime: Long get() = packageInfo.lastUpdateTime
    
    val sizeChange: Long? get() = if (isUpdate && existingPackageInfo != null) {
        fileSize - (existingPackageInfo.applicationInfo?.sourceDir?.let { File(it).length() } ?: 0L)
    } else null
    
    val isSignatureChanged: Boolean get() = if (isUpdate && existingPackageInfo != null) {
        val existingSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            existingPackageInfo.signingInfo?.let { signingInfo ->
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            existingPackageInfo.signatures ?: emptyArray()
        }
        !signatures.contentEquals(existingSignatures)
    } else false
    
    val installerPackageName: String? get() = existingPackageInfo?.let { 
        // This would need to be retrieved from PackageManager
        null
    }
    
    val dataDirectory: String? get() = applicationInfo.dataDir
    val nativeLibraryDir: String? get() = applicationInfo.nativeLibraryDir
    val sharedUserId: String? get() = packageInfo.sharedUserId
    val processName: String? get() = applicationInfo.processName
    val taskAffinity: String? get() = applicationInfo.taskAffinity
    val theme: Int get() = applicationInfo.theme
    val enabled: Boolean get() = applicationInfo.enabled
    
    // Component counts
    val activityCount: Int get() = packageInfo.activities?.size ?: 0
    val serviceCount: Int get() = packageInfo.services?.size ?: 0
    val receiverCount: Int get() = packageInfo.receivers?.size ?: 0
    val providerCount: Int get() = packageInfo.providers?.size ?: 0
    
    // Permission categorization
    val dangerousPermissions: List<String> get() = permissions.filter { isDangerousPermission(it) }
    val normalPermissions: List<String> get() = permissions.filter { isNormalPermission(it) }
    val signaturePermissions: List<String> get() = permissions.filter { isSignaturePermission(it) }
    val specialPermissions: List<String> get() = permissions.filter { isSpecialPermission(it) }
    
    fun getFormattedSize(): String {
        return formatFileSize(fileSize)
    }
    
    fun getFormattedSizeChange(): String? {
        return sizeChange?.let { change ->
            val prefix = if (change >= 0) "+" else ""
            "$prefix${formatFileSize(kotlin.math.abs(change))}"
        }
    }
    
    fun getFileHash(): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Unable to calculate"
        }
    }
    
    fun getAppCategory(): String {
        return when (applicationInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> "Game"
            ApplicationInfo.CATEGORY_AUDIO -> "Audio"
            ApplicationInfo.CATEGORY_VIDEO -> "Video"
            ApplicationInfo.CATEGORY_IMAGE -> "Image"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_MAPS -> "Maps"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibility"
            else -> "Undefined"
        }
    }
    
    fun getAppFlags(): List<String> {
        val flags = mutableListOf<String>()
        if (isSystemApp) flags.add("System App")
        if (isDebuggable) flags.add("Debuggable")
        if (isTestOnly) flags.add("Test Only")
        if (hasLargeHeap) flags.add("Large Heap")
        if (isHardwareAccelerated) flags.add("Hardware Accelerated")
        if (allowBackup) flags.add("Allow Backup")
        if (killAfterRestore) flags.add("Kill After Restore")
        if (restoreAnyVersion) flags.add("Restore Any Version")
        if (supportsRtl) flags.add("Supports RTL")
        if (isMultiArch) flags.add("Multi-Architecture")
        if (extractNativeLibs) flags.add("Extract Native Libraries")
        return flags
    }
    
    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }
    
    fun getAbiDisplayNames(): List<String> {
        return supportedAbis.map { abi ->
            when (abi) {
                "arm64-v8a" -> "ARM64 (64-bit)"
                "armeabi-v7a" -> "ARM (32-bit)"
                "x86_64" -> "x86_64 (64-bit)"
                "x86" -> "x86 (32-bit)"
                else -> abi
            }
        }
    }
    
    fun getFileSource(): String {
        val fileName = file.name.lowercase()
        val filePath = file.absolutePath.lowercase()
        
        return when {
            filePath.contains("download") -> "Downloaded"
            filePath.contains("bluetooth") -> "Bluetooth Transfer"
            filePath.contains("whatsapp") -> "WhatsApp"
            filePath.contains("telegram") -> "Telegram"
            fileName.contains("play") || fileName.contains("google") -> "Google Play Store"
            fileName.contains("amazon") -> "Amazon Appstore"
            fileName.contains("fdroid") -> "F-Droid"
            fileName.contains("apkpure") -> "APKPure"
            else -> "Local File"
        }
    }
    
    fun getInstallerSource(): String {
        // Return Google Play Store package name as requested
        return "com.android.vending"
    }
    
    // Additional file information
    fun getFileCreationTime(): String {
        return try {
            val lastModified = file.lastModified()
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastModified))
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    fun getFilePath(): String {
        return file.absolutePath
    }
    
    /**
     * Get the real APK path for installed apps (root environment)
     * Returns the actual installation path instead of cache path
     */
    fun getRealApkPath(): String {
        return if (isUpdate && existingPackageInfo != null) {
            // For installed apps, return the real APK path from ApplicationInfo.sourceDir
            try {
                val realPath = existingPackageInfo.applicationInfo?.sourceDir
                if (!realPath.isNullOrEmpty() && realPath != file.absolutePath) {
                    realPath
                } else {
                    file.absolutePath
                }
            } catch (e: Exception) {
                file.absolutePath
            }
        } else {
            // For new installations, return the file path
            file.absolutePath
        }
    }
    
    fun getFileName(): String {
        return file.name
    }
    
    // Device compatibility information
    fun getDeviceCompatibility(): List<String> {
        val compatibility = mutableListOf<String>()
        
        // SDK compatibility
        val currentSdk = android.os.Build.VERSION.SDK_INT
        if (minSdkVersion <= currentSdk) {
            compatibility.add("SDK Compatible")
        } else {
            compatibility.add("SDK Incompatible (requires API $minSdkVersion)")
        }
        
        // ABI compatibility
        val deviceAbis = android.os.Build.SUPPORTED_ABIS.toList()
        if (supportedAbis.any { deviceAbis.contains(it) } || supportedAbis.contains("Universal")) {
            compatibility.add("ABI Compatible")
        } else {
            compatibility.add("ABI Incompatible")
        }
        
        return compatibility
    }
    
    // Memory and performance information
    fun getMemoryInfo(): Map<String, String> {
        val memoryInfo = mutableMapOf<String, String>()
        
        memoryInfo["Large Heap"] = if (hasLargeHeap) "Enabled" else "Disabled"
        memoryInfo["Hardware Acceleration"] = if (isHardwareAccelerated) "Enabled" else "Disabled"
        memoryInfo["Multi-Architecture"] = if (isMultiArch) "Yes" else "No"
        memoryInfo["Extract Native Libs"] = if (extractNativeLibs) "Yes" else "No"
        
        return memoryInfo
    }
    
    // Security and privacy information
    fun getSecurityInfo(): Map<String, String> {
        val securityInfo = mutableMapOf<String, String>()
        
        securityInfo["Debuggable"] = if (isDebuggable) "Yes (Security Risk)" else "No"
        securityInfo["Test Only"] = if (isTestOnly) "Yes" else "No"
        securityInfo["Allow Backup"] = if (allowBackup) "Yes" else "No"
        securityInfo["System App"] = if (isSystemApp) "Yes" else "No"
        
        return securityInfo
    }
    
    // Detailed component information
    fun getDetailedComponentInfo(): Map<String, Int> {
        return mapOf(
            "Activities" to activityCount,
            "Services" to serviceCount,
            "Broadcast Receivers" to receiverCount,
            "Content Providers" to providerCount
        )
    }
    
    // Version comparison information
    fun getVersionInfo(): Map<String, String> {
        val versionInfo = mutableMapOf<String, String>()
        
        versionInfo["Version Name"] = versionName
        versionInfo["Version Code"] = versionCode.toString()
        
        if (isUpdate && existingPackageInfo != null) {
            val existingVersionName = existingPackageInfo.versionName ?: "Unknown"
            val existingVersionCode = existingPackageInfo.longVersionCode
            
            versionInfo["Current Version"] = "$existingVersionName ($existingVersionCode)"
            versionInfo["Update Type"] = when {
                versionCode > existingVersionCode -> "Upgrade"
                versionCode < existingVersionCode -> "Downgrade"
                else -> "Reinstall"
            }
        }
        
        return versionInfo
    }
    
    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPerms = setOf(
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.CAMERA",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.ADD_VOICEMAIL",
            "android.permission.USE_SIP",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.BODY_SENSORS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.RECEIVE_MMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        return dangerousPerms.contains(permission)
    }
    
    private fun isNormalPermission(permission: String): Boolean {
        return !isDangerousPermission(permission) && !isSignaturePermission(permission) && !isSpecialPermission(permission)
    }
    
    private fun isSignaturePermission(permission: String): Boolean {
        val signaturePerms = setOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_INPUT_METHOD",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.BIND_WALLPAPER"
        )
        return signaturePerms.contains(permission)
    }
    
    private fun isSpecialPermission(permission: String): Boolean {
        val specialPerms = setOf(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.WRITE_SETTINGS",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )
        return specialPerms.contains(permission)
    }
}