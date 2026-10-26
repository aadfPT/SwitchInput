package pt.aadf.sourcesswitcher

import java.io.ByteArrayOutputStream

/** Minimal `su -c` runner. The stick runs Magisk, whose su accepts `su -c <cmd>`. */
object Root {
    class Result(val exitCode: Int, val stdout: ByteArray, val stderr: String) {
        val ok get() = exitCode == 0
        val text get() = String(stdout)
    }

    fun run(cmd: String): Result {
        val p = ProcessBuilder("su", "-c", cmd).start()
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
