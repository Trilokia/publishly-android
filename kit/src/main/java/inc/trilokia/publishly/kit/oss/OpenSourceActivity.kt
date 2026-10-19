package inc.trilokia.publishly.kit.oss

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import com.mikepenz.aboutlibraries.entity.Library
import inc.trilokia.publishly.kit.KitColors
import inc.trilokia.publishly.kit.KitScreen
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R

/**
 * The licence list for View-based apps: one row per library (name, version, author, licence),
 * tap for the licence text with a link to the library's site. Plain widgets only, so it needs no
 * AppCompat or Material, in the [KitTheme] colours. Start it with [OpenSource.open].
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
        KitTheme.dialog(this)
            .setTitle(library.name)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .apply {
                if (site != null) setNeutralButton(site.removePrefix("https://").removePrefix("http://").take(40)) { _, _ ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site))) }
                }
            }
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
