package me.nukrs.root.installernext.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import me.nukrs.root.installernext.R
import me.nukrs.root.installernext.data.ApkInfo
import me.nukrs.root.installernext.ui.components.*
import me.nukrs.root.installernext.ui.theme.InstallerNEXTTheme
import me.nukrs.root.installernext.ui.activity.InstallationViewModel
import me.nukrs.root.installernext.utils.InstallationStatus
import me.nukrs.root.installernext.utils.SignatureInfo
import java.io.File

class InstallationActivity : ComponentActivity() {
    
    private val viewModel: InstallationViewModel by viewModels()
    
    companion object {
        private const val EXTRA_APK_PATH = "extra_apk_path"
        
        fun createIntent(context: Context, apkPath: String): Intent {
            return Intent(context, InstallationActivity::class.java).apply {
                putExtra(EXTRA_APK_PATH, apkPath)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Get APK path from intent
        val apkPath = intent.getStringExtra(EXTRA_APK_PATH)
        if (apkPath == null) {
            finish()
            return
        }
        
        // Initialize ViewModel with APK file
        viewModel.initializeWithApk(File(apkPath))
        
        setContent {
            InstallerNEXTTheme {
                InstallationScreen(
                    viewModel = viewModel,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallationScreen(
    viewModel: InstallationViewModel,
    onFinish: () -> Unit
) {
    var showAdvancedInfo by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailDialogTitle by remember { mutableStateOf("") }
    var detailDialogContent by remember { mutableStateOf("") }
    var detailDialogIcon by remember { mutableStateOf(Icons.Default.Info) }
    
    val context = LocalContext.current
    
    // Observe installation results
    LaunchedEffect(viewModel.installationResult) {
        viewModel.installationResult?.let { result ->
            when (result) {
                is me.nukrs.root.installernext.utils.InstallResult.Success -> {
                    // Show success dialog and finish
                    viewModel.showSuccessDialog(result.message)
                }
                is me.nukrs.root.installernext.utils.InstallResult.Error -> {
                    // Show error dialog
                    viewModel.showErrorDialog(result.message, result.detailedLog)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.install_app),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!viewModel.isInstalling) onFinish() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                viewModel.apkInfo?.let { apkInfo ->
                    // App Basic Info
                    AppInfoSection(apkInfo = apkInfo)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Installation Status
                    viewModel.installationStatus?.let { status ->
                        InstallationStatusSection(status = status)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Technical Details
                    TechnicalDetailsSection(
                        apkInfo = apkInfo,
                        signatureInfo = viewModel.signatureInfo,
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
                    if (apkInfo.isUpdate && (apkInfo.isSignatureChanged || viewModel.isDowngrade(apkInfo))) {
                        Spacer(modifier = Modifier.height(16.dp))
                        WarningSection(apkInfo = apkInfo)
                    }
                    
                    // Installation Progress
                    viewModel.installationProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(16.dp))
                        InstallationProgressSection(
                            progress = progress,
                            isInstalling = viewModel.isInstalling
                        )
                    }
                }
            }
            
            // Action Buttons
            if (!viewModel.isInstalling && viewModel.apkInfo != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ActionButtonsSection(
                    apkInfo = viewModel.apkInfo!!,
                    installationStatus = viewModel.installationStatus,
                    isRootAvailable = viewModel.isRootAvailable,
                    onInstall = { viewModel.installApk() },
                    onUninstall = { viewModel.uninstallExistingApp() },
                    onDismiss = onFinish
                )
            }
        }
    }
    
    // Detail Dialog
    if (showDetailDialog) {
        DetailInfoDialog(
            title = detailDialogTitle,
            content = detailDialogContent,
            icon = detailDialogIcon,
            onDismiss = { showDetailDialog = false }
        )
    }
    
    // Installation Success Dialog
    if (viewModel.showSuccessDialog && viewModel.successMessage != null) {
        InstallationSuccessDialog(
            packageName = viewModel.apkInfo?.packageName ?: "Unknown",
            successMessage = viewModel.successMessage!!,
            onDismiss = {
                viewModel.dismissSuccessDialog()
                onFinish()
            }
        )
    }
    
    // Installation Error Dialog
    if (viewModel.showErrorDialog && viewModel.errorMessage != null) {
        InstallationErrorDialog(
            packageName = viewModel.apkInfo?.packageName ?: "Unknown",
            errorMessage = viewModel.errorMessage!!,
            detailedLog = viewModel.detailedLog ?: "",
            logFilePath = viewModel.logFilePath,
            onDismiss = {
                viewModel.dismissErrorDialog()
                onFinish()
            }
        )
    }
}