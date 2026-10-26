package pt.aadf.sourcesswitcher

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class SwitchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AppLocale.localized(applicationContext)
        val type = intent?.getStringExtra("input")?.let { runCatching { Safe.inputType(it) }.getOrNull() }
        if (type == null) {
            Toast.makeText(app, R.string.unknown_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(app, app.getString(R.string.switching_to, labelFor(app, type) ?: type), Toast.LENGTH_SHORT).show()
        Thread { reportFailure(app, sendSwitch(app, type)) }.start()
        finish()
    }

    private fun sendSwitch(app: Context, type: String): String? = try {
        val prefs = Prefs(app)
        var endpoint = prefs.endpointId
        var device = prefs.deviceId
        if (endpoint == null) {
            val cfg = InputRepository(app).load()
            prefs.saveEndpoint(cfg)
            endpoint = cfg.endpointId
            device = cfg.deviceId
        }
        Switcher.switchTo(type, endpoint!!, device ?: "")
    } catch (e: Exception) {
        e.message ?: e.toString()
    }

    private fun reportFailure(app: Context, error: String?) {
        if (error == null) return
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(app, app.getString(R.string.switch_failed, error), Toast.LENGTH_LONG).show()
        }
    }

    private fun labelFor(ctx: Context, type: String): String? {
        val id = ctx.resources.getIdentifier("input_" + type.lowercase(), "string", packageName)
        return if (id != 0) ctx.getString(id) else null
    }
}
