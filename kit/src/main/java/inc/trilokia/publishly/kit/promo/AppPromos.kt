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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** One app of the feed, with whether this phone already has it. */
data class PromoAppEntry(val app: PromoApp, val installed: Boolean)

/**
 * Promotes your other apps from a JSON feed on your server ([PromoFeed] has the format). No ad
 * network, no SDK: the list is yours, and it changes when you edit the file.
 *
 * - [apps]: your apps that this phone doesn't have yet (the app itself and installed ones are
 *   dropped), best first. Show them in a "More apps from us" row or list.
 * - [allApps]: the same list with the installed apps kept at the end, each marked, for a list
 *   that also says "thanks" to people who already have them ([launch] opens one).
 * - [featured]: the one app the feed singles out, when it is due: not installed, not shown in
 *   the last [featuredRestMs], not dismissed for good. Show it in a dialog at a quiet moment.
 *
 * The feed is fetched at most once per [refreshMs] and kept in SharedPreferences, so it works
 * offline after the first load. [bundledJson] (a copy in `assets/`) is used until the server
 * answers the first time, or always when [url] is null.
 *
 * The lists are titled "More apps from us", or "More apps from Trilokia Inc." when a developer is
 * named: [developerName] here, or `"developer"` in the feed (see [title]).
 */
class AppPromos(
    context: Context,
    private val url: String?,
    private val bundledJson: String? = null,
    private val refreshMs: Long = TimeUnit.HOURS.toMillis(24),
    private val featuredRestMs: Long = TimeUnit.DAYS.toMillis(7),
    /** Your name for the title ("Trilokia Inc."); null takes the feed's `developer`, and without one the title is "More apps from us". */
    val developerName: String? = null,
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_promos", Context.MODE_PRIVATE)
    // The list and its title read the feed at the same moment; one fetch serves both
    private val feedLock = Mutex()

    /** The whole feed, from the server, the cache or the bundled copy; null when there is none. */
    suspend fun feed(): PromoFeed? = feedLock.withLock {
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

    /** The developer the title names: [developerName], else the feed's `developer`, else null. */
    suspend fun developer(): String? = developerOf(developerName, feed())

    /** "More apps from Trilokia Inc.", or "More apps from us" when no developer is named. */
    suspend fun title(): String = titleFor(developer())

    /** [title] for a known [developer], without reading the feed. */
    fun titleFor(developer: String?): String =
        developer?.let { app.getString(R.string.kit_more_apps_from, it) } ?: app.getString(R.string.kit_more_apps)

    /** Your apps this phone doesn't have yet, best first. */
    suspend fun apps(): List<PromoApp> = feed()?.let { pick(it, app.packageName, ::isInstalled) } ?: emptyList()

    /** Every app in the feed but this one: the ones not installed first, then the installed ones, best first in each. */
    suspend fun allApps(): List<PromoAppEntry> = feed()?.let { arrange(it, app.packageName, ::isInstalled) } ?: emptyList()

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

    /**
     * Opens an installed app at its launcher screen. Falls back to [open] (the Play page) when it
     * has no launcher screen, which add-on apps often don't.
     */
    fun launch(context: Context, promo: PromoApp) {
        val intent = context.packageManager.getLaunchIntentForPackage(promo.packageName)
        if (intent == null || runCatching { context.startActivity(intent) }.isFailure) open(context, promo)
    }

    /** Opens the plain-View "More apps" screen ([MoreAppsActivity]). Compose apps use `MoreAppsList`. */
    fun openList(context: Context) {
        context.startActivity(
            Intent(context, MoreAppsActivity::class.java)
                .putExtra(MoreAppsActivity.EXTRA_URL, url)
                .putExtra(MoreAppsActivity.EXTRA_BUNDLED_JSON, bundledJson)
                .putExtra(MoreAppsActivity.EXTRA_DEVELOPER, developerName),
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

        /**
         * The full-list rule on its own: drop [self], keep the rest with their installed flag, the
         * not-installed ones first, then the installed ones; best first within each group, ties in
         * feed order.
         */
        fun arrange(feed: PromoFeed, self: String, isInstalled: (String) -> Boolean): List<PromoAppEntry> =
            feed.apps.filter { it.packageName != self }
                .map { PromoAppEntry(it, isInstalled(it.packageName)) }
                .sortedWith(compareBy<PromoAppEntry> { it.installed }.thenByDescending { it.app.priority })

        /** Which name the title uses: the one given in code, else the feed's; blank counts as none. */
        fun developerOf(explicit: String?, feed: PromoFeed?): String? =
            explicit?.trim()?.ifEmpty { null } ?: feed?.developer?.trim()?.ifEmpty { null }

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
