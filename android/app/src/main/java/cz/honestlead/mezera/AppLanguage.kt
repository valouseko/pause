package cz.honestlead.mezera

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cz.honestlead.mezera.data.Store
import java.util.Locale

object AppLanguage {
    fun initialize(context: Context) {
        val store = Store.get(context)
        if (Build.VERSION.SDK_INT >= 33) {
            // Initialize once. Later changes in Android's language settings also win.
            if (!store.languageInitialized) {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(store.language)
            }
        } else {
            // Custom storage must be restored before any AppCompat activity is created.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(store.language))
        }
        store.languageInitialized = true
    }

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            val locale = if (locales.isEmpty) context.resources.configuration.locales[0] else locales[0]
            return if (locale.language == "cs") "cs" else "en"
        }
        return Store.get(context).language
    }

    fun select(context: Context, language: String) {
        Store.get(context).language = language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        cz.honestlead.mezera.update.Notifier.ensureChannel(context)
    }

    // Workers use an application context; on older Android it does not inherit activity locales.
    fun localizedContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(current(context)))
        return context.createConfigurationContext(configuration)
    }
}
