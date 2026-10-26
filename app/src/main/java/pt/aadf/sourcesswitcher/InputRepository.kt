package pt.aadf.sourcesswitcher

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File

class InputRepository(private val ctx: Context) {

    data class Input(val id: String, val type: String, val label: String, val functions: List<String>)
    data class Config(val inputs: List<Input>, val endpointId: String, val deviceId: String)

    companion object {
        const val DCS_DB_PATH = "/data/data/com.amazon.tv.devicecontrol/databases/device_control_db"
        private const val DEVICE_TYPE_TV = 1
        private const val FIRE_TV_ITSELF = "STARK"
    }

    fun load(): Config {
        val db = openRootCopyOfDcsDb()
        try {
            val inputs = readActivities(db)
            val (endpointId, deviceId) = readTvEndpoint(db)
            return Config(inputs, endpointId, deviceId)
        } finally {
            db.close()
        }
    }

    private fun openRootCopyOfDcsDb(): SQLiteDatabase {
        val res = Root.run("cat $DCS_DB_PATH")
        if (!res.ok || res.stdout.isEmpty()) throw IllegalStateException("cannot read DCS db: ${res.stderr}")
        val local = File(ctx.filesDir, "dcs_copy.db")
        local.writeBytes(res.stdout)
        File(ctx.filesDir, "dcs_copy.db-journal").delete()
        return SQLiteDatabase.openDatabase(local.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun readActivities(db: SQLiteDatabase): List<Input> {
        val inputs = ArrayList<Input>()
        db.rawQuery("select id, data from activities order by _id", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val activity = JSONObject(c.getString(1))
                val type = activity.optString("activityType")
                if (type.isEmpty()) continue
                val customName = activity.optString("name").takeIf { it.isNotEmpty() && it != "null" && it != type }
                val label = customName ?: typeLabel(type) ?: prettify(type)
                inputs.add(Input(id, type, label, activatedFunctions(activity)))
            }
        }
        return inputs
    }

    private fun activatedFunctions(activity: JSONObject): List<String> {
        val functions = ArrayList<String>()
        val map = activity.optJSONArray("activityMap") ?: return functions
        for (i in 0 until map.length()) {
            val pair = map.optJSONArray(i) ?: continue
            val activate = pair.optJSONObject(1)?.optJSONArray("activate") ?: continue
            for (k in 0 until activate.length()) functions.add(activate.getString(k))
        }
        return functions
    }

    private fun readTvEndpoint(db: SQLiteDatabase): Pair<String, String> {
        var endpointId = ""
        var deviceId = ""
        db.rawQuery("select data from data_store where id='METADATA_HASH'", null).use { c ->
            if (!c.moveToFirst()) return@use
            val devices = JSONObject(c.getString(0)).optJSONObject("deviceMetadata") ?: return@use
            val keys = devices.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val d = devices.getJSONObject(k)
                val isTv = d.optInt("type") == DEVICE_TYPE_TV
                if (endpointId.isEmpty() || isTv) {
                    endpointId = d.optString("chrEndpointId")
                    deviceId = d.optString("id", k)
                }
                if (isTv) break
            }
        }
        if (endpointId.isEmpty()) throw IllegalStateException("no TV endpoint in METADATA_HASH")
        return endpointId to deviceId
    }

    private fun typeLabel(type: String): String? {
        val id = ctx.resources.getIdentifier("input_" + type.lowercase(), "string", ctx.packageName)
        return if (id != 0) ctx.getString(id) else null
    }

    private fun prettify(type: String): String =
        if (type == FIRE_TV_ITSELF) "Fire TV"
        else type.split('_').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
            .replace(Regex("^Hdmi (\\d)"), "HDMI $1")
}
