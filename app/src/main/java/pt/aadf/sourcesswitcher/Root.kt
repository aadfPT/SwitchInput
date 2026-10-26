package pt.aadf.sourcesswitcher

import java.io.ByteArrayOutputStream

/**
 * Minimal `su` runner. The stick runs Magisk; `-mm` (--mount-master) runs the command in the
 * global mount namespace. Without it, Android 11 app-data isolation hides other packages'
 * /data/data directories from the root shell spawned inside this app's namespace, so the
 * device-control database would be "No such file or directory" even as root.
 */
object Root {
    class Result(val exitCode: Int, val stdout: ByteArray, val stderr: String) {
        val ok get() = exitCode == 0
        val text get() = String(stdout)
    }

    fun run(cmd: String): Result {
        val p = ProcessBuilder("su", "-mm", "-c", cmd).start()
        val out = ByteArrayOutputStream()
        var err = ByteArray(0)
        val t = Thread { err = p.errorStream.readBytes() }
        t.start()
        p.inputStream.copyTo(out)
        val code = p.waitFor()
        t.join()
        return Result(code, out.toByteArray(), String(err))
    }

    fun available(): Boolean = try { run("id").text.contains("uid=0") } catch (e: Exception) { false }
}
