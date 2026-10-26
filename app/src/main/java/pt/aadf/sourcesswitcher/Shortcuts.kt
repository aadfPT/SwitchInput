package pt.aadf.sourcesswitcher

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

object Shortcuts {
    private const val STUB_PACKAGE_PREFIX = "pt.aadf.sourcesswitcher.input."

    fun packageFor(type: String) = STUB_PACKAGE_PREFIX + type.lowercase()

    private fun stubAsset(type: String) = "stubs/" + type.lowercase() + ".apk"

    fun exists(ctx: Context, type: String): Boolean =
        try { ctx.assets.open(stubAsset(type)).close(); true } catch (e: Exception) { false }

    fun isEnabled(ctx: Context, type: String): Boolean = try {
        ctx.packageManager.getPackageInfo(packageFor(type), 0); true
    } catch (e: PackageManager.NameNotFoundException) { false }

    fun setEnabled(ctx: Context, type: String, on: Boolean): String? =
        if (on) install(ctx, type) else uninstall(type)

    private fun install(ctx: Context, type: String): String? {
        val apk = copyStubWhereRootCanRead(ctx, type)
        val r = Root.run("pm install -r -t --user 0 '${apk.path}'")
        apk.delete()
        return failureText(r)
    }

    private fun uninstall(type: String): String? =
        failureText(Root.run("pm uninstall --user 0 ${packageFor(type)}"))

    private fun copyStubWhereRootCanRead(ctx: Context, type: String): File {
        val dir = File(ctx.filesDir, "stubs").apply { mkdirs() }
        val apk = File(dir, type.lowercase() + ".apk")
        ctx.assets.open(stubAsset(type)).use { input -> apk.outputStream().use { input.copyTo(it) } }
        apk.setReadable(true, false)
        dir.setExecutable(true, false)
        ctx.filesDir.setExecutable(true, false)
        return apk
    }

    private fun failureText(r: Root.Result): String? =
        if (r.ok && r.text.contains("Success")) null else r.text.trim().ifEmpty { r.stderr }
}
