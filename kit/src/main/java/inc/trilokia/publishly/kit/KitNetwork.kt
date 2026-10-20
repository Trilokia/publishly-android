package inc.trilokia.publishly.kit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Whether the kit may touch the network at all. The library declares `INTERNET`, but an app can
 * take it away (`tools:node="remove"` in its manifest), and then every fetch would fail and log a
 * warning on every start. So the permission is read once per process, and without it nothing is
 * fetched: the JSON update file, the promo feed, the icons and the what's-new sources answer from
 * their bundled copies and caches, as they do offline. Play's updates and review sheet are not
 * affected: they talk to the Play Store app, not the network.
 */
internal object KitNetwork {
    private const val TAG = "PublishlyKit"

    @Volatile
    private var permitted: Boolean? = null

    /** True when this app holds the `INTERNET` permission. Checked once; one log line when it doesn't. */
    fun permitted(context: Context): Boolean = permitted ?: run {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.i(TAG, "No INTERNET permission: nothing will be fetched; bundled copies and caches are used instead")
        permitted = granted
        granted
    }

    /**
     * The rule on its own: [fetch] runs only when [permitted]; otherwise the answer is null and
     * the caller falls back to what it has, with nothing logged. [fetch] may throw, as the fetch
     * functions do; that is the caller's to catch and log.
     */
    suspend fun <T> fetchIfPermitted(permitted: Boolean, fetch: suspend () -> T): T? = if (permitted) fetch() else null
}
