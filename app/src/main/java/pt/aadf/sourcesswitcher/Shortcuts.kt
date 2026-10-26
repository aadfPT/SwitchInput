package pt.aadf.sourcesswitcher

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * A launcher shortcut is a separate tiny package per input (`pt.aadf.sourcesswitcher.input.<type>`),
 * built by tools/build_stubs.py and shipped in assets/stubs. Launchers such as Projectivy show one
 * card per package, so this is the only way an input shows up as its own TV app. Adding a
 * shortcut installs the stub as root, removing it uninstalls the package.
 */
object Shortcuts {
    private const val PKG_PREFIX = "pt.aadf.sourcesswitcher.input."

    fun packageFor(type: String) = PKG_PREFIX + type.lowercase()

    private fun assetName(type: String) = "stubs/" + type.lowercase() + ".apk"

    /** True when a stub was built for this input type. */
    fun exists(ctx: Context, type: String): Boolean =
        try { ctx.assets.open(assetName(type)).close(); true } catch (e: Exception) { false }

    fun isEnabled(ctx: Context, type: String): Boolean = try {
        ctx.packageManager.getPackageInfo(packageFor(type), 0); true
    } catch (e: PackageManager.NameNotFoundException) { false }

    /** Installs or uninstalls the stub. Blocking (1–3 s); returns null on success or an error text. */
    fun setEnabled(ctx: Context, type: String, on: Boolean): String? {
        val pkg = packageFor(type)
        if (!on) {
            val r = Root.run("pm uninstall --user 0 $pkg")
            return if (r.ok && r.text.contains("Success")) null else r.text.trim().ifEmpty { r.stderr }
        }
        // Copy the asset where root's pm can read it; app files are 0600, so make it world-readable.
        val dir = File(ctx.filesDir, "stubs").apply { mkdirs() }
        val apk = File(dir, type.lowercase() + ".apk")
        ctx.assets.open(assetName(type)).use { input -> apk.outputStream().use { input.copyTo(it) } }
        apk.setReadable(true, false)
        dir.setExecutable(true, false)
        ctx.filesDir.setExecutable(true, false)
        val r = Root.run("pm install -r -t --user 0 '${apk.path}'")
        apk.delete()
        return if (r.ok && r.text.contains("Success")) null else r.text.trim().ifEmpty { r.stderr }
    }
}
