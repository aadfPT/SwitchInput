package pt.aadf.sourcesswitcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Setup screen: lists the inputs the Fire TV knows about; selecting one saves it for the tile. */
class ConfigActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var testBtn: Button
    private val main = Handler(Looper.getMainLooper())
    private var config: InputRepository.Config? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        status = findViewById(R.id.status)
        list = findViewById(R.id.list)
        testBtn = findViewById(R.id.test)
        list.layoutManager = LinearLayoutManager(this)
        testBtn.setOnClickListener { testNow() }
        refreshTestButton()
        load()
    }

    private fun load() {
        status.setText(R.string.loading)
        Thread {
            try {
                if (!Root.available()) throw IllegalStateException(getString(R.string.no_root))
                val cfg = InputRepository(this).load()
                main.post { show(cfg) }
            } catch (e: Exception) {
                main.post { status.text = e.message ?: e.toString() }
            }
        }.start()
    }

    private fun show(cfg: InputRepository.Config) {
        config = cfg
        val prefs = Prefs(this)
        status.text = prefs.inputLabel?.let { getString(R.string.current_choice, it) } ?: getString(R.string.pick_one)
        list.adapter = Adapter(cfg.inputs, prefs.inputType) { input ->
            prefs.save(input, cfg)
            status.text = getString(R.string.saved, input.label)
            list.adapter?.notifyDataSetChanged()
            refreshTestButton()
        }
        list.requestFocus()
    }

    private fun refreshTestButton() {
        val p = Prefs(this)
        testBtn.isEnabled = p.inputType != null
        testBtn.text = p.inputLabel?.let { getString(R.string.test_now_x, it) } ?: getString(R.string.test_now)
    }

    private fun testNow() {
        val p = Prefs(this)
        val type = p.inputType ?: return
        val ep = p.endpointId ?: return
        status.text = getString(R.string.switching_to, p.inputLabel ?: type)
        Thread {
            val err = try { Switcher.switchTo(type, ep, p.deviceId ?: "") } catch (e: Exception) { e.toString() }
            main.post { status.text = if (err == null) getString(R.string.sent) else getString(R.string.switch_failed, err) }
        }.start()
    }

    private class Adapter(
        val items: List<InputRepository.Input>,
        var selectedType: String?,
        val onPick: (InputRepository.Input) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) { val label: TextView = v.findViewById(R.id.label) }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_input, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val it = items[pos]
            val mark = if (it.type == selectedType) "✔  " else "     "
            val fn = if (it.functions.isEmpty()) "" else "   (" + it.functions.joinToString(" + ") + ")"
            h.label.text = mark + it.label + fn
            h.label.setOnClickListener { _ -> selectedType = it.type; onPick(it) }
        }
    }
}
