package pt.aadf.sourcesswitcher

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File

/**
 * Reads the Fire TV Equipment Control configuration straight from the device-control
 * app's SQLite database (root required, the DB belongs to com.amazon.tv.devicecontrol).
 *
 * Table `activities` holds one row per selectable input ("activity"): HDMI_1..4, ANTENA,
 * plus user-named ones such as PLAY_STATION. `activityType` is the identifier Alexa's
 * SelectInput directive carries. Table `data_store` row METADATA_HASH holds the TV's
 * cloud endpoint id (`chrEndpointId`), which the directive must target.
 */
class InputRepository(private val ctx: Context) {

    data class Input(val id: String, val type: String, val label: String, val functions: List<String>)
    data class Config(val inputs: List<Input>, val endpointId: String, val deviceId: String)

    companion object {
        const val DCS_DB = "/data/data/com.amazon.tv.devicecontrol/databases/device_control_db"
    }

    fun load(): Config {
        val res = Root.run("cat $DCS_DB")
        if (!res.ok || res.stdout.isEmpty()) throw IllegalStateException("cannot read DCS db: ${res.stderr}")
        val local = File(ctx.filesDir, "dcs_copy.db")
        local.writeBytes(res.stdout)
        File(ctx.filesDir, "dcs_copy.db-journal").delete()

        val db = SQLiteDatabase.openDatabase(local.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val inputs = ArrayList<Input>()
            db.rawQuery("select id, data from activities order by _id", null).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val j = JSONObject(c.getString(1))
                    val type = j.optString("activityType")
                    if (type.isEmpty()) continue
                    val name = j.optString("name").takeIf { it.isNotEmpty() && it != "null" }
                    val functions = ArrayList<String>()
                    val map = j.optJSONArray("activityMap")
                    if (map != null) for (i in 0 until map.length()) {
                        val pair = map.optJSONArray(i) ?: continue
                        val act = pair.optJSONObject(1)?.optJSONArray("activate") ?: continue
                        for (k in 0 until act.length()) functions.add(act.getString(k))
                    }
                    // Prefer Fire TV's own display name for the type (res/values/input_types.xml,
                    // generated from Definitions.ActivityType); "STARK" is the Fire TV itself.
                    val label = name?.takeIf { it != type } ?: typeLabel(type) ?: prettify(type)
                    inputs.add(Input(id, type, label, functions))
                }
            }

            var endpointId = ""
            var deviceId = ""
            db.rawQuery("select data from data_store where id='METADATA_HASH'", null).use { c ->
                if (c.moveToFirst()) {
                    val meta = JSONObject(c.getString(0)).optJSONObject("deviceMetadata")
                    if (meta != null) {
                        val keys = meta.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val d = meta.getJSONObject(k)
                            // type 1 = TV. Prefer it; fall back to whatever is first.
                            if (endpointId.isEmpty() || d.optInt("type") == 1) {
                                endpointId = d.optString("chrEndpointId")
                                deviceId = d.optString("id", k)
                            }
                            if (d.optInt("type") == 1) break
                        }
                    }
                }
            }
            if (endpointId.isEmpty()) throw IllegalStateException("no TV endpoint in METADATA_HASH")
            return Config(inputs, endpointId, deviceId)
        } finally {
            db.close()
        }
    }

    private fun typeLabel(type: String): String? {
        val id = ctx.resources.getIdentifier("input_" + type.lowercase(), "string", ctx.packageName)
        return if (id != 0) ctx.getString(id) else null
    }

    private fun prettify(type: String): String =
        type.split('_').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
            .replace(Regex("^Hdmi (\\d)"), "HDMI $1")
}
