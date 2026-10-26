package pt.aadf.sourcesswitcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLocale.restore(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        list = findViewById(R.id.grid)
        list.layoutManager = LinearLayoutManager(this)
        findViewById<TextView>(R.id.version).text = "v" + BuildConfig.VERSION_NAME
        val language = findViewById<TextView>(R.id.language)
        language.text = getString(R.string.language_value, AppLocale.displayName(this, AppLocale.current(this)))
        language.setOnClickListener { chooseLanguage() }
    }

    override fun onStart() {
        super.onStart()
        load()
    }

    private fun chooseLanguage() {
        val current = AppLocale.current(this)
        val names = AppLocale.CHOICES.map { AppLocale.displayName(this, it) }.toTypedArray()
        AlertDialog.Builder(this, R.style.Theme_SourcesSwitcher_Dialog)
            .setTitle(R.string.language_choose)
            .setSingleChoiceItems(names, AppLocale.CHOICES.indexOf(current)) { dialog, which ->
                dialog.dismiss()
                val tag = AppLocale.CHOICES[which]
                if (tag != current) AppLocale.apply(this, tag)
            }
            .show()
    }

    private fun load() {
        status.setText(R.string.loading)
        Thread {
            try {
                if (!Root.available()) throw IllegalStateException(getString(R.string.no_root))
                val cfg = InputRepository(this).load()
                Prefs(this).saveEndpoint(cfg)
                main.post { show(cfg) }
            } catch (e: Exception) {
                main.post { status.text = e.message ?: e.toString() }
            }
        }.start()
    }

    private fun show(cfg: InputRepository.Config) {
        val rows = cfg.inputs.map { Row(it, Shortcuts.exists(this, it.type), Shortcuts.isEnabled(this, it.type)) }
        list.adapter = Adapter(rows, onToggle = { toggleShortcut(it, rows) }, onTry = { switchNow(it, cfg) })
        updateCount(rows)
        list.requestFocus()
    }

    private fun toggleShortcut(row: Row, rows: List<Row>) {
        if (!row.supported) {
            status.setText(R.string.unknown_input)
            return
        }
        if (row.busy) return
        val on = !row.enabled
        row.busy = true
        status.text = getString(if (on) R.string.adding else R.string.removing, row.input.label)
        Thread {
            val err = try { Shortcuts.setEnabled(this, row.input.type, on) } catch (e: Exception) { e.toString() }
            main.post {
                row.busy = false
                if (err == null) {
                    row.enabled = on
                    status.text = getString(if (on) R.string.added else R.string.removed, row.input.label)
                    main.postDelayed({ updateCount(rows) }, 2500)
                } else {
                    status.text = getString(R.string.shortcut_failed, err)
                }
                list.adapter?.notifyItemChanged(rows.indexOf(row))
            }
        }.start()
    }

    private fun switchNow(row: Row, cfg: InputRepository.Config) {
        status.text = getString(R.string.switching_to, row.input.label)
        Thread {
            val err = try { Switcher.switchTo(row.input.type, cfg.endpointId, cfg.deviceId) } catch (e: Exception) { e.toString() }
            main.post { status.text = if (err == null) getString(R.string.switching_to, row.input.label) else getString(R.string.switch_failed, err) }
        }.start()
    }

    private fun updateCount(rows: List<Row>) {
        val n = rows.count { it.enabled }
        status.text = if (n == 0) getString(R.string.status_count_none) else resources.getQuantityString(R.plurals.status_count, n, n)
    }

    class Row(val input: InputRepository.Input, val supported: Boolean, var enabled: Boolean, var busy: Boolean = false)

    private class Adapter(val rows: List<Row>, val onToggle: (Row) -> Unit, val onTry: (Row) -> Unit) :
        RecyclerView.Adapter<Adapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val card: View = v.findViewById(R.id.card)
            val badgeSmall: TextView = v.findViewById(R.id.badge_small)
            val badge: TextView = v.findViewById(R.id.badge)
            val label: TextView = v.findViewById(R.id.label)
            val meta: TextView = v.findViewById(R.id.meta)
            val pill: TextView = v.findViewById(R.id.pill)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_input, parent, false))

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val row = rows[pos]
            val ctx = h.itemView.context
            val (small, big) = badge(row.input.type)
            h.badgeSmall.text = small
            h.badgeSmall.visibility = if (small.isEmpty()) View.GONE else View.VISIBLE
            h.badge.text = big
            h.badge.textSize = if (big.length <= 2) 24f else if (big.length <= 4) 16f else 13f
            h.label.text = row.input.label
            h.meta.text = secondLine(ctx, row.input)
            bindPill(h, row)
            h.card.setOnClickListener { onToggle(row) }
            h.card.setOnLongClickListener { onTry(row); true }
            h.card.setOnFocusChangeListener { v, focused ->
                val scale = if (focused) 1.03f else 1f
                v.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
            }
            h.card.alpha = if (row.supported) 1f else 0.45f
            h.card.contentDescription = row.input.label + ", " + ctx.getString(if (row.enabled) R.string.pill_on else R.string.pill_off)
        }

        private fun secondLine(ctx: android.content.Context, input: InputRepository.Input): String {
            val fns = input.functions
            val nothingBeyondTheTypeItself = fns.isEmpty() || fns == listOf(input.type)
            return if (nothingBeyondTheTypeItself) ctx.getString(R.string.voice_phrase, input.label)
            else fns.joinToString(" + ") { pretty(it) }
        }

        private fun bindPill(h: VH, row: Row) {
            val ctx = h.itemView.context
            val lit = row.enabled && !row.busy
            h.pill.setText(if (row.busy) R.string.pill_busy else if (row.enabled) R.string.pill_on else R.string.pill_off)
            h.pill.setBackgroundResource(if (lit) R.drawable.pill_on else R.drawable.pill_off)
            h.pill.setTextColor(ctx.getColor(if (lit) R.color.accent_fg else R.color.muted))
        }

        private fun badge(type: String): Pair<String, String> {
            Regex("^HDMI_(\\d)$").find(type)?.let { return "HDMI" to it.groupValues[1] }
            return "" to when (type) {
                "STARK" -> "FTV"; "ANTENA" -> "ANT"; "PLAY_STATION" -> "PS"
                "PLAY_STATION_2" -> "PS2"; "PLAY_STATION_3" -> "PS3"; "PLAY_STATION_4" -> "PS4"
                "XBOX_ONE" -> "XB1"; "XBOX_360" -> "X360"; "NINTENDO_SWITCH" -> "NSW"
                "APPLE_TV" -> "ATV"; "BLU_RAY" -> "BD"; "DVD_PLAYER" -> "DVD"; "COMPOSITE" -> "AV"
                else -> type.replace('_', ' ').take(4)
            }
        }

        private fun pretty(fn: String) = fn.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
            .replace(Regex("^Hdmi (\\d)"), "HDMI $1")
    }
}
