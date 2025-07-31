package me.nukrs.root.installernext

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import me.nukrs.root.installernext.ui.activity.BaseActivity
import me.nukrs.root.installernext.ui.screen.MainScreen
import me.nukrs.root.installernext.ui.theme.InstallerNEXTTheme
import me.nukrs.root.installernext.ui.viewmodel.MainViewModel
import me.nukrs.root.installernext.utils.LanguageManager
import me.nukrs.root.installernext.utils.GlobalLanguageManager
import java.io.File
import java.io.FileOutputStream

class MainActivity : BaseActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var mainViewModel: MainViewModel
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate called")
        
        // Ensure GlobalLanguageManager is initialized
        GlobalLanguageManager.initialize(this)
        
        // Hide status bar using new API
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Initialize viewModel first
        mainViewModel = MainViewModel(applicationContext)
        
        setContent {
            InstallerNEXTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        onSelectFile = { openFilePicker() }
                    )
                }
            }
        }
        
        // Handle intent if app was opened with an APK file
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called with action: ${intent.action}, data: ${intent.data}")
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent called with action: ${intent.action}, data: ${intent.data}")
        
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                Log.d(TAG, "Handling ACTION_VIEW")
                intent.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
            "android.intent.action.INSTALL_PACKAGE" -> {
                Log.d(TAG, "Handling ACTION_INSTALL_PACKAGE - Installation intercepted!")
                // Intercept installation intent
                intent.data?.let { uri ->
                    handleInstallationIntent(uri)
                }
            }
            else -> {
                Log.d(TAG, "Handling other action: ${intent.action}")
                // Check if this is a package installation notification
                if (intent.getBooleanExtra("SHOW_INSTALLATION_DETAILS", false)) {
                    val appName = intent.getStringExtra("INSTALLED_APP_NAME")
                    val packageName = intent.getStringExtra("INSTALLED_PACKAGE_NAME")
                    handlePackageInstallationNotification(appName, packageName)
                }
            }
        }
    }
    
    private fun openFilePicker() {
        filePickerLauncher.launch("application/vnd.android.package-archive")
    }
    
    private fun handleSelectedFile(uri: Uri) {
        Log.d(TAG, "handleSelectedFile called with URI: $uri")
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // Create a temporary file
                val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.apk")
                val outputStream = FileOutputStream(tempFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "APK file copied to temp location: ${tempFile.absolutePath}")
                // Parse the APK file
                mainViewModel.selectApkFile(tempFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file", e)
            e.printStackTrace()
            // Handle error
        }
    }
    
    private fun handleInstallationIntent(uri: Uri) {
        Log.d(TAG, "handleInstallationIntent called with URI: $uri")
        // This method intercepts system installation intents
        // and redirects them to our custom installation dialog
        handleSelectedFile(uri)
    }
    
    private fun handlePackageInstallationNotification(appName: String?, packageName: String?) {
        Log.d(TAG, "handlePackageInstallationNotification called: $appName ($packageName)")
        // Show a notification or dialog about the installed package
        // For now, we'll just log it
        if (appName != null && packageName != null) {
            // You can show a toast or notification here
            Log.d(TAG, "Package installed: $appName ($packageName)")
        }
    }
}