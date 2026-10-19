package inc.trilokia.publishly.kit.promo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads promo icons and keeps them: in memory for the session, on disk (the app's cache
 * folder) across runs. Small on purpose, so the library needs no image-loading dependency.
 */
object PromoIcons {
    private val memory = LruCache<String, Bitmap>(32)

    /** The icon at [url], or null when it can't be had right now. Safe to call from the main thread. */
    suspend fun load(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        memory.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            val dir = File(context.applicationContext.cacheDir, "kit_icons").apply { mkdirs() }
            val file = File(dir, md5(url))
            val bitmap = runCatching {
                if (!file.exists()) download(url, file)
                BitmapFactory.decodeFile(file.path)
            }.getOrNull()
            if (bitmap == null) file.delete() else memory.put(url, bitmap)
            bitmap
        }
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
