//不用了孩子们，喜欢的话自己pr下把
package me.nukrs.root.installernext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import me.nukrs.root.installernext.R
import me.nukrs.root.installernext.data.ApkInfo
import me.nukrs.root.installernext.ui.components.DetailInfoDialog
import me.nukrs.root.installernext.utils.InstallationStatus
import me.nukrs.root.installernext.utils.SignatureInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningSection(apkInfo: ApkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.apk_unexpected_situation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.signature_changed_or_downgrade),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun isDowngrade(apkInfo: ApkInfo): Boolean {
    return apkInfo.isUpdate && 
           apkInfo.existingPackageInfo != null && 
           apkInfo.versionCode < apkInfo.existingPackageInfo.longVersionCode
}

@Composable
fun InstallationDialog(
    apkInfo: ApkInfo,
    signatureInfo: SignatureInfo?,
    installationStatus: InstallationStatus?,
    isInstalling: Boolean,
    installationProgress: String?,
    isRootAvailable: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    var showAdvancedInfo by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailDialogTitle by remember { mutableStateOf("") }
    var detailDialogContent by remember { mutableStateOf("") }
    var detailDialogIcon by remember { mutableStateOf(Icons.Default.Info) }
    
    Dialog(
        onDismissRequest = { if (!isInstalling) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isInstalling,
            dismissOnClickOutside = !isInstalling
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.install_app),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isInstalling) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // App Basic Info
                    AppInfoSection(apkInfo = apkInfo)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Installation Status
                    installationStatus?.let { status ->
                        InstallationStatusSection(status = status)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Technical Details
                    TechnicalDetailsSection(
                        apkInfo = apkInfo,
                        signatureInfo = signatureInfo,
                        showAdvanced = showAdvancedInfo,
                        onToggleAdvanced = { showAdvancedInfo = !showAdvancedInfo },
                        onDetailClick = { title, content, icon ->
                            detailDialogTitle = title
                            detailDialogContent = content
                            detailDialogIcon = icon
                            showDetailDialog = true
                        }
                    )
                    
                    // Warning section for signature changes or downgrades
                    if (apkInfo.isUpdate && (apkInfo.isSignatureChanged || isDowngrade(apkInfo))) {
                        Spacer(modifier = Modifier.height(16.dp))
                        WarningSection(apkInfo = apkInfo)
                    }
                    
                    // Installation Progress or Success Message
                    if (installationProgress != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        InstallationProgressSection(
                            progress = installationProgress,
                            isInstalling = isInstalling
                        )
                    }
                }
                
                // Action Buttons
                if (!isInstalling) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ActionButtonsSection(
                        apkInfo = apkInfo,
                        installationStatus = installationStatus,
                        isRootAvailable = isRootAvailable,
                        onInstall = onInstall,
                        onUninstall = onUninstall,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
    
    // Detail Info Dialog
    if (showDetailDialog) {
        DetailInfoDialog(
            title = detailDialogTitle,
            content = detailDialogContent,
            icon = detailDialogIcon,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
fun AppInfoSection(apkInfo: ApkInfo) {
    val context = LocalContext.current
    
    // Load app icon outside of composable
    val appIcon = remember(apkInfo.packageName) {
        try {
            val packageManager = context.packageManager
            packageManager.getApplicationIcon(apkInfo.packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // App icon and basic info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap(
                            width = 120,
                            height = 120,
                            config = android.graphics.Bitmap.Config.ARGB_8888
                        ).asImageBitmap(),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    // Fallback icon if app icon cannot be loaded
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Default App Icon",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // App name and package info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = apkInfo.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = apkInfo.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    label = stringResource(R.string.version),
                    value = "${apkInfo.versionName} (${apkInfo.versionCode})"
                )
                
                InfoChip(
                    label = stringResource(R.string.size),
                    value = apkInfo.getFormattedSize()
                )
            }
            
            if (apkInfo.isUpdate) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.update_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (apkInfo.getFormattedSizeChange() != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${apkInfo.getFormattedSizeChange()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstallationStatusSection(status: InstallationStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.canInstall) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (status.canInstall) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (status.canInstall) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (status.canInstall) {
                        stringResource(R.string.ready_to_install)
                    } else {
                        stringResource(R.string.installation_blocked)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (status.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                status.issues.forEach { issue ->
                    Text(
                        text = "• $issue",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
            
            if (status.requiresRoot) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.requires_root),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun TechnicalDetailsSection(
    apkInfo: ApkInfo,
    signatureInfo: SignatureInfo?,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    onDetailClick: (String, String, ImageVector) -> Unit
) {
    // Pre-fetch all string resources that will be used in onClick callbacks
    val memoryDetailTitle = stringResource(R.string.memory_detail_title)
    val memoryDetailContent = stringResource(R.string.memory_detail_content, 
        apkInfo.getMemoryInfo().map { "${it.key}: ${it.value}" }.joinToString("\n"))
    
    val sdkDetailTitle = stringResource(R.string.sdk_detail_title)
    val sdkDetailContent = stringResource(R.string.sdk_detail_content,
        apkInfo.targetSdkVersion,
        apkInfo.minSdkVersion,
        apkInfo.compileSdkVersion ?: 0)
    
    val architectureDetailTitle = stringResource(R.string.architecture_detail_title)
    val architectureDetailContent = stringResource(R.string.architecture_detail_content,
        apkInfo.supportedAbis.joinToString("\n• ", "• "))
    
    val permissionDetailTitle = stringResource(R.string.permission_detail_title)
    val permissionDetails = buildString {
        if (apkInfo.dangerousPermissions.isNotEmpty()) {
            appendLine("Dangerous Permissions:")
            apkInfo.dangerousPermissions.forEach { permission ->
                appendLine("• ${permission.removePrefix("android.permission.")}")
            }
            appendLine()
        }
        if (apkInfo.normalPermissions.isNotEmpty()) {
            appendLine("Normal Permissions:")
            apkInfo.normalPermissions.forEach { permission ->
                appendLine("• ${permission.removePrefix("android.permission.")}")
            }
        }
    }
    val permissionDetailContent = stringResource(R.string.permission_detail_content, permissionDetails)
    
    val fileHashDetailTitle = stringResource(R.string.hash_detail_title)
    val fileHashDetailContent = stringResource(R.string.hash_detail_content, apkInfo.getFileHash())
    
    val certificateIssuerDetailTitle = stringResource(R.string.certificate_issuer_detail_title)
    val certificateIssuerDetailContent = signatureInfo?.let { sig ->
        stringResource(R.string.certificate_issuer_detail_content, sig.issuer, sig.algorithm)
    } ?: ""
    
    val certificateValidityDetailTitle = stringResource(R.string.certificate_validity_detail_title)
    val certificateValidityDetailContent = signatureInfo?.let { sig ->
        stringResource(R.string.certificate_validity_detail_content, "${sig.validFrom} - ${sig.validTo}")
    } ?: ""
    
    val filePathDetailTitle = stringResource(R.string.file_path_detail_title)
    val filePathDetailContent = stringResource(R.string.file_path_detail_content,
        apkInfo.getRealApkPath(),
        apkInfo.getFileName(),
        apkInfo.getFileCreationTime())
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Basic Technical Info
            val supportedAbisLabel = stringResource(R.string.supported_abis)
            DetailRow(
                icon = Icons.Default.Settings,
                label = supportedAbisLabel,
                value = apkInfo.supportedAbis.joinToString(", "),
                onClick = {
                    onDetailClick(
                        supportedAbisLabel,
                        apkInfo.supportedAbis.joinToString("\n"),
                        Icons.Default.Settings
                    )
                }
            )
            
            DetailRow(
                icon = Icons.Default.Build,
                label = stringResource(R.string.target_sdk),
                value = apkInfo.targetSdkVersion.toString()
            )
            
            DetailRow(
                icon = Icons.Default.Build,
                label = stringResource(R.string.compile_sdk),
                value = apkInfo.compileSdkVersion?.toString() ?: "Unknown"
            )
            
            DetailRow(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.min_sdk),
                value = apkInfo.minSdkVersion.toString()
            )
            
            DetailRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.installer_package),
                value = apkInfo.getInstallerSource()
            )
            
            DetailRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.app_category),
                value = apkInfo.getAppCategory()
            )
            
            // Component Information
            val componentInfoLabel = stringResource(R.string.component_info)
            DetailRow(
                icon = Icons.Default.List,
                label = componentInfoLabel,
                value = "Activities: ${apkInfo.activityCount}, Services: ${apkInfo.serviceCount}, Receivers: ${apkInfo.receiverCount}, Providers: ${apkInfo.providerCount}",
                onClick = {
                    val componentDetails = buildString {
                        appendLine("Activities: ${apkInfo.activityCount}")
                        appendLine("Services: ${apkInfo.serviceCount}")
                        appendLine("Receivers: ${apkInfo.receiverCount}")
                        appendLine("Providers: ${apkInfo.providerCount}")
                    }
                    onDetailClick(
                        componentInfoLabel,
                        componentDetails,
                        Icons.Default.List
                    )
                }
            )
            
            // Memory Information
            val memoryInfo = apkInfo.getMemoryInfo()
            if (memoryInfo.isNotEmpty()) {
                val memoryInfoLabel = stringResource(R.string.memory_info_label)
                DetailRow(
                    icon = Icons.Default.Info,
                    label = memoryInfoLabel,
                    value = stringResource(
                        R.string.memory_info_format,
                        memoryInfo["Large Heap"] ?: "Unknown",
                        memoryInfo["Hardware Acceleration"] ?: "Unknown"
                    ),
                    onClick = {
                        onDetailClick(
                            memoryDetailTitle,
                            memoryDetailContent,
                            Icons.Default.Info
                        )
                    }
                )
            }
            
            // Page Count (estimated based on activities and components)
            val pageCount = apkInfo.activityCount + (apkInfo.serviceCount / 2) + (apkInfo.receiverCount / 4)
            DetailRow(
                icon = Icons.Default.List,
                label = stringResource(R.string.estimated_pages_label),
                value = stringResource(R.string.estimated_pages_format, pageCount)
            )
            
            // SDK Information
            val sdkVersionLabel = stringResource(R.string.sdk_version_label)
            DetailRow(
                icon = Icons.Default.Build,
                label = sdkVersionLabel,
                value = stringResource(
                    R.string.sdk_version_format,
                    apkInfo.targetSdkVersion,
                    apkInfo.minSdkVersion,
                    apkInfo.compileSdkVersion
                ),
                onClick = {
                    onDetailClick(
                        sdkDetailTitle,
                        sdkDetailContent,
                        Icons.Default.Build
                    )
                }
            )
            
            // Supported ABIs
            if (apkInfo.supportedAbis.isNotEmpty()) {
                val supportedArchLabel = stringResource(R.string.supported_architecture_label)
                DetailRow(
                    icon = Icons.Default.Settings,
                    label = supportedArchLabel,
                    value = apkInfo.supportedAbis.joinToString(", "),
                    onClick = {
                        onDetailClick(
                            architectureDetailTitle,
                            architectureDetailContent,
                            Icons.Default.Settings
                        )
                    }
                )
            }
            
            // Permission Summary
            val permissionCountLabel = stringResource(R.string.permission_count)
            DetailRow(
                icon = Icons.Default.Lock,
                label = permissionCountLabel,
                value = "${apkInfo.permissions.size} total (${apkInfo.dangerousPermissions.size} dangerous)",
                onClick = {
                    onDetailClick(
                        permissionDetailTitle,
                        permissionDetailContent,
                        Icons.Default.Lock
                    )
                }
            )
            
            // Signature Info
            signatureInfo?.let { sig ->
                DetailRow(
                    icon = Icons.Default.Lock,
                    label = stringResource(R.string.signature_algorithm),
                    value = sig.algorithm
                )
                
                if (apkInfo.isUpdate && apkInfo.isSignatureChanged) {
                    DetailRow(
                        icon = Icons.Default.Warning,
                        label = stringResource(R.string.signature_changed),
                        value = stringResource(R.string.yes),
                        isWarning = true
                    )
                }
            }
            
            // Advanced Info Toggle
            TextButton(
                onClick = onToggleAdvanced,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showAdvanced) {
                        stringResource(R.string.hide_advanced_info)
                    } else {
                        stringResource(R.string.show_advanced_info)
                    }
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            
            // Advanced Info
            if (showAdvanced) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Application Flags Section
                if (apkInfo.getAppFlags().isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.app_flags),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    apkInfo.getAppFlags().forEach { flag ->
                        Text(
                            text = "• $flag",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Security Information
                Text(
                    text = stringResource(R.string.security_info),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                val fileHashLabel = stringResource(R.string.file_hash)
                DetailRow(
                    icon = Icons.Default.Info,
                    label = fileHashLabel,
                    value = apkInfo.getFileHash().take(16) + "...",
                    onClick = {
                        onDetailClick(
                            fileHashDetailTitle,
                            fileHashDetailContent,
                            Icons.Default.Info
                        )
                    }
                )
                
                signatureInfo?.let { sig ->
                    val certificateIssuerLabel = stringResource(R.string.certificate_issuer)
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = certificateIssuerLabel,
                        value = sig.issuer,
                        onClick = {
                            onDetailClick(
                                certificateIssuerDetailTitle,
                                certificateIssuerDetailContent,
                                Icons.Default.Person
                            )
                        }
                    )
                    
                    val certificateValidityLabel = stringResource(R.string.certificate_validity)
                    DetailRow(
                        icon = Icons.Default.DateRange,
                        label = certificateValidityLabel,
                        value = "${sig.validFrom} - ${sig.validTo}",
                        onClick = {
                            onDetailClick(
                                certificateValidityDetailTitle,
                                certificateValidityDetailContent,
                                Icons.Default.DateRange
                            )
                        }
                    )
                }
                
                // System Information
                 apkInfo.processName?.let { processName ->
                     DetailRow(
                         icon = Icons.Default.Settings,
                         label = stringResource(R.string.process_name),
                         value = processName
                     )
                 }
                 
                 apkInfo.taskAffinity?.let { taskAffinity ->
                     DetailRow(
                         icon = Icons.Default.List,
                         label = stringResource(R.string.task_affinity),
                         value = taskAffinity
                     )
                 }
                 
                 apkInfo.dataDirectory?.let { dataDir ->
                     DetailRow(
                         icon = Icons.Default.Info,
                         label = stringResource(R.string.data_directory),
                         value = dataDir
                     )
                 }
                 
                 apkInfo.nativeLibraryDir?.let { nativeDir ->
                     DetailRow(
                         icon = Icons.Default.Build,
                         label = stringResource(R.string.native_library_dir),
                         value = nativeDir
                     )
                 }
                 
                 apkInfo.sharedUserId?.let { sharedUserId ->
                     DetailRow(
                         icon = Icons.Default.Person,
                         label = stringResource(R.string.shared_user_id),
                         value = sharedUserId
                     )
                 }
                
                // Time Information
                apkInfo.firstInstallTime?.let { time ->
                    DetailRow(
                        icon = Icons.Default.DateRange,
                        label = stringResource(R.string.first_install_time),
                        value = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time))
                    )
                }
                
                apkInfo.lastUpdateTime?.let { time ->
                    DetailRow(
                        icon = Icons.Default.Refresh,
                        label = stringResource(R.string.last_update_time),
                        value = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time))
                    )
                }
                
                // File Information
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.file_information),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                DetailRow(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.file_name),
                    value = apkInfo.getFileName()
                )
                
                DetailRow(
                    icon = Icons.Default.DateRange,
                    label = stringResource(R.string.file_modified),
                    value = apkInfo.getFileCreationTime()
                )
                
                val filePathLabel = stringResource(R.string.file_path)
                DetailRow(
                    icon = Icons.Default.Info,
                    label = filePathLabel,
                    value = apkInfo.getRealApkPath().takeLast(50) + if (apkInfo.getRealApkPath().length > 50) "..." else "",
                    onClick = {
                        onDetailClick(
                            filePathDetailTitle,
                            filePathDetailContent,
                            Icons.Default.Info
                        )
                    }
                )
                
                // Show path type for installed apps
                if (apkInfo.isUpdate && apkInfo.existingPackageInfo != null) {
                    val realPath = apkInfo.getRealApkPath()
                    val cachePath = apkInfo.getFilePath()
                    if (realPath != cachePath) {
                        DetailRow(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.path_type),
                            value = stringResource(R.string.real_apk_path_root)
                        )
                        DetailRow(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.cache_path),
                            value = cachePath.takeLast(50) + if (cachePath.length > 50) "..." else ""
                        )
                    }
                }
                
                // Device Compatibility
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.device_compatibility),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                apkInfo.getDeviceCompatibility().forEach { compatibility ->
                    val isCompatible = !compatibility.contains("Incompatible")
                    Text(
                        text = "• $compatibility",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompatible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }
                
                // Memory & Performance Information
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.memory_performance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                apkInfo.getMemoryInfo().forEach { (key, value) ->
                    DetailRow(
                        icon = Icons.Default.Build,
                        label = key,
                        value = value
                    )
                }
                
                // Enhanced Security Information
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.enhanced_security_info),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                apkInfo.getSecurityInfo().forEach { (key, value) ->
                    val isSecurityRisk = value.contains("Security Risk") || (key == "Debuggable" && value == "Yes (Security Risk)")
                    DetailRow(
                        icon = if (isSecurityRisk) Icons.Default.Warning else Icons.Default.Lock,
                        label = key,
                        value = value,
                        isWarning = isSecurityRisk
                    )
                }
                
                // Detailed Component Information
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.detailed_components),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                apkInfo.getDetailedComponentInfo().forEach { (component, count) ->
                    DetailRow(
                        icon = Icons.Default.List,
                        label = component,
                        value = count.toString()
                    )
                }
                
                // Version Information
                if (apkInfo.isUpdate) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.version_comparison),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    apkInfo.getVersionInfo().forEach { (key, value) ->
                        val isDowngrade = key == "Update Type" && value == "Downgrade"
                        DetailRow(
                            icon = if (isDowngrade) Icons.Default.Warning else Icons.Default.Info,
                            label = key,
                            value = value,
                            isWarning = isDowngrade
                        )
                    }
                }
                
                // Permission Categories
                if (apkInfo.permissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.permission_categories),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (apkInfo.dangerousPermissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.dangerous_permissions)} (${apkInfo.dangerousPermissions.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        apkInfo.dangerousPermissions.take(3).forEach { permission ->
                            Text(
                                text = "• ${permission.removePrefix("android.permission.")}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (apkInfo.dangerousPermissions.size > 3) {
                            Text(
                                text = "• and ${apkInfo.dangerousPermissions.size - 3} more...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    if (apkInfo.specialPermissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.special_permissions)} (${apkInfo.specialPermissions.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        apkInfo.specialPermissions.forEach { permission ->
                            Text(
                                text = "• ${permission.removePrefix("android.permission.")}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    if (apkInfo.signaturePermissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.signature_permissions)} (${apkInfo.signaturePermissions.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        apkInfo.signaturePermissions.forEach { permission ->
                            Text(
                                text = "• ${permission.removePrefix("android.permission.")}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                    }
                    
                    if (apkInfo.normalPermissions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.normal_permissions)} (${apkInfo.normalPermissions.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        apkInfo.normalPermissions.forEach { permission ->
                            Text(
                                text = "• ${permission.removePrefix("android.permission.")}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstallationProgressSection(progress: String, isInstalling: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInstalling) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isInstalling) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Show success icon for completed installation
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = progress,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isInstalling) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionButtonsSection(
    apkInfo: ApkInfo,
    installationStatus: InstallationStatus?,
    isRootAvailable: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit
) {
    // Check if this is a risky installation (signature change or downgrade)
    val isRiskyInstall = apkInfo.isUpdate && (apkInfo.isSignatureChanged || isDowngrade(apkInfo))
    
    Column {
        // Uninstall button (if app is already installed)
        if (apkInfo.isUpdate) {
            OutlinedButton(
                onClick = onUninstall,
                modifier = Modifier.fillMaxWidth(),
                enabled = isRootAvailable
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.uninstall_existing))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
            
            // Install button - red for risky installs, normal for safe installs
            if (isRiskyInstall && isRootAvailable) {
                // Red install button for risky installations
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.install))
                }
            } else {
                // Normal install button
                val canInstall = installationStatus?.canInstall == true || 
                                (installationStatus?.hasRiskyIssues == true && isRootAvailable)
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                    enabled = canInstall
                ) {
                    Text(stringResource(R.string.install))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}