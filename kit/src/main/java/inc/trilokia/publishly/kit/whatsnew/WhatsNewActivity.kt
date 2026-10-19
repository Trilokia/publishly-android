package inc.trilokia.publishly.kit.whatsnew

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import inc.trilokia.publishly.kit.KitScreen
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The "What's new" list for View-based apps: a heading (version, date) and the notes per entry,
 * tap to open the release or commit page when there is one. Plain widgets in the [KitTheme]
 * colours. Start it with [WhatsNew.openList].
 */
class WhatsNewActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        KitScreen.applyTheme(this)
        super.onCreate(savedInstanceState)
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_SOURCE, WhatsNewSource::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_SOURCE) as? WhatsNewSource
        }
        if (source == null) {
            finish()
            return
        }
        val whatsNew = WhatsNew(this, source, intent.getLongExtra(EXTRA_REFRESH_MS, 24L * 60 * 60 * 1000))
        val adapter = Adapter()
        val colors = KitTheme.colors(this)
        val empty = TextView(this).apply {
            setText(R.string.kit_whats_new_empty)
            setTextColor(colors.textSecondary)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val list = ListView(this).apply {
            this.adapter = adapter
            divider = null
            setOnItemClickListener { _, _, position, _ ->
                adapter.items[position].url?.let { JsonUpdates.open(this@WhatsNewActivity, it) }
            }
        }
        val content = FrameLayout(this).apply {
            addView(list)
            addView(empty, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        KitScreen.setContent(this, getString(R.string.kit_whats_new), content)
        scope.launch {
            adapter.items = whatsNew.notes()
            adapter.notifyDataSetChanged()
            // Only now: "Nothing to show yet" must not flash while the source loads
            list.emptyView = empty
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class Adapter : BaseAdapter() {
        var items: List<ReleaseNote> = emptyList()
        private val colors = KitTheme.colors(this@WhatsNewActivity)

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: newRow()
            val note = items[position]
            row.findViewById<TextView>(ID_HEADING).apply {
                text = note.heading
                visibility = if (note.heading.isBlank()) View.GONE else View.VISIBLE
            }
            row.findViewById<TextView>(ID_NOTES).text = note.notes
            return row
        }

        private fun newRow(): View = LinearLayout(this@WhatsNewActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(TextView(context).apply {
                id = ID_HEADING
                setTextAppearance(android.R.style.TextAppearance_Material_Subhead)
                setTextColor(colors.text)
            })
            addView(TextView(context).apply {
                id = ID_NOTES
                setTextAppearance(android.R.style.TextAppearance_Material_Body1)
                setTextColor(colors.textSecondary)
            })
        }
    }

    companion object {
        const val EXTRA_SOURCE = "source"
        const val EXTRA_REFRESH_MS = "refresh_ms"
        private val ID_HEADING = View.generateViewId()
        private val ID_NOTES = View.generateViewId()
    }
}
