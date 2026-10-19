package inc.trilokia.publishly.kit.promo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Promotes your other apps from a JSON feed on your server ([PromoFeed] has the format). No ad
 * network, no SDK: the list is yours, and it changes when you edit the file.
 *
 * - [apps]: your apps that this phone doesn't have yet (the app itself and installed ones are
 *   dropped), best first. Show them in a "More apps from us" row or list.
 * - [featured]: the one app the feed singles out, when it is due: not installed, not shown in
 *   the last [featuredRestMs], not dismissed for good. Show it in a dialog at a quiet moment.
 *
 * The feed is fetched at most once per [refreshMs] and kept in SharedPreferences, so it works
 * offline after the first load. [bundledJson] (a copy in `assets/`) is used until the server
 * answers the first time, or always when [url] is null.
 */
class AppPromos(
    context: Context,
    private val url: String?,
    private val bundledJson: String? = null,
    private val refreshMs: Long = TimeUnit.HOURS.toMillis(24),
    private val featuredRestMs: Long = TimeUnit.DAYS.toMillis(7),
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_promos", Context.MODE_PRIVATE)

    /** The whole feed, from the server, the cache or the bundled copy; null when there is none. */
    suspend fun feed(): PromoFeed? {
        val cached = prefs.getString(KEY_JSON, null)
        val now = System.currentTimeMillis()
        val feedUrl = url
        if (feedUrl != null && (cached == null || now - prefs.getLong(KEY_FETCHED_AT, 0L) >= refreshMs)) {
            val fresh = runCatching { fetch(feedUrl) }
                .onFailure { Log.w(TAG, "Promo feed not fetched: $it") }
                .getOrNull()
            val parsed = fresh?.let { json -> runCatching { PromoFeed.parse(json) }.onFailure { Log.w(TAG, "Promo feed unreadable: $it") }.getOrNull() }
            if (fresh != null && parsed != null) {
                prefs.edit().putString(KEY_JSON, fresh).putLong(KEY_FETCHED_AT, now).apply()
                return parsed
            }
        }
        return cached?.let { runCatching { PromoFeed.parse(it) }.getOrNull() }
            ?: bundledJson?.let { runCatching { PromoFeed.parse(it) }.getOrNull() }
    }

    /** Your apps this phone doesn't have yet, best first. */
    suspend fun apps(): List<PromoApp> = feed()?.let { pick(it, app.packageName, ::isInstalled) } ?: emptyList()

    /** The featured app, when it is due; call [markFeaturedShown] once you show it. */
    suspend fun featured(): FeaturedPromo? {
        val feed = feed() ?: return null
        val f = feed.featured ?: return null
        val promo = feed.apps.firstOrNull { it.packageName == f.packageName } ?: return null
        if (promo.packageName == app.packageName || isInstalled(promo.packageName)) return null
        val due = featuredDue(
            lastShownAt = prefs.getLong(KEY_FEATURED_AT + promo.packageName, 0L),
            now = System.currentTimeMillis(),
            restMs = featuredRestMs,
            dismissed = prefs.getBoolean(KEY_DISMISSED + promo.packageName, false),
        )
        if (!due) return null
        return FeaturedPromo(
            app = promo,
            title = f.title ?: app.getString(R.string.kit_featured_title, promo.name),
            message = f.message ?: promo.tagline,
        )
    }

    fun markFeaturedShown(promo: PromoApp) {
        prefs.edit().putLong(KEY_FEATURED_AT + promo.packageName, System.currentTimeMillis()).apply()
    }

    /** The user said no thanks: never feature this app on this phone again. */
    fun dismiss(promo: PromoApp) {
        prefs.edit().putBoolean(KEY_DISMISSED + promo.packageName, true).apply()
    }

    fun isInstalled(packageName: String): Boolean = try {
        app.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** "Get": the Play app when the link is a Play page, the browser otherwise. */
    fun open(context: Context, promo: PromoApp) {
        val web = promo.storeUrl(from = app.packageName)
        val inPlay = promo.url == null && runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(web.replace("https://play.google.com/store/apps/details", "market://details")))
                    .setPackage("com.android.vending"),
            )
        }.isSuccess
        if (!inPlay) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web))) }
            .onFailure { Log.w(TAG, "Nothing can open $web") }
    }

    /** Opens the plain-View "More apps" screen ([MoreAppsActivity]). Compose apps use `MoreAppsList`. */
    fun openList(context: Context) {
        context.startActivity(
            Intent(context, MoreAppsActivity::class.java)
                .putExtra(MoreAppsActivity.EXTRA_URL, url)
                .putExtra(MoreAppsActivity.EXTRA_BUNDLED_JSON, bundledJson),
        )
    }

    /** The plain-dialog version of the featured promo, for View-based apps. Marks it shown. */
    fun showFeaturedDialog(activity: Activity, featured: FeaturedPromo) {
        if (activity.isFinishing) return
        markFeaturedShown(featured.app)
        KitTheme.dialog(activity)
            .setTitle(featured.title)
            .setMessage(featured.message)
            .setPositiveButton(R.string.kit_get) { _, _ -> open(activity, featured.app) }
            .setNegativeButton(R.string.kit_not_now, null)
            .let { KitTheme.show(it) }
    }

    companion object {
        private const val TAG = "PublishlyKit.AppPromos"
        private const val KEY_JSON = "json"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_FEATURED_AT = "featured_at:"
        private const val KEY_DISMISSED = "dismissed:"

        /** The list rule on its own: drop [self] and anything installed, best first (ties keep feed order). */
        fun pick(feed: PromoFeed, self: String, isInstalled: (String) -> Boolean): List<PromoApp> =
            feed.apps.filter { it.packageName != self && !isInstalled(it.packageName) }.sortedByDescending { it.priority }

        /** The featured rule on its own. [lastShownAt] is 0 while never shown. */
        fun featuredDue(lastShownAt: Long, now: Long, restMs: Long, dismissed: Boolean): Boolean =
            !dismissed && (lastShownAt == 0L || now - lastShownAt >= restMs)

        /** Downloads the feed text. Throws when the server can't be reached. */
        suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode} from $url")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
