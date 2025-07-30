package me.nukrs.root.installernext.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import me.nukrs.root.installernext.data.ApkInfo
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.io.ByteArrayInputStream

/**
 * Utility class for parsing APK files and extracting information
 */
class ApkParser(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    /**
     * Parse an APK file and extract comprehensive information
     */
    fun parseApk(file: File): ApkInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    file.absolutePath,
                    PackageInfoFlags.of(
                        (PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_SIGNING_CERTIFICATES or
                        PackageManager.GET_CONFIGURATIONS or
                        PackageManager.GET_INSTRUMENTATION).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(
                    file.absolutePath,
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_SIGNATURES or
                    PackageManager.GET_CONFIGURATIONS or
                    PackageManager.GET_INSTRUMENTATION
                )
            } ?: return null
            
            // Set the application info source directory for proper icon loading
            packageInfo.applicationInfo?.sourceDir = file.absolutePath
            packageInfo.applicationInfo?.publicSourceDir = file.absolutePath
            
            // Check if this is an update to an existing app
            val existingPackageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageInfo.packageName,
                        PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        packageInfo.packageName,
                        PackageManager.GET_SIGNATURES
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            
            val isUpdate = existingPackageInfo != null
            
            ApkInfo(
                file = file,
                packageInfo = packageInfo,
                applicationInfo = packageInfo.applicationInfo!!,
                isUpdate = isUpdate,
                existingPackageInfo = existingPackageInfo,
                packageManager = packageManager
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get detailed signature information
     */
    fun getSignatureInfo(apkInfo: ApkInfo): SignatureInfo {
        val signatures = apkInfo.signatures
        if (signatures.isEmpty()) {
            return SignatureInfo(
                hasSignature = false,
                algorithm = "None",
                issuer = "Unknown",
                subject = "Unknown",
                validFrom = null,
                validTo = null,
                serialNumber = "Unknown"
            )
        }
        
        return try {
            val signature = signatures[0]
            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate
            
            SignatureInfo(
                hasSignature = true,
                algorithm = cert.sigAlgName,
                issuer = cert.issuerDN.name,
                subject = cert.subjectDN.name,
                validFrom = cert.notBefore,
                validTo = cert.notAfter,
                serialNumber = cert.serialNumber.toString()
            )
        } catch (e: Exception) {
            SignatureInfo(
                hasSignature = true,
                algorithm = "Unknown",
                issuer = "Unknown",
                subject = "Unknown",
                validFrom = null,
                validTo = null,
                serialNumber = "Unknown"
            )
        }
    }
    
    /**
     * Check if the device supports the APK's ABIs
     */
    fun isAbiCompatible(apkInfo: ApkInfo): Boolean {
        val deviceAbis = android.os.Build.SUPPORTED_ABIS.toList()
        val apkAbis = apkInfo.supportedAbis
        
        // If APK has no specific ABI requirements, it's universal
        if (apkAbis.isEmpty() || apkAbis.contains("Universal")) {
            return true
        }
        
        // Check if any APK ABI is supported by the device
        return apkAbis.any { apkAbi -> deviceAbis.contains(apkAbi) }
    }
    
    /**
     * Get the installation status and potential issues
     */
    fun getInstallationStatus(apkInfo: ApkInfo): InstallationStatus {
        val issues = mutableListOf<String>()
        val riskyIssues = mutableListOf<String>()
        
        // Check ABI compatibility
        if (!isAbiCompatible(apkInfo)) {
            issues.add("Incompatible CPU architecture")
        }
        
        // Check SDK version compatibility
        if (apkInfo.minSdkVersion > android.os.Build.VERSION.SDK_INT) {
            issues.add("Requires Android API ${apkInfo.minSdkVersion} or higher")
        }
        
        // Check if it's a downgrade - this is a risky issue but allowed with root
        if (apkInfo.isUpdate && apkInfo.existingPackageInfo != null) {
            val existingVersionCode = apkInfo.existingPackageInfo.longVersionCode
            if (apkInfo.versionCode < existingVersionCode) {
                riskyIssues.add("Version downgrade detected")
            }
        }
        
        // Check signature compatibility for updates - this is a risky issue but allowed with root
        if (apkInfo.isUpdate && apkInfo.isSignatureChanged) {
            riskyIssues.add("Signature mismatch - may require uninstall first")
        }
        
        // Only non-risky issues prevent installation
        return InstallationStatus(
            canInstall = issues.isEmpty(),
            issues = issues + riskyIssues,
            requiresRoot = apkInfo.isSystemApp || issues.isNotEmpty() || riskyIssues.isNotEmpty(),
            hasRiskyIssues = riskyIssues.isNotEmpty()
        )
    }
}

/**
 * Data class for signature information
 */
data class SignatureInfo(
    val hasSignature: Boolean,
    val algorithm: String,
    val issuer: String,
    val subject: String,
    val validFrom: java.util.Date?,
    val validTo: java.util.Date?,
    val serialNumber: String
)

/**
 * Data class for installation status
 */
data class InstallationStatus(
    val canInstall: Boolean,
    val issues: List<String>,
    val requiresRoot: Boolean,
    val hasRiskyIssues: Boolean = false
)