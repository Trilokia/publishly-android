package inc.trilokia.publishly.kit

import android.app.Activity
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** What the plain-View screens share: the theme, a top bar with a back arrow, and edge-to-edge insets. */
internal object KitScreen {
    /** Call before `super.onCreate`. */
    fun applyTheme(activity: Activity) {
        activity.setTheme(KitTheme.style ?: R.style.Theme_PublishlyKit)
    }

    /**
     * Sets the content: [content] under a top bar showing [title], all in the PublishlyKit colours,
     * drawn edge to edge with the system bars padded out. Call after `super.onCreate`.
     */
    fun setContent(activity: Activity, title: CharSequence, content: View) {
        val c = KitTheme.colors(activity)
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val back = ImageButton(activity).apply {
            setImageResource(R.drawable.kit_arrow_back)
            imageTintList = ColorStateList.valueOf(c.onTopBar)
            background = RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(c.onTopBar, 0x40)), null, null)
            contentDescription = activity.getString(R.string.kit_back)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(4) }
            setOnClickListener { activity.finish() }
        }
        val titleView = TextView(activity).apply {
            text = title
            setTextAppearance(android.R.style.TextAppearance_Material_Title)
            setTextColor(c.onTopBar)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
            }
        }
        val topBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setBackgroundColor(c.topBar)
            elevation = 2 * density
            addView(back)
            addView(titleView)
        }
        content.setBackgroundColor(c.background)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(c.background)
            addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        activity.setContentView(root)

        // Edge to edge on every Android version (it is forced from 15 anyway): the top bar runs
        // under the status bar, the list scrolls under the navigation bar
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = KitColors.luminance(c.topBar) > 0.4
            isAppearanceLightNavigationBars = KitColors.luminance(c.background) > 0.4
        }
        if (content is AbsListView) content.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            topBar.setPadding(bars.left, bars.top, bars.right, 0)
            content.setPadding(bars.left, content.paddingTop, bars.right, bars.bottom)
            insets
        }
    }
}
