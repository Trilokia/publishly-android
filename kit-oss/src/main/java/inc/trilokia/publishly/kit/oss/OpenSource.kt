package inc.trilokia.publishly.kit.oss

import android.content.Context
import android.content.Intent
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import java.text.Collator
import java.util.Locale

/**
 * The "Open source licences" list. Nothing is written by hand: the app applies the
 * `com.mikepenz.aboutlibraries.plugin.android` Gradle plugin, which reads every dependency of the
 * app at build time and writes `res/raw/aboutlibraries.json` (names, versions, authors, licence
 * texts). This reads that file back. It lives in the `kit-oss` module, so an app with a licence
 * screen of its own doesn't carry AboutLibraries; `kit-oss` has no Compose in it, so a View app's
 * licence list doesn't name Compose either.
 *
 * Release builds shrink resources and may drop the file; keep it with a `res/raw/keep.xml`
 * holding `tools:keep="@raw/aboutlibraries"` (the sample app has one).
 */
object OpenSource {
    /** Every library in the build, sorted [byName]; empty when the plugin wasn't applied. */
    fun libraries(context: Context): Libs {
        val libs = Libs.Builder().withContext(context.applicationContext).build()
        return Libs(libs.libraries.sortedWith(byName()), libs.licenses)
    }

    /**
     * Name order as people read it: case and accents ignored, the language's own rules
     * ([Collator] at PRIMARY strength), so "libadb-android" sits under L, not after "Zstd".
     * Equal names keep a fixed order by uniqueId.
     */
    fun byName(locale: Locale = Locale.getDefault()): Comparator<Library> = byName(locale, Library::name, Library::uniqueId)

    // Generic so the JVM tests can sort plain names: AboutLibraries' classes need Java 21 to load
    internal fun <T> byName(locale: Locale, name: (T) -> String, id: (T) -> String): Comparator<T> {
        val collator = collator(locale)
        return Comparator<T> { a, b -> collator.compare(name(a), name(b)) }.thenBy(id)
    }

    /** [byName] for plain strings (licence names, say). Names equal to the collator fall back to exact order, so a sorted map never merges two of them. */
    fun nameOrder(locale: Locale = Locale.getDefault()): Comparator<String> {
        val collator = collator(locale)
        return Comparator<String> { a, b -> collator.compare(a, b) }.then(naturalOrder())
    }

    // A new one per comparator: Collator isn't thread-safe
    private fun collator(locale: Locale) = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

    /** Opens the plain-View list screen ([OpenSourceActivity]). Compose apps use `OpenSourceScreen` from `kit-oss-compose`. */
    fun open(context: Context) {
        context.startActivity(Intent(context, OpenSourceActivity::class.java))
    }
}
