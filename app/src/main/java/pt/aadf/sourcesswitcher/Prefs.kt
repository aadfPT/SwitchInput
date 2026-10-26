package pt.aadf.sourcesswitcher

import android.content.Context

/**
 * The TV endpoint every SelectInput directive must target, cached from the device-control
 * database by the main screen so a shortcut can fire without reading the database again.
 */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)

    var endpointId: String?
        get() = sp.getString("endpoint_id", null)
        set(v) = sp.edit().putString("endpoint_id", v).apply()
    var deviceId: String?
        get() = sp.getString("device_id", null)
        set(v) = sp.edit().putString("device_id", v).apply()

    fun saveEndpoint(cfg: InputRepository.Config) {
        sp.edit().putString("endpoint_id", cfg.endpointId).putString("device_id", cfg.deviceId).apply()
    }
}
