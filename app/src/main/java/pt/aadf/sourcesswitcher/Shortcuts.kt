package pt.aadf.sourcesswitcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Launcher shortcuts are the `activity-alias` entries generated into the manifest, one per
 * Fire TV input type, named `Shortcut_<activityType>` and disabled by default. Enabling one
 * makes it show up in Projectivy / the Fire TV launcher like any other app; disabling removes it.
 */
object Shortcuts {
    private const val PREFIX = "Shortcut_"

    fun component(ctx: Context, type: String) = ComponentName(ctx, ctx.packageName + "." + PREFIX + type)

    /** The input type behind a launched alias, or null when the activity was started directly. */
    fun typeFromComponent(name: ComponentName?): String? =
        name?.className?.substringAfterLast('.')?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)

    fun exists(ctx: Context, type: String): Boolean = try {
        ctx.packageManager.getActivityInfo(component(ctx, type), PackageManager.MATCH_DISABLED_COMPONENTS)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isEnabled(ctx: Context, type: String): Boolean =
        ctx.packageManager.getComponentEnabledSetting(component(ctx, type)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setEnabled(ctx: Context, type: String, on: Boolean) {
        ctx.packageManager.setComponentEnabledSetting(
            component(ctx, type),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
