package inc.trilokia.publishly.kit.promo

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import inc.trilokia.publishly.kit.KitScreen
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The "More apps from us" screen for View-based apps: icon, name and tagline per app, tap to
 * open its Play page. Plain widgets in the [KitTheme] colours. Start it with [AppPromos.openList].
 */
class MoreAppsActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        KitScreen.applyTheme(this)
        super.onCreate(savedInstanceState)
        val promos = AppPromos(
            this,
            url = intent.getStringExtra(EXTRA_URL),
            bundledJson = intent.getStringExtra(EXTRA_BUNDLED_JSON),
        )
        val adapter = Adapter()
        val list = ListView(this).apply {
            this.adapter = adapter
            divider = null
            setOnItemClickListener { _, _, position, _ -> promos.open(this@MoreAppsActivity, adapter.items[position]) }
        }
        KitScreen.setContent(this, getString(R.string.kit_more_apps), list)
        scope.launch {
            adapter.items = promos.apps()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class Adapter : BaseAdapter() {
        var items: List<PromoApp> = emptyList()
        private val colors = KitTheme.colors(this@MoreAppsActivity)

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: newRow()
            val app = items[position]
            val icon = row.findViewById<ImageView>(ID_ICON)
            row.findViewById<TextView>(ID_NAME).text = if (app.badge != null) "${app.name}  ·  ${app.badge}" else app.name
            row.findViewById<TextView>(ID_TAGLINE).apply {
                text = app.tagline
                visibility = if (app.tagline.isBlank()) View.GONE else View.VISIBLE
            }
            icon.setImageDrawable(null)
            icon.tag = app.iconUrl
            scope.launch {
                val bitmap = PromoIcons.load(this@MoreAppsActivity, app.iconUrl)
                // The row may have been recycled for another app while the icon loaded
                if (icon.tag == app.iconUrl) icon.setImageBitmap(bitmap)
            }
            return row
        }

        private fun newRow(): View = LinearLayout(this@MoreAppsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(ImageView(context).apply {
                id = ID_ICON
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(16) }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    id = ID_NAME
                    setTextAppearance(android.R.style.TextAppearance_Material_Subhead)
                    setTextColor(colors.text)
                })
                addView(TextView(context).apply {
                    id = ID_TAGLINE
                    setTextAppearance(android.R.style.TextAppearance_Material_Body1)
                    setTextColor(colors.textSecondary)
                    maxLines = 2
                })
            })
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_BUNDLED_JSON = "bundled_json"
        private val ID_ICON = View.generateViewId()
        private val ID_NAME = View.generateViewId()
        private val ID_TAGLINE = View.generateViewId()
    }
}
