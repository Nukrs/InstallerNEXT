package me.nukrs.root.installernext.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import me.nukrs.root.installernext.MainActivity

class PackageInstallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PackageInstallReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        Log.d(TAG, "Received action: $action, data: $data")
        
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // New package installed
                    handlePackageInstalled(context, data)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Package updated
                handlePackageUpdated(context, data)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Package removed
                handlePackageRemoved(context, data)
            }
        }
    }
    
    private fun handlePackageInstalled(context: Context, data: Uri?) {
        data?.let { uri ->
            val packageName = uri.schemeSpecificPart
            Log.d(TAG, "Package installed: $packageName")
            
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                val appName = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: packageName
                
                Log.d(TAG, "Installed app: $appName ($packageName)")
                
                // Show notification or launch our app to show installation details
                showInstallationNotification(context, appName, packageName)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting package info for $packageName", e)
            }
        }
    }
    
    private fun handlePackageUpdated(context: Context, data: Uri?) {
        data?.let { uri ->
            val packageName = uri.schemeSpecificPart
            Log.d(TAG, "Package updated: $packageName")
        }
    }
    
    private fun handlePackageRemoved(context: Context, data: Uri?) {
        data?.let { uri ->
            val packageName = uri.schemeSpecificPart
            Log.d(TAG, "Package removed: $packageName")
        }
    }
    
    private fun showInstallationNotification(context: Context, appName: String, packageName: String) {
        // Launch our main activity to show installation details
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_INSTALLATION_DETAILS", true)
            putExtra("INSTALLED_APP_NAME", appName)
            putExtra("INSTALLED_PACKAGE_NAME", packageName)
        }
        
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching main activity", e)
        }
    }
}