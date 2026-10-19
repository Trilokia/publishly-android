package inc.trilokia.publishly.kit.review

import android.app.Activity
import android.content.Context
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * When and how to ask for a Play rating. The flow:
 *
 * ```
 * a good moment ─► due? ─ yes ─► our own prompt "Enjoying the app?"
 *                                  ├─ Rate it ─► Play's review sheet ─ Play refuses ─► the app's Play listing
 *                                  ├─ Not now ─► rests [restMs]
 *                                  └─ Don't ask again ─► never again on this phone
 * ```
 *
 * Asking ourselves first costs nothing and protects Play's quota: Play shows its sheet only a few
 * times per user, so it should only be asked for people who have just said yes.
 *
 * "Good moments" (an app launch, a settings apply, a saving found: whatever means the app has just
 * been useful) are counted with [recordEvent]; the next ask is due:
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
    /** What the user said to our own prompt. */
    enum class Answer { RATE, LATER, NEVER }

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_review_ask", Context.MODE_PRIVATE)
    private val _prompt = MutableStateFlow(false)

    /** True while our own prompt should be on screen. Compose apps render `ReviewPromptDialog(reviewAsk)`. */
    val prompt: StateFlow<Boolean> = _prompt.asStateFlow()

    init {
        if (prefs.getLong(KEY_FIRST_SEEN, 0L) == 0L) prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
    }

    /** One more good moment. Call it, then [askIfDue] / [requestIfDue] right after. */
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

    /**
     * View apps: when [due], shows our own prompt as a plain dialog ([showPrompt]) and counts the
     * ask. With [ownPromptFirst] false it skips the prompt and goes straight to Play's sheet (or
     * the listing). Returns whether it asked.
     */
    fun askIfDue(activity: Activity, ownPromptFirst: Boolean = true): Boolean {
        if (!due()) return false
        markAsked()
        if (ownPromptFirst) showPrompt(activity) else AppReview.askOrOpenStore(activity)
        return true
    }

    /** Compose apps: when [due], raises [prompt] (shown by `ReviewPromptDialog`) and counts the ask. */
    fun requestIfDue(): Boolean {
        if (!due()) return false
        markAsked()
        _prompt.value = true
        return true
    }

    /** Raise the prompt regardless of the rule: a "Rate this app" row in Settings. */
    fun requestPrompt() {
        _prompt.value = true
    }

    /** The user answered the prompt. [activity] is where Play's sheet draws; null falls back to the listing. */
    fun answer(activity: Activity?, answer: Answer) {
        _prompt.value = false
        when (answer) {
            Answer.RATE -> if (activity != null) AppReview.askOrOpenStore(activity) else AppReview.openStore(app)
            Answer.LATER -> {}
            Answer.NEVER -> stop()
        }
    }

    /** Our own prompt as a plain dialog in the Kit theme, for View apps. */
    fun showPrompt(activity: Activity) {
        if (activity.isFinishing) return
        _prompt.value = true
        val name = app.applicationInfo.loadLabel(app.packageManager)
        val builder = KitTheme.dialog(activity)
            .setTitle(app.getString(R.string.kit_review_title, name))
            .setMessage(R.string.kit_review_message)
            .setPositiveButton(R.string.kit_review_rate) { _, _ -> answer(activity, Answer.RATE) }
            .setNegativeButton(R.string.kit_review_later) { _, _ -> answer(activity, Answer.LATER) }
            .setNeutralButton(R.string.kit_review_never) { _, _ -> answer(activity, Answer.NEVER) }
            .setOnCancelListener { answer(activity, Answer.LATER) }
        KitTheme.show(builder)
    }

    /** Record an ask made some other way. */
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
