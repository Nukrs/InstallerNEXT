package me.nukrs.root.installernext.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.nukrs.root.installernext.R
import me.nukrs.root.installernext.ui.components.ConfirmationDialog
import me.nukrs.root.installernext.ui.components.InstallationDialog
import me.nukrs.root.installernext.ui.components.InstallationErrorDialog
import me.nukrs.root.installernext.ui.components.InstallationSuccessDialog
import me.nukrs.root.installernext.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSelectFile: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Root Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isRootAvailable) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (viewModel.isRootAvailable) Icons.Default.Warning else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (viewModel.isRootAvailable) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.root_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (viewModel.isRootAvailable) {
                            stringResource(R.string.root_available)
                        } else {
                            stringResource(R.string.root_not_available)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (!viewModel.isRootAvailable) {
                    Button(
                        onClick = { viewModel.requestRootAccess() }
                    ) {
                        Text(stringResource(R.string.request_root))
                    }
                }
            }
        }
        
        // File Selection Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.select_apk_file),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = stringResource(R.string.select_apk_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.browse_files))
                }
            }
        }
        
        // Open Source and More Info Section
        Spacer(modifier = Modifier.height(16.dp))
        
        val uriHandler = LocalUriHandler.current
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.open_source_info),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Project description
                Text(
                    text = stringResource(R.string.project_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Features section
                Text(
                    text = stringResource(R.string.features_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Feature list
                val features = listOf(
                    stringResource(R.string.feature_root_install),
                    stringResource(R.string.feature_signature_bypass),
                    stringResource(R.string.feature_downgrade_support),
                    stringResource(R.string.feature_detailed_info)
                )
                
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // GitHub link button
                val githubUrl = stringResource(R.string.github_url)
                OutlinedButton(
                    onClick = {
                        uriHandler.openUri(githubUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.visit_github),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Important notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.important_notice),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.xposed_module_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Loading indicator
        if (viewModel.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.parsing_apk),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Error message
        viewModel.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
    
    // Installation Dialog
    if (viewModel.showInstallDialog && viewModel.selectedApkInfo != null) {
        InstallationDialog(
            apkInfo = viewModel.selectedApkInfo!!,
            signatureInfo = viewModel.signatureInfo,
            installationStatus = viewModel.installationStatus,
            isInstalling = viewModel.isInstalling,
            installationProgress = viewModel.installationProgress,
            isRootAvailable = viewModel.isRootAvailable,
            onDismiss = { viewModel.dismissInstallDialog() },
            onInstall = { viewModel.installApk() },
            onUninstall = { viewModel.uninstallExistingApp() }
        )
    }
    
    // Confirmation Dialog for risky installations
    if (viewModel.showConfirmationDialog && viewModel.selectedApkInfo != null) {
        val apkInfo = viewModel.selectedApkInfo!!
        val riskType = when {
            apkInfo.isSignatureChanged && viewModel.isDowngrade(apkInfo) -> stringResource(R.string.signature_change_and_downgrade)
            apkInfo.isSignatureChanged -> stringResource(R.string.signature_change_only)
            viewModel.isDowngrade(apkInfo) -> stringResource(R.string.version_downgrade_only)
            else -> stringResource(R.string.unknown_risk)
        }
        
        ConfirmationDialog(
            title = stringResource(R.string.apk_unexpected_situation),
            message = "${stringResource(R.string.signature_changed_or_downgrade)}: $riskType\n\n${stringResource(R.string.confirm_installation_warning)}",
            onConfirm = { viewModel.confirmInstallation() },
            onDismiss = { viewModel.dismissConfirmationDialog() }
        )
    }
    
    // Installation Error Dialog
    if (viewModel.showErrorDialog && viewModel.errorDialogData != null) {
        InstallationErrorDialog(
            packageName = viewModel.selectedApkInfo?.packageName ?: "Unknown",
            errorMessage = viewModel.errorDialogData!!.message,
            detailedLog = viewModel.errorDialogData!!.detailedLog,
            logFilePath = viewModel.logFilePath,
            onDismiss = { viewModel.dismissErrorDialog() }
        )
    }
    
    // Installation Success Dialog
    if (viewModel.showSuccessDialog && viewModel.successDialogData != null) {
        InstallationSuccessDialog(
            packageName = viewModel.selectedApkInfo?.packageName ?: "Unknown",
            successMessage = viewModel.successDialogData!!.message,
            onDismiss = { viewModel.dismissSuccessDialog() }
        )
    }
}