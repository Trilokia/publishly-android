package inc.trilokia.publishly.kit.promo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import inc.trilokia.publishly.kit.KitNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Promo icons, from inside the app or from the web, kept in memory for the session; web icons
 * are also kept on disk (the app's cache folder) across runs. Small on purpose, so the library
 * needs no image-loading dependency.
 *
 * An app's icon can come from two places, tried in order ([sources]): the feed's `"icon"`, and its
 * `"iconUrl"` when the first can't be had. Each is a web URL or `"asset:promo/x.webp"`, a file in
 * this app's `assets/` folder. An asset shows at once, even on a new install with no network; the
 * web copy covers the apps that don't ship that asset (one feed serves all your apps).
 */
object PromoIcons {
    /** One place an icon can come from. */
    sealed class Source {
        /** The memory-cache key, and what the feed wrote. */
        abstract val key: String

        /** A file in the app's `assets/` folder, by its path there ("promo/cardjuggler.webp"). */
        data class Asset(val path: String) : Source() {
            override val key get() = "asset:$path"
        }

        data class Web(val url: String) : Source() {
            override val key get() = url
        }
    }

    private val memory = LruCache<String, Bitmap>(32)

    /**
     * Reads one feed value: `asset:promo/x.webp` (also `asset:///promo/x.webp` and
     * `file:///android_asset/promo/x.webp`) is an asset, `https://…` or `http://…` a web icon.
     * Blank and anything else is null.
     */
    fun source(value: String?): Source? {
        val v = value?.trim().orEmpty()
        val asset = when {
            v.startsWith("asset:") -> v.removePrefix("asset:")
            v.startsWith("file:///android_asset/") -> v.removePrefix("file:///android_asset/")
            else -> null
        }
        if (asset != null) return asset.trimStart('/').takeIf { it.isNotBlank() }?.let { Source.Asset(it) }
        if (v.startsWith("https://", ignoreCase = true) || v.startsWith("http://", ignoreCase = true)) return Source.Web(v)
        return null
    }

    /** Where [app]'s icon can come from, in the order tried: its `icon`, then its `iconUrl`. */
    fun sources(app: PromoApp): List<Source> = listOfNotNull(source(app.iconUrl), source(app.iconFallbackUrl)).distinct()

    /** [app]'s icon if it is already in memory, for a first frame without a blank square. */
    fun cached(app: PromoApp): Bitmap? = sources(app).firstNotNullOfOrNull { memory.get(it.key) }

    /** [app]'s icon from the first of its [sources] that can be had, or null. Safe to call from the main thread. */
    suspend fun load(context: Context, app: PromoApp): Bitmap? = load(context, sources(app))

    /** The icon at [url] (a web URL or `asset:` path), or null when it can't be had right now. Safe to call from the main thread. */
    suspend fun load(context: Context, url: String?): Bitmap? = load(context, listOfNotNull(source(url)))

    private suspend fun load(context: Context, sources: List<Source>): Bitmap? {
        sources.firstNotNullOfOrNull { memory.get(it.key) }?.let { return it }
        if (sources.isEmpty()) return null
        val app = context.applicationContext
        return withContext(Dispatchers.IO) {
            sources.firstNotNullOfOrNull { source ->
                val bitmap = when (source) {
                    is Source.Asset -> runCatching { app.assets.open(source.path).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                    is Source.Web -> web(app, source.url)
                }
                bitmap?.also { memory.put(source.key, it) }
            }
        }
    }

    private fun web(context: Context, url: String): Bitmap? {
        val dir = File(context.cacheDir, "kit_icons").apply { mkdirs() }
        val file = File(dir, md5(url))
        // A web icon that isn't on disk yet needs the INTERNET permission; without it, the soft square stays
        if (!file.exists() && !KitNetwork.permitted(context)) return null
        val bitmap = runCatching {
            if (!file.exists()) download(url, file)
            BitmapFactory.decodeFile(file.path)
        }.getOrNull()
        if (bitmap == null) file.delete()
        return bitmap
    }

    private fun download(url: String, to: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode} from $url")
            val tmp = File(to.path + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(to)) throw IllegalStateException("Couldn't keep $to")
        } finally {
            conn.disconnect()
        }
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
