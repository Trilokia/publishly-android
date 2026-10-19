package inc.trilokia.publishly.kit

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/** The colours PublishlyKit's plain-View screens and dialogs are painted with. ARGB ints. */
data class KitColors(
    /** The top bar of the list screens. */
    val topBar: Int,
    val onTopBar: Int,
    /** Buttons and links: dialog buttons, "Get". */
    val accent: Int,
    /** The screens' background and their main text. */
    val background: Int,
    val text: Int,
    val textSecondary: Int,
    /** Dialogs and cards. */
    val surface: Int,
) {
    companion object {
        /** Black or white, whichever reads better on [color]. */
        fun onColor(color: Int): Int = if (luminance(color) > 0.4) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

        /** Relative luminance (sRGB, 0 dark to 1 light). Pure Kotlin, so it runs in JVM tests too. */
        fun luminance(color: Int): Double {
            fun channel(v: Int): Double {
                val c = v / 255.0
                return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel((color shr 16) and 0xFF) + 0.7152 * channel((color shr 8) and 0xFF) + 0.0722 * channel(color and 0xFF)
        }
    }
}

/**
 * One look for every PublishlyKit element. The Compose ones (`UpdateDialog`, `MoreAppsRow`,
 * `WhatsNewList`...) take the app's `MaterialTheme` by themselves. The plain-View screens and
 * dialogs take their colours from here, in this order:
 *
 * 1. [colors], when the app set it. Compose apps call `SyncKitTheme()` inside their theme
 *    and get the same colours in both worlds, light and dark.
 * 2. Otherwise the app's own theme: `android:colorPrimary`, `colorAccent`, `colorBackground`,
 *    `textColorPrimary`... (what AppCompat and Material themes set), so an app with a themed
 *    toolbar gets PublishlyKit screens with the same toolbar colour.
 * 3. Otherwise Material 3's baseline palette, light or dark with the system.
 *
 * [style] and [dialogStyle] swap the base themes (`Theme.PublishlyKit`, `Theme.PublishlyKit.Dialog`) for the
 * app's own; the colours above are still painted on top.
 */
object KitTheme {
    /** Explicit colours; null means "from the app's theme". */
    @JvmStatic
    @Volatile
    var colors: KitColors? = null

    /** A theme resource for the View screens instead of `Theme.PublishlyKit`. Use one without an action bar: PublishlyKit draws its own top bar. */
    @JvmStatic
    @Volatile
    var style: Int? = null

    /** A theme resource for the dialogs instead of `Theme.PublishlyKit.Dialog`. */
    @JvmStatic
    @Volatile
    var dialogStyle: Int? = null

    /** The colours in force, see the class comment. */
    @JvmStatic
    fun colors(context: Context): KitColors = colors ?: fromAppTheme(context.applicationContext)

    /** An AlertDialog builder on the PublishlyKit dialog theme. Finish it with [show], which paints the colours. */
    @JvmStatic
    fun dialog(activity: Activity): AlertDialog.Builder = AlertDialog.Builder(activity, dialogStyle ?: R.style.Theme_PublishlyKit_Dialog)

    /** Shows the dialog with the PublishlyKit colours: surface background, text, accent buttons. */
    @JvmStatic
    fun show(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()
        dialog.show()
        paint(dialog)
        return dialog
    }

    private fun paint(dialog: AlertDialog) {
        val context = dialog.context
        val c = colors(context)
        val density = context.resources.displayMetrics.density
        val shape = GradientDrawable().apply {
            setColor(c.surface)
            cornerRadius = 28 * density
        }
        dialog.window?.setBackgroundDrawable(InsetDrawable(shape, (16 * density).toInt()))
        val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
        if (titleId != 0) dialog.findViewById<TextView>(titleId)?.setTextColor(c.text)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(c.textSecondary)
        for (which in intArrayOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            dialog.getButton(which)?.setTextColor(c.accent)
        }
    }

    /** Reads the application theme's colour attributes, falling back to the PublishlyKit palette per colour. */
    internal fun fromAppTheme(app: Context): KitColors {
        val dark = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val defaults = defaults(app)
        val theme = app.theme
        fun attr(id: Int, fallback: Int): Int {
            val a = theme.obtainStyledAttributes(intArrayOf(id))
            try {
                return a.getColor(0, fallback)
            } finally {
                a.recycle()
            }
        }
        // Attributes from AppCompat / Material Components, when the app has them (merged into the app's R)
        fun appAttr(name: String, fallback: Int): Int {
            val id = app.resources.getIdentifier(name, "attr", app.packageName)
            return if (id == 0) fallback else attr(id, fallback)
        }
        val primary = attr(android.R.attr.colorPrimary, defaults.topBar)
        val background = attr(android.R.attr.colorBackground, defaults.background)
        val text = attr(android.R.attr.textColorPrimary, defaults.text)
        val textSecondary = attr(android.R.attr.textColorSecondary, defaults.textSecondary)
        return KitColors(
            topBar = primary,
            onTopBar = appAttr("colorOnPrimary", KitColors.onColor(primary)),
            accent = attr(android.R.attr.colorAccent, primary),
            background = background,
            text = text,
            textSecondary = textSecondary,
            surface = appAttr("colorSurface", if (dark) ColorUtils.blendARGB(background, Color.WHITE, 0.08f) else background),
        )
    }

    /** The Material 3 baseline, light or dark with the system. */
    internal fun defaults(context: Context): KitColors = KitColors(
        topBar = context.getColor(R.color.kit_primary),
        onTopBar = context.getColor(R.color.kit_on_primary),
        accent = context.getColor(R.color.kit_primary),
        background = context.getColor(R.color.kit_background),
        text = context.getColor(R.color.kit_text),
        textSecondary = context.getColor(R.color.kit_text_secondary),
        surface = context.getColor(R.color.kit_surface),
    )
}
