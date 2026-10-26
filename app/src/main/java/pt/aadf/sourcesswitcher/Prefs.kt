package pt.aadf.sourcesswitcher

import android.content.Context

/** Which equipment-control input the launcher tile should switch to, plus the TV endpoint to target. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)

    var inputType: String?
        get() = sp.getString("input_type", null)
        set(v) = sp.edit().putString("input_type", v).apply()
    var inputLabel: String?
        get() = sp.getString("input_label", null)
        set(v) = sp.edit().putString("input_label", v).apply()
    var endpointId: String?
        get() = sp.getString("endpoint_id", null)
        set(v) = sp.edit().putString("endpoint_id", v).apply()
    var deviceId: String?
        get() = sp.getString("device_id", null)
        set(v) = sp.edit().putString("device_id", v).apply()

    fun save(input: InputRepository.Input, cfg: InputRepository.Config) {
        sp.edit()
            .putString("input_type", input.type)
            .putString("input_label", input.label)
            .putString("endpoint_id", cfg.endpointId)
            .putString("device_id", cfg.deviceId)
            .apply()
    }
}
