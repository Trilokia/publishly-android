package inc.trilokia.publishly.kit.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play's rating sheet, and the way out when Play won't give it.
 *
 * The sheet is Play's own: it draws over the app, the user rates without leaving, and Play may
 * also decide to show nothing at all (its quota) without ever saying which happened. What *can*
 * be detected is Play refusing the request outright: a side-loaded build, a phone without Play, a
 * Play error. [askOrOpenStore] covers those by opening the app's Play listing instead.
 * Play ignores apps that ask too often, so ration the calls: see [ReviewAsk].
 */
object AppReview {
    private const val TAG = "PublishlyKit.AppReview"

    /** Play's sheet, fire and forget. Not on Play means nothing happens. */
    fun ask(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnSuccessListener { info ->
            if (!activity.isFinishing) manager.launchReviewFlow(activity, info)
        }
    }

    /**
     * Play's sheet when Play gives it; the app's Play listing when Play refuses. Play accepting
     * the request and then quietly skipping the sheet is the one case nobody can detect.
     */
    fun askOrOpenStore(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnSuccessListener { info ->
                if (activity.isFinishing) return@addOnSuccessListener
                manager.launchReviewFlow(activity, info).addOnFailureListener { e ->
                    Log.i(TAG, "Review sheet failed to launch (${e.message}); opening the Play listing")
                    openStore(activity)
                }
            }
            .addOnFailureListener { e ->
                Log.i(TAG, "No review sheet (${e.message}); opening the Play listing")
                openStore(activity)
            }
    }

    /** The app's page on Play: in the Play app when there is one, otherwise in the browser. */
    fun openStore(context: Context) {
        val pkg = context.packageName
        val inPlay = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).setPackage("com.android.vending"))
        }.isSuccess
        if (!inPlay) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) }
                .onFailure { Log.w(TAG, "Nothing can open the Play listing") }
        }
    }
}
