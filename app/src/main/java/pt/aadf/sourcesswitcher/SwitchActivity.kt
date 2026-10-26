package pt.aadf.sourcesswitcher

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Target of every launcher shortcut alias. It has no window (Theme.SourcesSwitcher.Invisible):
 * it works out which input its alias stands for, sends the switch and finishes immediately,
 * so the launcher that started it is back on screen while the TV changes input.
 */
class SwitchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext
        val type = Shortcuts.typeFromComponent(intent.component)
        val label = type?.let { t -> labelFor(t) } ?: type
        if (type == null) {
            Toast.makeText(app, R.string.unknown_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(app, getString(R.string.switching_to, label), Toast.LENGTH_SHORT).show()
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

    private fun labelFor(type: String): String? {
        val id = resources.getIdentifier("input_" + type.lowercase(), "string", packageName)
        return if (id != 0) getString(id) else null
    }
}
