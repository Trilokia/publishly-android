package inc.trilokia.publishly.kit.oss

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import com.mikepenz.aboutlibraries.entity.Library
import inc.trilokia.publishly.kit.KitColors
import inc.trilokia.publishly.kit.KitScreen
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.oss.R
import inc.trilokia.publishly.kit.R as KitR

/**
 * The licence list for View-based apps: one row per library (name, version, author, licence),
 * sorted by [OpenSource.byName]; tap for the licence text ([LicenceText.reflow]) under a link to the
 * library's site. Plain widgets only, so it needs no AppCompat, Material or Compose, in the
 * [KitTheme] colours. Start it with [OpenSource.open]. In the `kit-oss` module.
 */
class OpenSourceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        KitScreen.applyTheme(this)
        super.onCreate(savedInstanceState)
        val colors = KitTheme.colors(this)
        val libraries = OpenSource.libraries(this).libraries
        val empty = TextView(this).apply {
            setText(R.string.kit_open_source_empty)
            setTextColor(colors.textSecondary)
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val list = ListView(this).apply {
            adapter = LibraryAdapter(libraries, colors)
            emptyView = empty
            setOnItemClickListener { _, _, position, _ -> show(libraries[position]) }
        }
        // The empty view must share the parent to be swapped in
        val content = FrameLayout(this).apply {
            addView(list)
            addView(empty)
        }
        KitScreen.setContent(this, getString(R.string.kit_open_source), content)
    }

    private fun show(library: Library) {
        val licence = library.licenses.firstOrNull()
        val text = licence?.licenseContent?.takeIf { it.isNotBlank() }
            ?: licence?.url
            ?: library.description.orEmpty()
        val site = library.website ?: licence?.url
        val colors = KitTheme.colors(this)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        // The link scrolls with the text: as a button under OK it was cut off by the dialog's round bottom
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(4), dp(24), dp(8))
        }
        if (site != null) {
            body.addView(TextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Body1)
                this.text = site.removePrefix("https://").removePrefix("http://")
                setTextColor(colors.accent)
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                minHeight = dp(48)
                gravity = Gravity.CENTER_VERTICAL
                setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site))) } }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                // Keeps a two-line link (large fonts) apart from the licence title
                bottomMargin = dp(8)
            })
        }
        body.addView(TextView(this).apply {
            setTextAppearance(android.R.style.TextAppearance_Material_Body1)
            this.text = LicenceText.reflow(text)
            setTextColor(colors.textSecondary)
        })
        KitTheme.dialog(this)
            .setTitle(library.name)
            .setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton(KitR.string.kit_ok, null)
            .let { KitTheme.show(it) }
    }

    private inner class LibraryAdapter(private val items: List<Library>, private val colors: KitColors) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            val library = items[position]
            val author = library.developers.mapNotNull { it.name }.joinToString(", ").ifBlank { library.organization?.name.orEmpty() }
            val licence = library.licenses.joinToString(", ") { it.name }
            view.findViewById<TextView>(android.R.id.text1).apply {
                text = listOfNotNull(library.name, library.artifactVersion).joinToString(" ")
                setTextColor(colors.text)
            }
            view.findViewById<TextView>(android.R.id.text2).apply {
                text = listOf(author, licence).filter { it.isNotBlank() }.joinToString(" · ")
                setTextColor(colors.textSecondary)
            }
            return view
        }
    }
}
