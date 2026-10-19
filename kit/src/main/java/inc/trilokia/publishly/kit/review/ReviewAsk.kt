package inc.trilokia.publishly.kit.review

import android.app.Activity
import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * When to ask for a Play rating. Counts "good moments" (an app launch, a settings apply, a saving
 * found: whatever means the app has just been useful) and says when the next [AppReview.ask] is
 * due. Play shows the sheet only a few times per user anyway, so the app must ration asks itself:
 *
 * - not before [minEvents] good moments (the first could be luck),
 * - not before the app has been on the phone for [minAgeMs],
 * - never twice inside [restMs],
 * - never more than [maxAsks] times on one phone. Whoever hasn't rated by then has answered.
 *
 * The defaults are the PGT apps' habit (ask from the second day on) tightened a little.
 * Everything is kept in SharedPreferences, so it survives app restarts but not a reinstall.
 */
class ReviewAsk(
    context: Context,
    private val minEvents: Int = 3,
    private val minAgeMs: Long = TimeUnit.DAYS.toMillis(2),
    private val restMs: Long = TimeUnit.DAYS.toMillis(60),
    private val maxAsks: Int = 4,
) {
    private val prefs = context.applicationContext.getSharedPreferences("kit_review_ask", Context.MODE_PRIVATE)

    init {
        if (prefs.getLong(KEY_FIRST_SEEN, 0L) == 0L) prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
    }

    /** One more good moment. Call it, then [askIfDue] (or [due]) right after. */
    fun recordEvent() {
        prefs.edit().putInt(KEY_EVENTS, prefs.getInt(KEY_EVENTS, 0) + 1).apply()
    }

    /** Should the next good moment ask? Doesn't change anything. */
    fun due(now: Long = System.currentTimeMillis()): Boolean = isDue(
        events = prefs.getInt(KEY_EVENTS, 0),
        asks = prefs.getInt(KEY_ASKS, 0),
        askedAt = prefs.getLong(KEY_ASKED_AT, 0L),
        firstSeenAt = prefs.getLong(KEY_FIRST_SEEN, now),
        now = now,
        minEvents = minEvents,
        minAgeMs = minAgeMs,
        restMs = restMs,
        maxAsks = maxAsks,
    )

    /** [AppReview.ask] when [due], and count the ask. Returns whether it asked. */
    fun askIfDue(activity: Activity): Boolean {
        if (!due()) return false
        markAsked()
        AppReview.ask(activity)
        return true
    }

    /** Record an ask made some other way (your own "Rate us" dialog, say). */
    fun markAsked(now: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putInt(KEY_ASKS, prefs.getInt(KEY_ASKS, 0) + 1)
            .putLong(KEY_ASKED_AT, now)
            .apply()
    }

    /** The user rated, or said never: stop asking on this phone. */
    fun stop() {
        prefs.edit().putInt(KEY_ASKS, Int.MAX_VALUE).apply()
    }

    companion object {
        private const val KEY_EVENTS = "events"
        private const val KEY_ASKS = "asks"
        private const val KEY_ASKED_AT = "asked_at"
        private const val KEY_FIRST_SEEN = "first_seen"

        /** The rule itself, with nothing stored, so it can be tested. [askedAt] is 0 while nothing has asked yet. */
        fun isDue(
            events: Int,
            asks: Int,
            askedAt: Long,
            firstSeenAt: Long,
            now: Long,
            minEvents: Int,
            minAgeMs: Long,
            restMs: Long,
            maxAsks: Int,
        ): Boolean =
            events >= minEvents &&
                asks < maxAsks &&
                now - firstSeenAt >= minAgeMs &&
                (askedAt == 0L || now - askedAt >= restMs)
    }
}
