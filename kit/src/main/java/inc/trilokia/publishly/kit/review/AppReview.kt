package inc.trilokia.publishly.kit.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * The in-app rating sheet, Play's own. Play draws it over the app and the user rates without
 * leaving; Play may also decide to show nothing at all, and never says which happened. So: fire
 * and forget, and a phone without a working Play (an emulator, a side load) simply never sees
 * it. Play ignores apps that ask too often, so ration the calls: see [ReviewAsk].
 */
object AppReview {
    fun ask(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnSuccessListener { info ->
            if (!activity.isFinishing) manager.launchReviewFlow(activity, info)
        }
        // No failure listener on purpose: not on Play means nothing to ask
    }
}
