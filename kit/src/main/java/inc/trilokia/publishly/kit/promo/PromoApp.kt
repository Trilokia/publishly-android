package inc.trilokia.publishly.kit.promo

import org.json.JSONObject
import java.net.URLEncoder

/** One of your own apps, as listed in the promo feed. */
data class PromoApp(
    val packageName: String,
    val name: String,
    val tagline: String = "",
    /**
     * The feed's `"icon"`: a square icon, 192–512 px, as a web URL (the Play listing's icon URL
     * works) or as a file in this app's `assets/` folder, `"asset:promo/cardjuggler.webp"`, which
     * shows even on a new install with no network. [PromoIcons] reads both.
     */
    val iconUrl: String? = null,
    /** Where "Get" goes when it isn't the Play page (your site, another store). */
    val url: String? = null,
    /** A short label on the card: "New", "Pro", "Free". */
    val badge: String? = null,
    /** Higher shows first. */
    val priority: Int = 0,
    /**
     * The feed's `"iconUrl"`: a web copy of the icon, tried when [iconUrl] is an asset this app
     * doesn't ship (one feed serves all your apps) or can't be read.
     */
    val iconFallbackUrl: String? = null,
) {
    /**
     * Where "Get" goes: [url], or the Play page. [from] (the promoting app's package) is attached
     * as the Play referrer, so the Play Console shows which of your apps sent each install.
     */
    fun storeUrl(from: String? = null): String {
        url?.let { return it }
        val page = "https://play.google.com/store/apps/details?id=$packageName"
        if (from == null) return page
        return page + "&referrer=" + URLEncoder.encode("utm_source=$from&utm_medium=kit", "UTF-8")
    }
}

/** The one app the feed wants pushed right now, with the words for its dialog. */
data class FeaturedPromo(val app: PromoApp, val title: String, val message: String)

/**
 * The feed: a JSON file on your server listing your apps. Change it any time; every app that
 * uses PublishlyKit picks the change up within a day. The format:
 *
 * ```json
 * {
 *   "developer": "Trilokia Inc.",
 *   "apps": [
 *     { "package": "inc.trilokia.cardjuggler", "name": "Card Juggler", "tagline": "...",
 *       "icon": "https://.../icon.png", "badge": "New", "priority": 100 },
 *     { "package": "...", "name": "...", "icon": "asset:promo/converter.webp", "iconUrl": "https://.../icon.png" }
 *   ],
 *   "featured": { "package": "inc.trilokia.cardjuggler", "title": "...", "message": "..." }
 * }
 * ```
 *
 * Only `package` and `name` are required; `featured` is optional. `icon` is a web URL or, for
 * a copy shipped inside the app, `asset:` and its path under `assets/`; `iconUrl` is the web copy
 * for when the asset isn't there. The repo's `promo/apps.json` is a complete example.
 */
data class PromoFeed(
    val apps: List<PromoApp>,
    val featured: Featured?,
    /** Your name, for the "More apps from …" title ("Trilokia Inc."); null when the feed doesn't say. */
    val developer: String? = null,
) {
    data class Featured(val packageName: String, val title: String?, val message: String?)

    companion object {
        /** Reads the JSON above. Throws on anything missing or malformed. */
        fun parse(json: String): PromoFeed {
            val o = JSONObject(json)
            val arr = o.getJSONArray("apps")
            val apps = (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                PromoApp(
                    packageName = a.getString("package"),
                    name = a.getString("name"),
                    tagline = a.optString("tagline"),
                    iconUrl = a.optString("icon").ifEmpty { null },
                    iconFallbackUrl = a.optString("iconUrl").ifEmpty { null },
                    url = a.optString("url").ifEmpty { null },
                    badge = a.optString("badge").ifEmpty { null },
                    priority = a.optInt("priority"),
                )
            }
            val featured = o.optJSONObject("featured")?.let {
                Featured(it.getString("package"), it.optString("title").ifEmpty { null }, it.optString("message").ifEmpty { null })
            }
            return PromoFeed(apps, featured, developer = o.optString("developer").trim().ifEmpty { null })
        }
    }
}
