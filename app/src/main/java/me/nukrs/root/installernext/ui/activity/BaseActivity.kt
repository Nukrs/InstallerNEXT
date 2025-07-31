package me.nukrs.root.installernext.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import me.nukrs.root.installernext.data.Language
import me.nukrs.root.installernext.utils.GlobalLanguageManager
import me.nukrs.root.installernext.utils.LanguageManager

/**
 * 为了穿死莱特搞的activity
 */
abstract class BaseActivity : ComponentActivity() {
    
    private lateinit var languageManager: LanguageManager
    
    override fun attachBaseContext(newBase: Context?) {
        val contextToUse = newBase?.let { context ->
            if (!GlobalLanguageManager.isInitialized()) {
                GlobalLanguageManager.initialize(context)
            }
            val savedLanguage = GlobalLanguageManager.getCurrentLanguage(context)
            GlobalLanguageManager.applyLanguage(context, savedLanguage)
        } ?: newBase
        
        super.attachBaseContext(contextToUse)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }

    protected fun getLanguageManager(): LanguageManager {
        if (!::languageManager.isInitialized) {
            languageManager = LanguageManager(this)
        }
        return languageManager
    }
    
    /**
     * Change language for all activities
     */
    protected fun changeLanguageGlobally(language: Language) {
        GlobalLanguageManager.changeLanguage(this, language)
    }
}