package inc.trilokia.publishly.kit.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.review.ReviewAsk.Answer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Our own "Enjoying the app?" prompt, shown while [ReviewAsk.prompt] is true (after
 * [ReviewAsk.requestIfDue] or [ReviewAsk.requestPrompt]). Five stars pop in; four or five of them
 * lead to Play's review sheet (or the Play listing when Play refuses), fewer open the mail app
 * with feedback to [ReviewAsk.feedbackEmail]. "Not now" rests; "Don't ask again" stops for good.
 * Put it anywhere in the composition; it draws nothing until asked.
 */
@Composable
fun ReviewPromptDialog(reviewAsk: ReviewAsk) {
    val visible by reviewAsk.prompt.collectAsStateWithLifecycle()
    if (!visible) return
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var stars by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = { reviewAsk.answer(activity, Answer.LATER) },
        title = { Text(stringResource(R.string.kit_review_title, reviewAsk.appName)) },
        text = { ReviewPromptBody(reviewAsk, stars) { stars = it } },
        confirmButton = { ReviewConfirmButton(reviewAsk, stars, filled = false) { reviewAsk.answerRating(activity, stars) } },
        dismissButton = {
            Row {
                TextButton(onClick = { reviewAsk.answer(activity, Answer.NEVER) }) { Text(stringResource(R.string.kit_review_never)) }
                TextButton(onClick = { reviewAsk.answer(activity, Answer.LATER) }) { Text(stringResource(R.string.kit_review_later)) }
            }
        },
    )
}

/** The message, the stars and the hint under them: shared by the dialog and the sheet. */
@Composable
internal fun ReviewPromptBody(reviewAsk: ReviewAsk, stars: Int, onStars: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.kit_review_message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        StarRating(stars, onStars)
        Text(
            text = when {
                stars == 0 -> stringResource(R.string.kit_review_tap_star)
                reviewAsk.answerFor(stars) == Answer.RATE -> stringResource(R.string.kit_review_hint_good)
                else -> stringResource(R.string.kit_review_hint_low)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Rate it" before a star is picked, then "Rate on Google Play" or "Send feedback". */
@Composable
internal fun ReviewConfirmButton(reviewAsk: ReviewAsk, stars: Int, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val label = when {
        stars == 0 -> stringResource(R.string.kit_review_rate)
        reviewAsk.answerFor(stars) == Answer.RATE -> stringResource(R.string.kit_review_rate_play)
        reviewAsk.answerFor(stars) == Answer.FEEDBACK -> stringResource(R.string.kit_review_send_feedback)
        else -> stringResource(R.string.kit_review_later)
    }
    if (filled) Button(onClick = onClick, enabled = stars > 0, modifier = modifier) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    else TextButton(onClick = onClick, enabled = stars > 0, modifier = modifier) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/**
 * Five tappable stars. They pop in one after another when first shown, and the chosen ones
 * bounce when tapped. [rating] is 0 to 5.
 */
@Composable
fun StarRating(
    rating: Int,
    onRating: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val scale = remember { Animatable(0f) }
            // Pop in, staggered
            LaunchedEffect(Unit) {
                delay(60L * i)
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            // Bounce when this star becomes part of the rating
            LaunchedEffect(rating) {
                if (rating >= i && scale.value >= 0.99f) {
                    delay(30L * (i - 1))
                    scale.animateTo(1.3f, tween(90))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            Star(
                filled = i <= rating,
                color = color,
                emptyColor = emptyColor,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRating(i) },
            )
        }
    }
}

@Composable
private fun Star(filled: Boolean, color: Color, emptyColor: Color, modifier: Modifier) {
    Canvas(modifier) {
        val path = starPath(size.minDimension / 2f, center = Offset(size.width / 2f, size.height / 2f))
        if (filled) drawPath(path, color) else drawPath(path, emptyColor, style = Stroke(width = size.minDimension * 0.07f))
    }
}

/** A five-point star, the outer points on a circle of [outer] radius. */
private fun starPath(outer: Float, center: Offset): Path {
    val inner = outer * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
