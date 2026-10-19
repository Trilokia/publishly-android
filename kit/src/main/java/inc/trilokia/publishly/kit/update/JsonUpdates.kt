package inc.trilokia.publishly.kit.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One version on the update server. */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    /** Where "Update" goes: a Play link, or an APK on your own server. */
    val url: String,
    /** Shown in the dialog; the JSON's lines joined with newlines. */
    val releaseNotes: String,
)

/**
 * The update check the PGT apps have always used: a small JSON file on your own server says which
 * version is the latest. Works without Play (side loads, other stores) and lets you push a message
 * to old versions at any time. The file looks like this (releaseNotes may also be one string):
 *
 * ```json
 * {
 *   "latestVersion": "0.21.7",
 *   "latestVersionCode": "193",
 *   "url": "https://play.google.com/store/apps/details?id=inc.trilokia.pubgfxtool.free",
 *   "releaseNotes": ["This update includes:", "- New feature ..."]
 * }
 * ```
 *
 * Use with [PlayUpdates] or on its own: call [check] from a coroutine in `onResume`; when it
 * returns something, show [showDialog] (or your own Compose dialog). The last answer is kept in
 * SharedPreferences, so a phone that is offline today still sees the dialog it saw yesterday.
 * [bundledJson] (the same format, shipped in `assets/`) is the answer when the server has never
 * been reached, or the only answer when [url] is null.
 */
class JsonUpdates(context: Context, private val url: String?, private val bundledJson: String? = null) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_json_updates", Context.MODE_PRIVATE)

    /** The newer version, or null when this build is the latest (or nothing is known). */
    suspend fun check(): UpdateInfo? {
        val current = currentVersionCode(app)
        val fetched = url?.let { u ->
            runCatching { fetch(u) }
                .onFailure { Log.w(TAG, "Update check failed: $it") }
                .getOrNull()
        }
        if (fetched != null) {
            remember(fetched)
            return fetched.takeIf { it.versionCode > current }
        }
        val known = remembered() ?: bundledJson?.let { runCatching { parse(it) }.getOrNull() }
        return known?.takeIf { it.versionCode > current }
    }

    /** The plain-dialog version of the update prompt, for View-based apps. */
    fun showDialog(activity: Activity, info: UpdateInfo, cancelable: Boolean = true) {
        if (activity.isFinishing) return
        val message = info.releaseNotes.ifBlank { activity.getString(R.string.kit_update_version, info.versionName) }
        KitTheme.dialog(activity)
            .setTitle(R.string.kit_update_title)
            .setMessage(message)
            .setCancelable(cancelable)
            .setPositiveButton(R.string.kit_update_now) { _, _ -> open(activity, info.url) }
            .apply { if (cancelable) setNegativeButton(R.string.kit_update_later) { d, _ -> d.dismiss() } }
            .let { KitTheme.show(it) }
    }

    private fun remember(info: UpdateInfo) {
        prefs.edit()
            .putString("name", info.versionName)
            .putInt("code", info.versionCode)
            .putString("url", info.url)
            .putString("notes", info.releaseNotes)
            .apply()
    }

    private fun remembered(): UpdateInfo? {
        val code = prefs.getInt("code", 0)
        if (code == 0) return null
        return UpdateInfo(
            versionName = prefs.getString("name", "") ?: "",
            versionCode = code,
            url = prefs.getString("url", "") ?: "",
            releaseNotes = prefs.getString("notes", "") ?: "",
        )
    }

    companion object {
        private const val TAG = "PublishlyKit.JsonUpdates"

        /** Reads the JSON described above. Throws on anything missing or malformed. */
        fun parse(json: String): UpdateInfo {
            val o = JSONObject(json)
            val notes = when (val n = o.opt("releaseNotes")) {
                is JSONArray -> (0 until n.length()).joinToString("\n") { n.getString(it).trimEnd() }
                null -> ""
                else -> n.toString()
            }
            return UpdateInfo(
                versionName = o.getString("latestVersion"),
                // The PGT files write the code as a string ("193"); accept a number too
                versionCode = o.get("latestVersionCode").toString().trim().toInt(),
                url = o.getString("url"),
                releaseNotes = notes,
            )
        }

        /** Downloads and parses the JSON at [url]. Throws when the server can't be reached. */
        suspend fun fetch(url: String): UpdateInfo = withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode} from $url")
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }

        fun currentVersionCode(context: Context): Int = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
            else @Suppress("DEPRECATION") info.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }

        /** Opens a Play link in the Play app when there is one, otherwise in the browser. */
        fun open(context: Context, url: String) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { Log.w(TAG, "Nothing can open $url") }
        }
    }
}
