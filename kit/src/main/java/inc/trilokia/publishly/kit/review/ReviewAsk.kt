package inc.trilokia.publishly.kit.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import inc.trilokia.publishly.kit.KitTheme
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.whatsnew.WhatsNew
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * When and how to ask for a Play rating. The flow:
 *
 * ```
 * a good moment ─► due? ─ yes ─► our own prompt "Enjoying the app?" with five stars
 *                                  ├─ 4 or 5 stars ─► Play's review sheet ─ Play refuses ─► the app's Play listing
 *                                  ├─ 1 to 3 stars ─► "Send feedback": the mail app, to [feedbackEmail], rating and device filled in
 *                                  ├─ Not now ─► rests [restMs]
 *                                  └─ Don't ask again ─► never again on this phone
 * ```
 *
 * Asking ourselves first costs nothing and protects Play's quota: Play shows its sheet only a few
 * times per user, so it should only be asked for people who have just said yes. And an unhappy
 * user gets to tell you, instead of the whole store.
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
 *
 * @param feedbackEmail where low ratings go. Null means a low rating just rests, like "Not now".
 * @param feedbackSubject the mail subject; the default names the app and its version.
 * @param goodRating the lowest star count that goes to Play (4: four and five stars).
 */
class ReviewAsk(
    context: Context,
    private val minEvents: Int = 3,
    private val minAgeMs: Long = TimeUnit.DAYS.toMillis(2),
    private val restMs: Long = TimeUnit.DAYS.toMillis(60),
    private val maxAsks: Int = 4,
    val feedbackEmail: String? = null,
    private val feedbackSubject: String? = null,
    val goodRating: Int = 4,
) {
    /** What the user said to our own prompt. */
    enum class Answer { RATE, FEEDBACK, LATER, NEVER }

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("kit_review_ask", Context.MODE_PRIVATE)
    private val _prompt = MutableStateFlow(false)

    /** True while our own prompt should be on screen. Compose apps render `ReviewPrompt(reviewAsk)`. */
    val prompt: StateFlow<Boolean> = _prompt.asStateFlow()

    /** The app's name, for "Enjoying X?". */
    val appName: String get() = app.applicationInfo.loadLabel(app.packageManager).toString()

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

    /** Compose apps: when [due], raises [prompt] (shown by `ReviewPrompt`) and counts the ask. */
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

    /** Which way [stars] goes: Play, or feedback (or just a rest when there is no feedback address). */
    fun answerFor(stars: Int): Answer = when {
        stars >= goodRating -> Answer.RATE
        feedbackEmail != null -> Answer.FEEDBACK
        else -> Answer.LATER
    }

    /** The user picked [stars]: Play for a good rating, the mail app for a low one. Remembers the rating. */
    fun answerRating(activity: Activity?, stars: Int): Answer {
        prefs.edit().putInt(KEY_RATING, stars).apply()
        val answer = answerFor(stars)
        answer(activity, answer, stars)
        return answer
    }

    /** The user answered the prompt. [activity] is where Play's sheet draws; null falls back to the listing. */
    fun answer(activity: Activity?, answer: Answer, stars: Int = 0) {
        _prompt.value = false
        when (answer) {
            Answer.RATE -> if (activity != null) AppReview.askOrOpenStore(activity) else AppReview.openStore(app)
            Answer.FEEDBACK -> sendFeedback(activity ?: app, stars)
            Answer.LATER -> {}
            Answer.NEVER -> stop()
        }
    }

    /** The last rating the user gave in our prompt, 1 to 5, or 0. */
    val lastRating: Int get() = prefs.getInt(KEY_RATING, 0)

    /**
     * Opens the mail app with a message to [feedbackEmail]: the subject, and a body that starts
     * with the rating and ends with the app version and the phone, so you know what you're
     * reading. Does nothing without an address or a mail app.
     */
    fun sendFeedback(context: Context, stars: Int = 0) {
        val to = feedbackEmail ?: return
        val version = WhatsNew.currentVersionName(app)
        val subject = feedbackSubject ?: app.getString(R.string.kit_feedback_subject, appName, version)
        val body = buildString {
            if (stars > 0) append(app.getString(R.string.kit_feedback_rating, stars)).append("\n\n")
            append(app.getString(R.string.kit_feedback_body)).append("\n\n\n---\n")
            append("$appName $version (${JsonUpdates.currentVersionCode(app)})\n")
            append("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n")
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "No mail app for feedback") }
    }

    /**
     * Our own prompt as a plain dialog in the Kit theme, for View apps: the question, five stars
     * that bounce in, and a button that reads "Rate on Google Play" or "Send feedback" once a star
     * is picked.
     */
    fun showPrompt(activity: Activity) {
        if (activity.isFinishing) return
        _prompt.value = true
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val colors = KitTheme.colors(activity)
        var stars = 0
        val hint = TextView(activity).apply {
            setText(R.string.kit_review_tap_star)
            setTextColor(colors.textSecondary)
            textSize = 13f
        }
        val ratingBar = RatingBar(activity).apply {
            numStars = 5
            stepSize = 1f
            rating = 0f
            scaleX = 0f
            scaleY = 0f
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(TextView(activity).apply {
                setText(R.string.kit_review_message)
                setTextColor(colors.text)
                textSize = 15f
            })
            addView(ratingBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            })
            addView(hint, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            })
        }
        val builder = KitTheme.dialog(activity)
            .setTitle(app.getString(R.string.kit_review_title, appName))
            .setView(content)
            .setPositiveButton(R.string.kit_review_rate) { _, _ -> if (stars > 0) answerRating(activity, stars) else answer(activity, Answer.RATE) }
            .setNegativeButton(R.string.kit_review_later) { _, _ -> answer(activity, Answer.LATER) }
            .setNeutralButton(R.string.kit_review_never) { _, _ -> answer(activity, Answer.NEVER) }
            .setOnCancelListener { answer(activity, Answer.LATER) }
        val dialog = KitTheme.show(builder)
        // The stars pop in, then bounce every time one is picked
        ratingBar.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(OvershootInterpolator(2.5f)).start()
        ratingBar.setOnRatingBarChangeListener { bar, value, fromUser ->
            if (!fromUser) return@setOnRatingBarChangeListener
            stars = value.toInt()
            bar.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction {
                bar.animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(OvershootInterpolator()).start()
            }.start()
            val good = answerFor(stars) == Answer.RATE
            hint.setText(if (good) R.string.kit_review_hint_good else R.string.kit_review_hint_low)
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setText(
                when (answerFor(stars)) {
                    Answer.RATE -> R.string.kit_review_rate_play
                    Answer.FEEDBACK -> R.string.kit_review_send_feedback
                    else -> R.string.kit_review_later
                },
            )
        }
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
        private const val TAG = "PublishlyKit.ReviewAsk"
        private const val KEY_EVENTS = "events"
        private const val KEY_ASKS = "asks"
        private const val KEY_ASKED_AT = "asked_at"
        private const val KEY_FIRST_SEEN = "first_seen"
        private const val KEY_RATING = "rating"

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
