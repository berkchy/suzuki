package hime.suzuki.core

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

data class AppLanguage(val tag: String, val nativeName: String)

object AppLocale {

    private const val PREFS = "suzuki_locale"
    private const val KEY = "language_tag"

    val languages = listOf(
        AppLanguage("en", "English"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("fr", "Français"),
        AppLanguage("es", "Español"),
        AppLanguage("es-MX", "Español (México)"),
        AppLanguage("pt", "Português"),
        AppLanguage("pt-BR", "Português (Brasil)"),
        AppLanguage("it", "Italiano"),
        AppLanguage("ru", "Русский"),
        AppLanguage("uk", "Українська"),
        AppLanguage("ar", "العربية"),
        AppLanguage("fa", "فارسی"),
        AppLanguage("he", "עברית"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("ja", "日本語"),
        AppLanguage("ko", "한국어"),
        AppLanguage("zh-Hans", "简体中文"),
        AppLanguage("zh-Hant", "繁體中文"),
        AppLanguage("az", "Azərbaycanca"),
        AppLanguage("nl", "Nederlands"),
        AppLanguage("pl", "Polski"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("th", "ไทย"),
    )

    fun tag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()

    fun set(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, tag)
            .commit()
    }

    fun wrap(base: Context): Context {
        val tag = tag(base)
        if (tag.isBlank()) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun displayName(context: Context, tag: String): String {
        if (tag.isBlank()) return ""
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }
}
