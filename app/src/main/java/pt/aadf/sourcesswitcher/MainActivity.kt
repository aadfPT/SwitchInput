package pt.aadf.sourcesswitcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * The app's own screen: every input Fire TV knows about, as a card. Select toggles the
 * input's launcher shortcut on or off; a long press switches to it right away.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var grid: RecyclerView
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        grid = findViewById(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, 2)
    }

    /** Re-read the equipment list every time the screen comes to the front, so equipment
     *  added in Fire TV Settings while this app sat in the background shows up at once. */
    override fun onStart() {
        super.onStart()
        load()
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
        val items = cfg.inputs.map { Row(it, Shortcuts.exists(this, it.type), Shortcuts.isEnabled(this, it.type)) }
        grid.adapter = Adapter(items,
            onToggle = { row ->
                if (!row.supported) {
                    status.setText(R.string.unknown_input); return@Adapter
                }
                if (row.busy) return@Adapter
                val on = !row.enabled
                row.busy = true
                status.text = getString(if (on) R.string.adding else R.string.removing, row.input.label)
                Thread {
                    // Installing / uninstalling the stub package takes a second or two.
                    val err = try { Shortcuts.setEnabled(this, row.input.type, on) } catch (e: Exception) { e.toString() }
                    main.post {
                        row.busy = false
                        if (err == null) {
                            row.enabled = on
                            status.text = getString(if (on) R.string.added else R.string.removed, row.input.label)
                            main.postDelayed({ updateCount(items) }, 2500)
                        } else {
                            status.text = getString(R.string.shortcut_failed, err)
                        }
                        grid.adapter?.notifyItemChanged(items.indexOf(row))
                    }
                }.start()
            },
            onTry = { row ->
                status.text = getString(R.string.switching_to, row.input.label)
                Thread {
                    val err = try { Switcher.switchTo(row.input.type, cfg.endpointId, cfg.deviceId) } catch (e: Exception) { e.toString() }
                    main.post { status.text = if (err == null) getString(R.string.switching_to, row.input.label) else getString(R.string.switch_failed, err) }
                }.start()
            })
        updateCount(items)
        grid.requestFocus()
    }

    private fun updateCount(items: List<Row>) {
        val n = items.count { it.enabled }
        status.text = if (n == 0) getString(R.string.status_count_none) else resources.getQuantityString(R.plurals.status_count, n, n)
    }

    class Row(val input: InputRepository.Input, val supported: Boolean, var enabled: Boolean, var busy: Boolean = false)

    private class Adapter(val items: List<Row>, val onToggle: (Row) -> Unit, val onTry: (Row) -> Unit) :
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

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val row = items[pos]
            val ctx = h.itemView.context
            val (small, big) = badge(row.input.type)
            h.badgeSmall.text = small
            h.badgeSmall.visibility = if (small.isEmpty()) View.GONE else View.VISIBLE
            h.badge.text = big
            h.badge.textSize = if (big.length <= 2) 24f else if (big.length <= 4) 16f else 13f
            h.label.text = row.input.label
            // Second line: what the switch blasts when that adds information (PlayStation -> HDMI 2,
            // Fire TV -> Power on + HDMI 3); otherwise the voice command the shortcut replaces.
            val fns = row.input.functions
            h.meta.text = if (fns.isEmpty() || fns == listOf(row.input.type))
                ctx.getString(R.string.voice_phrase, row.input.label)
            else fns.joinToString(" + ") { pretty(it) }
            bindPill(h, row)
            h.card.setOnClickListener { onToggle(row) }
            h.card.setOnLongClickListener { onTry(row); true }
            h.card.setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.03f else 1f).scaleY(if (focused) 1.03f else 1f).setDuration(120).start()
            }
            h.card.alpha = if (row.supported) 1f else 0.45f
            h.card.contentDescription = row.input.label + ", " + ctx.getString(if (row.enabled) R.string.pill_on else R.string.pill_off)
        }

        private fun bindPill(h: VH, row: Row) {
            val ctx = h.itemView.context
            h.pill.setText(if (row.busy) R.string.pill_busy else if (row.enabled) R.string.pill_on else R.string.pill_off)
            h.pill.setBackgroundResource(if (row.enabled && !row.busy) R.drawable.pill_on else R.drawable.pill_off)
            h.pill.setTextColor(ctx.getColor(if (row.enabled && !row.busy) R.color.accent_fg else R.color.muted))
        }

        /** Badge text: ("HDMI", "2") for HDMI inputs, ("", short code) otherwise. */
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
