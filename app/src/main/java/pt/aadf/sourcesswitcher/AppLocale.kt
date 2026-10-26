package pt.aadf.sourcesswitcher

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocale {
    const val FOLLOW_SYSTEM = ""
    val CHOICES = listOf(FOLLOW_SYSTEM, "en", "de", "es", "fr", "it", "ja", "pt-PT", "pt-BR", "hi")

    fun displayName(ctx: Context, tag: String): String = when (tag) {
        FOLLOW_SYSTEM -> ctx.getString(R.string.language_system)
        "pt-PT" -> "Português (Portugal)"
        "pt-BR" -> "Português (Brasil)"
        else -> Locale.forLanguageTag(tag).let { it.getDisplayName(it).replaceFirstChar { c -> c.titlecase(it) } }
    }

    fun current(ctx: Context): String = Prefs(ctx).languageTag ?: FOLLOW_SYSTEM

    fun apply(ctx: Context, tag: String) {
        Prefs(ctx).languageTag = tag.ifEmpty { null }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun restore(ctx: Context) {
        val tag = current(ctx)
        if (tag.isNotEmpty() && AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    fun localized(ctx: Context): Context {
        val tag = current(ctx)
        if (tag.isEmpty()) return ctx
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocales(LocaleList.forLanguageTags(tag))
        return ctx.createConfigurationContext(cfg)
    }
}
