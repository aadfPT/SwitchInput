package pt.aadf.sourcesswitcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * The launcher tile. Translucent, no UI: send the configured switch and exit at once.
 * If nothing is configured yet, open the setup screen instead.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val type = prefs.inputType
        val endpoint = prefs.endpointId
        if (type == null || endpoint == null) {
            Toast.makeText(this, R.string.not_configured, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ConfigActivity::class.java))
            finish()
            return
        }
        Toast.makeText(this, getString(R.string.switching_to, prefs.inputLabel ?: type), Toast.LENGTH_SHORT).show()
        val app = applicationContext
        Thread {
            val err = try { Switcher.switchTo(type, endpoint, prefs.deviceId ?: "") } catch (e: Exception) { e.toString() }
            if (err != null) Handler(Looper.getMainLooper()).post {
                Toast.makeText(app, app.getString(R.string.switch_failed, err), Toast.LENGTH_LONG).show()
            }
        }.start()
        finish()
    }
}
