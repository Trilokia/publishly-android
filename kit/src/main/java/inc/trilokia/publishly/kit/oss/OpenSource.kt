package inc.trilokia.publishly.kit.oss

import android.content.Context
import android.content.Intent
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

/**
 * The "Open source licences" list. Nothing is written by hand: the app applies the
 * `com.mikepenz.aboutlibraries.plugin.android` Gradle plugin, which reads every dependency of the
 * app at build time and writes `res/raw/aboutlibraries.json` (names, versions, authors, licence
 * texts). This reads that file back.
 *
 * Release builds shrink resources and may drop the file; keep it with a `res/raw/keep.xml`
 * holding `tools:keep="@raw/aboutlibraries"` (the sample app has one).
 */
object OpenSource {
    /** Every library in the build, sorted by name; empty when the plugin wasn't applied. */
    fun libraries(context: Context): Libs =
        Libs.Builder().withContext(context.applicationContext).build()

    /** Opens the plain-View list screen ([OpenSourceActivity]). Compose apps use `OpenSourceScreen`. */
    fun open(context: Context) {
        context.startActivity(Intent(context, OpenSourceActivity::class.java))
    }
}
