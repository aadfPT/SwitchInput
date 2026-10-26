package pt.aadf.sourcesswitcher

import android.content.Context
import android.content.pm.PackageManager

object Shortcuts {
    private const val STUB_PACKAGE_PREFIX = "pt.aadf.sourcesswitcher.input."

    fun packageFor(type: String) = STUB_PACKAGE_PREFIX + Safe.inputType(type).lowercase()

    private fun stubAsset(type: String) = "stubs/" + Safe.inputType(type).lowercase() + ".apk"

    fun exists(ctx: Context, type: String): Boolean =
        try { ctx.assets.open(stubAsset(type)).close(); true } catch (e: Exception) { false }

    fun isEnabled(ctx: Context, type: String): Boolean =
        signedLikeUs(ctx, packageFor(type))

    fun setEnabled(ctx: Context, type: String, on: Boolean): String? =
        if (on) install(ctx, type) else uninstall(ctx, type)

    private fun install(ctx: Context, type: String): String? {
        val apk = ctx.assets.open(stubAsset(type)).use { it.readBytes() }
        return failureText(Root.run("pm install -r -t --user 0 -S ${apk.size}", stdin = apk))
    }

    private fun uninstall(ctx: Context, type: String): String? {
        val pkg = packageFor(type)
        if (!signedLikeUs(ctx, pkg)) return "not a Switch Input shortcut"
        return failureText(Root.run("pm uninstall --user 0 $pkg"))
    }

    private fun signedLikeUs(ctx: Context, pkg: String): Boolean = try {
        ctx.packageManager.getPackageInfo(pkg, 0)
        ctx.packageManager.checkSignatures(ctx.packageName, pkg) == PackageManager.SIGNATURE_MATCH
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun failureText(r: Root.Result): String? =
        if (r.ok && r.text.contains("Success")) null else r.text.trim().ifEmpty { r.stderr }
}
