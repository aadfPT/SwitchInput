package pt.aadf.sourcesswitcher

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Language handling. The app follows the system language unless the user picked one on the
 * main screen; the choice is stored both by AppCompat (which applies it to AppCompat
 * activities automatically) and in Prefs, so the windowless SwitchActivity, which is not an
 * AppCompat activity, can localise its toasts too.
 */
object AppLocale {
    /** BCP-47 tags offered in the picker; "" = follow the system. Order = menu order. */
    val CHOICES = listOf("", "en", "de", "es", "fr", "it", "ja", "pt-PT", "pt-BR", "hi")

    /** Each language in its own name, so the list is readable whatever is currently active. */
    fun displayName(ctx: Context, tag: String): String = when (tag) {
        "" -> ctx.getString(R.string.language_system)
        "pt-PT" -> "Português (Portugal)"
        "pt-BR" -> "Português (Brasil)"
        else -> Locale.forLanguageTag(tag).let { it.getDisplayName(it).replaceFirstChar { c -> c.titlecase(it) } }
    }

    fun current(ctx: Context): String = Prefs(ctx).languageTag ?: ""

    fun apply(ctx: Context, tag: String) {
        Prefs(ctx).languageTag = tag.ifEmpty { null }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Re-applies the stored choice on process start (AppCompat does it only for its own activities). */
    fun restore(ctx: Context) {
        val tag = current(ctx)
        if (tag.isNotEmpty() && AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    /** A context whose resources use the chosen language, for non-AppCompat code paths. */
    fun localized(ctx: Context): Context {
        val tag = current(ctx)
        if (tag.isEmpty()) return ctx
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocales(LocaleList.forLanguageTags(tag))
        return ctx.createConfigurationContext(cfg)
    }
}
