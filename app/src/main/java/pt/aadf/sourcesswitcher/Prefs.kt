package pt.aadf.sourcesswitcher

import android.content.Context

/** Which equipment-control activity the launcher tile should trigger. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)
    var activityId: String?
        get() = sp.getString("activity_id", null)
        set(v) = sp.edit().putString("activity_id", v).apply()
    var activityLabel: String?
        get() = sp.getString("activity_label", null)
        set(v) = sp.edit().putString("activity_label", v).apply()
}
