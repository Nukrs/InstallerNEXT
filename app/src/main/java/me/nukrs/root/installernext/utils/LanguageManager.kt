package me.nukrs.root.installernext.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import me.nukrs.root.installernext.data.Language
import java.util.*

class LanguageManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getCurrentLanguage(): Language {
        val savedLanguage = prefs.getString(KEY_LANGUAGE, Language.SYSTEM_DEFAULT.code)
        return Language.values().find { it.code == savedLanguage } ?: Language.SYSTEM_DEFAULT
    }
    
    fun setLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
    
    fun applyLanguage(context: Context, language: Language): Context {
        val locale = when (language) {
            Language.CHINESE -> Locale("zh")
            Language.ENGLISH -> Locale("en")
            Language.SYSTEM_DEFAULT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
            }
        }
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return context.createConfigurationContext(config)
    }
}