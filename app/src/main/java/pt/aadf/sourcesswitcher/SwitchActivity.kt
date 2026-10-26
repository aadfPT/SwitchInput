package pt.aadf.sourcesswitcher

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Started by the per-input stub packages with an "input" extra (the Fire TV activity type).
 * It has no window (Theme.SourcesSwitcher.Invisible): it sends the switch and finishes
 * immediately, so the launcher that started the stub is back on screen while the TV changes input.
 */
class SwitchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Toast texts follow the language chosen in the app, not only the system language.
        val app = AppLocale.localized(applicationContext)
        val type = intent?.getStringExtra("input")?.takeIf { it.isNotBlank() }
        if (type == null) {
            Toast.makeText(app, R.string.unknown_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(app, app.getString(R.string.switching_to, labelFor(app, type) ?: type), Toast.LENGTH_SHORT).show()
        Thread {
            val err = try {
                val prefs = Prefs(app)
                var endpoint = prefs.endpointId
                var device = prefs.deviceId
                if (endpoint == null) {
                    // First shortcut used before the main screen ever ran: read the TV endpoint now.
                    val cfg = InputRepository(app).load()
                    prefs.saveEndpoint(cfg)
                    endpoint = cfg.endpointId
                    device = cfg.deviceId
                }
                Switcher.switchTo(type, endpoint!!, device ?: "")
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
            if (err != null) Handler(Looper.getMainLooper()).post {
                Toast.makeText(app, app.getString(R.string.switch_failed, err), Toast.LENGTH_LONG).show()
            }
        }.start()
        finish()
    }

    private fun labelFor(ctx: android.content.Context, type: String): String? {
        val id = ctx.resources.getIdentifier("input_" + type.lowercase(), "string", packageName)
        return if (id != 0) ctx.getString(id) else null
    }
}
