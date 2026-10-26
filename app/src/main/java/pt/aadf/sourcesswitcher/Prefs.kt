package pt.aadf.sourcesswitcher

import android.content.Context

/**
 * Small persistent state: the TV endpoint every SelectInput directive must target (cached from
 * the device-control database by the main screen, so a shortcut can fire without reading the
 * database again) and the user's language choice.
 */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)

    var endpointId: String?
        get() = sp.getString("endpoint_id", null)
        set(v) = sp.edit().putString("endpoint_id", v).apply()
    var deviceId: String?
        get() = sp.getString("device_id", null)
        set(v) = sp.edit().putString("device_id", v).apply()

    /** BCP-47 tag of the language the user picked, or null to follow the system. */
    var languageTag: String?
        get() = sp.getString("language", null)
        set(v) = sp.edit().putString("language", v).apply()

    fun saveEndpoint(cfg: InputRepository.Config) {
        sp.edit().putString("endpoint_id", cfg.endpointId).putString("device_id", cfg.deviceId).apply()
    }
}
