package com.edu.maktab.manager

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.edu.maktab.helper.AppConstant.LANGUAGE_KEY
import com.edu.maktab.helper.PreferenceHelper
import com.edu.maktab.helper.PreferenceHelper.set
import java.util.*

/**
 * Created by Furqan on 01-06-2018.
 */
class LocaleManager {
    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_URDU = "ur"
        fun setLocale(c: Context): Context {
            return updateResources(c, getLanguage(c))
        }

        fun setNewLocale(c: Context, language: String): Context {
            persistLanguage(c, language)
            return updateResources(c, language)
        }

        private fun updateResources(context: Context, language: String): Context {
            var context = context
            val locale = Locale(language)
            Locale.setDefault(locale)
            val res = context.resources
            val config = Configuration(res.configuration)
            config.setLocale(locale)
            context = context.createConfigurationContext(config)

            return context
        }

        fun getLanguage(c: Context): String {
            val prefs =
                PreferenceHelper.defaultPrefs(c)//PreferenceManager.getDefaultSharedPreferences(c)
            return prefs.getString(LANGUAGE_KEY, LANGUAGE_URDU)!!
        }

        private fun persistLanguage(c: Context, language: String) {
            val prefs = PreferenceHelper.defaultPrefs(c)
            prefs[LANGUAGE_KEY] = language
        }

        fun getLocale(res: Resources): Locale {
            val config = res.configuration
            return if (Build.VERSION.SDK_INT >= 24) config.locales.get(0) else config.locale
        }
    }
}