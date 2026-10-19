package inc.trilokia.publishly.kit.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.review.ReviewAsk.Answer

/**
 * Our own "Enjoying the app?" prompt, shown while [ReviewAsk.prompt] is true (after
 * [ReviewAsk.requestIfDue] or [ReviewAsk.requestPrompt]). "Rate it" asks Play for its review sheet
 * and opens the Play listing when Play refuses; "Not now" rests; "Don't ask again" stops for good.
 * Put it anywhere in the composition; it draws nothing until asked.
 */
@Composable
fun ReviewPromptDialog(reviewAsk: ReviewAsk) {
    val visible by reviewAsk.prompt.collectAsStateWithLifecycle()
    if (!visible) return
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
    AlertDialog(
        onDismissRequest = { reviewAsk.answer(activity, Answer.LATER) },
        title = { Text(stringResource(R.string.kit_review_title, appName)) },
        text = { Text(stringResource(R.string.kit_review_message)) },
        confirmButton = {
            TextButton(onClick = { reviewAsk.answer(activity, Answer.RATE) }) { Text(stringResource(R.string.kit_review_rate)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { reviewAsk.answer(activity, Answer.NEVER) }) { Text(stringResource(R.string.kit_review_never)) }
                TextButton(onClick = { reviewAsk.answer(activity, Answer.LATER) }) { Text(stringResource(R.string.kit_review_later)) }
            }
        },
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
