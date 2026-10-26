package pt.aadf.sourcesswitcher

import java.io.ByteArrayOutputStream

object Root {
    private const val GLOBAL_MOUNT_NAMESPACE = "-mm"

    class Result(val exitCode: Int, val stdout: ByteArray, val stderr: String) {
        val ok get() = exitCode == 0
        val text get() = String(stdout)
    }

    fun run(cmd: String): Result {
        val p = ProcessBuilder("su", GLOBAL_MOUNT_NAMESPACE, "-c", cmd).start()
        val out = ByteArrayOutputStream()
        var err = ByteArray(0)
        val errReader = Thread { err = p.errorStream.readBytes() }
        errReader.start()
        p.inputStream.copyTo(out)
        val code = p.waitFor()
        errReader.join()
        return Result(code, out.toByteArray(), String(err))
    }

    fun available(): Boolean = try { run("id").text.contains("uid=0") } catch (e: Exception) { false }
}
