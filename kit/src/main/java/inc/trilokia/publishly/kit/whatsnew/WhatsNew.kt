package inc.trilokia.publishly.kit.whatsnew

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * A "What's new" screen and an after-update dialog, filled from one [WhatsNewSource]: the app's
 * Play listing, a JSON file on your server, or a GitHub repo's releases or commits.
 *
 * - [notes]: every version the source knows, newest first. Show them in a list (Compose:
 *   `WhatsNewList`; View: [openList]).
 * - [pending]: the notes to show once after an update, or nothing. Show them in a dialog
 *   (Compose: `WhatsNewDialog`; View: [showDialog]) and call [markSeen]. Nothing is pending on
 *   the first run on a phone, only after a versionCode change.
 *
 * The source is fetched at most once per [refreshMs] and kept in SharedPreferences, so the screen
 * works offline after the first load and a broken fetch keeps showing the last good copy.
 */
class WhatsNew(
    context: Context,
    val source: WhatsNewSource,
    private val refreshMs: Long = TimeUnit.HOURS.toMillis(24),
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_whats_new", Context.MODE_PRIVATE)
    private val bodyKey = "body:" + source.url
    private val fetchedKey = "fetched_at:" + source.url

    /** All known notes, newest first; empty when nothing could be read yet. */
    suspend fun notes(): List<ReleaseNote> {
        val cached = prefs.getString(bodyKey, null)
        val now = System.currentTimeMillis()
        if (cached == null || now - prefs.getLong(fetchedKey, 0L) >= refreshMs) {
            val fresh = runCatching { fetch(source) }
                .onFailure { Log.w(TAG, "What's new not fetched: $it") }
                .getOrNull()
            val parsed = fresh?.let { body ->
                runCatching { source.parse(body) }.onFailure { Log.w(TAG, "What's new unreadable: $it") }.getOrNull()
            }
            if (fresh != null && parsed != null) {
                prefs.edit().putString(bodyKey, fresh).putLong(fetchedKey, now).apply()
                return parsed
            }
        }
        return cached?.let { runCatching { source.parse(it) }.getOrNull() } ?: emptyList()
    }

    /**
     * What to show after an update: the versions between the one last marked seen and this one
     * when the source has version codes (your JSON file), otherwise the newest entry. Empty when
     * the app hasn't changed since [markSeen], and on the first run (which marks the current
     * version seen, so a fresh install isn't greeted with old news).
     */
    suspend fun pending(): List<ReleaseNote> {
        val current = JsonUpdates.currentVersionCode(app)
        val lastSeen = prefs.getInt(KEY_SEEN, 0)
        if (lastSeen == 0) {
            markSeen()
            return emptyList()
        }
        if (current <= lastSeen) return emptyList()
        return pick(notes(), lastSeen, current)
    }

    /** This version's notes have been shown; nothing is pending until the next update. */
    fun markSeen() {
        prefs.edit().putInt(KEY_SEEN, JsonUpdates.currentVersionCode(app)).apply()
    }

    /** The plain-dialog version of the after-update notes, for View-based apps. Marks them seen. */
    fun showDialog(activity: Activity, notes: List<ReleaseNote>) {
        if (activity.isFinishing || notes.isEmpty()) return
        markSeen()
        KitTheme.dialog(activity)
            .setTitle(activity.getString(R.string.kit_whats_new_in, currentVersionName(activity)))
            .setMessage(text(notes))
            .setPositiveButton(R.string.kit_ok, null)
            .let { KitTheme.show(it) }
    }

    /** Opens the plain-View list screen ([WhatsNewActivity]). Compose apps use `WhatsNewList`. */
    fun openList(context: Context) {
        context.startActivity(
            Intent(context, WhatsNewActivity::class.java)
                .putExtra(WhatsNewActivity.EXTRA_SOURCE, source as Serializable)
                .putExtra(WhatsNewActivity.EXTRA_REFRESH_MS, refreshMs),
        )
    }

    companion object {
        private const val TAG = "PublishlyKit.WhatsNew"
        private const val KEY_SEEN = "seen_version_code"

        /** The after-update rule on its own, for tests: see [pending]. */
        fun pick(notes: List<ReleaseNote>, lastSeenCode: Int, currentCode: Int): List<ReleaseNote> {
            if (notes.isEmpty() || currentCode <= lastSeenCode) return emptyList()
            if (notes.any { it.versionCode != null }) {
                return notes.filter { val c = it.versionCode; c != null && c > lastSeenCode && c <= currentCode }
            }
            return notes.take(1)
        }

        /** The notes as one block of text for a dialog: heading, lines, blank line, next version. */
        fun text(notes: List<ReleaseNote>): String = notes.joinToString("\n\n") { n ->
            listOf(n.heading, n.notes).filter { it.isNotBlank() }.joinToString("\n")
        }

        /** GETs [source]'s URL with its headers. Throws when the server can't be reached or says no. */
        suspend fun fetch(source: WhatsNewSource): String = withContext(Dispatchers.IO) {
            if (source is WhatsNewSource.Bundled) return@withContext source.json
            val conn = URL(source.url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.instanceFollowRedirects = true
                source.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode} from ${source.url}")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }

        fun currentVersionName(context: Context): String = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
