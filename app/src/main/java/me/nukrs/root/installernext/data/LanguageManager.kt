package me.nukrs.root.installernext.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale
import me.nukrs.root.installernext.data.Language as AppLanguage

class LanguageManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_LANGUAGE = "selected_language"
    }
    
    fun getCurrentLanguage(): AppLanguage {
        val languageCode = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM_DEFAULT.code)
        return AppLanguage.values().find { it.code == languageCode } ?: AppLanguage.SYSTEM_DEFAULT
    }
    
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        applyLanguage(language)
    }
    
    fun applyLanguage(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.SYSTEM_DEFAULT -> Locale.getDefault()
            AppLanguage.CHINESE -> Locale.CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}