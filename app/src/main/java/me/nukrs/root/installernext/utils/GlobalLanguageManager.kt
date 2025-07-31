package me.nukrs.root.installernext.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import me.nukrs.root.installernext.data.Language
import java.lang.ref.WeakReference

/**
 * Global language manager that tracks all activities and can recreate them when language changes
 */
object GlobalLanguageManager : Application.ActivityLifecycleCallbacks {
    
    private const val TAG = "GlobalLanguageManager"
    private val activeActivities = mutableSetOf<WeakReference<Activity>>()
    private var languageManager: LanguageManager? = null
    private var isRegistered = false
    
    /**
     * Check if the global language manager is initialized
     */
    fun isInitialized(): Boolean {
        return languageManager != null
    }
    
    /**
     * Initialize the global language manager
     * This should be called from the Application class or the first Activity
     */
    fun initialize(context: Context) {
        Log.d(TAG, "initialize() called")
        if (languageManager == null) {
            languageManager = LanguageManager(context.applicationContext)
            Log.d(TAG, "LanguageManager created")

            if (!isRegistered) {
                val application = if (context is Activity) {
                    context.application
                } else {
                    context.applicationContext as? Application
                }
                application?.registerActivityLifecycleCallbacks(this)
                isRegistered = true
                Log.d(TAG, "ActivityLifecycleCallbacks registered")
            }
        }
    }
    
    /**
     * Change the language for all activities
     */
    fun changeLanguage(context: Context, language: Language) {
        Log.d(TAG, "changeLanguage() called with language: ${language.code}")
        initialize(context)

        languageManager?.let { manager ->
            val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_language", language.code).apply()
            Log.d(TAG, "Language preference saved: ${language.code}")
        }

        recreateAllActivities()
    }
    
    /**
     * Get current language
     */
    fun getCurrentLanguage(context: Context): Language {
        initialize(context)
        val currentLanguage = languageManager?.getCurrentLanguage() ?: Language.SYSTEM_DEFAULT
        Log.d(TAG, "getCurrentLanguage() returning: ${currentLanguage.code}")
        return currentLanguage
    }
    
    /**
     * Apply language to a context
     */
    fun applyLanguage(context: Context, language: Language): Context {
        Log.d(TAG, "applyLanguage() called with language: ${language.code}")
        initialize(context)
        return languageManager?.applyLanguage(context, language) ?: context
    }
    
    /**
     * Recreate all active activities to apply language changes
     */
    private fun recreateAllActivities() {
        val activitiesToRecreate = activeActivities.mapNotNull { it.get() }
        Log.d(TAG, "recreateAllActivities() - Found ${activitiesToRecreate.size} active activities")
        
        activitiesToRecreate.forEach { activity ->
            Log.d(TAG, "Recreating activity: ${activity.javaClass.simpleName}")
            activity.recreate()
        }
    }
    
    // ActivityLifecycleCallbacks implementation
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activeActivities.add(WeakReference(activity))
        Log.d(TAG, "onActivityCreated: ${activity.javaClass.simpleName}, total active: ${activeActivities.size}")
    }
    
    override fun onActivityStarted(activity: Activity) {}
    
    override fun onActivityResumed(activity: Activity) {}
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        // Remove destroyed activities from the set
        val sizeBefore = activeActivities.size
        activeActivities.removeAll { it.get() == null || it.get() == activity }
        Log.d(TAG, "onActivityDestroyed: ${activity.javaClass.simpleName}, active activities: $sizeBefore -> ${activeActivities.size}")
    }
}