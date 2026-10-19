package inc.trilokia.publishly.kit.promo

import org.json.JSONObject
import java.net.URLEncoder

/** One of your own apps, as listed in the promo feed. */
data class PromoApp(
    val packageName: String,
    val name: String,
    val tagline: String = "",
    /** A square icon, 192–512 px. The Play listing's icon URL works. */
    val iconUrl: String? = null,
    /** Where "Get" goes when it isn't the Play page (your site, another store). */
    val url: String? = null,
    /** A short label on the card: "New", "Pro", "Free". */
    val badge: String? = null,
    /** Higher shows first. */
    val priority: Int = 0,
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
 *   "apps": [
 *     { "package": "inc.trilokia.cardjuggler", "name": "Card Juggler", "tagline": "...",
 *       "icon": "https://.../icon.png", "badge": "New", "priority": 100 }
 *   ],
 *   "featured": { "package": "inc.trilokia.cardjuggler", "title": "...", "message": "..." }
 * }
 * ```
 *
 * Only `package` and `name` are required; `featured` is optional. The repo's `promo/apps.json`
 * is a complete example.
 */
data class PromoFeed(val apps: List<PromoApp>, val featured: Featured?) {
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
                    url = a.optString("url").ifEmpty { null },
                    badge = a.optString("badge").ifEmpty { null },
                    priority = a.optInt("priority"),
                )
            }
            val featured = o.optJSONObject("featured")?.let {
                Featured(it.getString("package"), it.optString("title").ifEmpty { null }, it.optString("message").ifEmpty { null })
            }
            return PromoFeed(apps, featured)
        }
    }
}
