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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
 *
 * [style] changes the dialog's colours, shape and type. [content] draws the prompt your way
 * instead: the kit then draws nothing of its own (no dialog window, no Material surface) and calls
 * it while the prompt is up, with the [ReviewPromptState] that knows the stars, the labels and the
 * answers. Wrap it in your own `Dialog` or sheet.
 */
@Composable
fun ReviewPromptDialog(reviewAsk: ReviewAsk, style: PromptStyle = PromptDefaults.style(), content: (@Composable (ReviewPromptState) -> Unit)? = null) {
    val s = rememberReviewPromptState(reviewAsk)
    if (!s.visible) return
    if (content != null) {
        content(s)
        return
    }
    AlertDialog(
        onDismissRequest = s::later,
        title = { Text(s.title, style = style.title) },
        text = { ReviewPromptBody(s, style) },
        confirmButton = { ReviewConfirmButton(s, filled = false, style = style) },
        dismissButton = {
            Row {
                PromptTextButton(style, onClick = s::never) { Text(s.neverLabel) }
                PromptTextButton(style, onClick = s::later) { Text(s.laterLabel) }
            }
        },
        containerColor = style.containerColor,
        titleContentColor = style.titleColor,
        textContentColor = style.bodyColor,
        shape = style.shape,
        tonalElevation = style.tonalElevation,
    )
}

/**
 * The review prompt's state and answers, for drawing the prompt yourself. The `content` of
 * [ReviewPrompt], [ReviewPromptDialog] and [ReviewPromptSheet] gets one; [rememberReviewPromptState]
 * makes one for a prompt built from scratch. The kit keeps the logic: which way the stars go (Play
 * or feedback), what each answer does, the rest after "Not now" and the stop after "Don't ask again".
 */
@Stable
class ReviewPromptState internal constructor(
    /** The rules and answers behind this prompt. */
    val reviewAsk: ReviewAsk,
    private val context: Context,
    private val activity: Activity?,
    private val starsState: MutableIntState,
    private val visibleState: State<Boolean>,
) {
    /** True while the prompt should be on screen ([ReviewAsk.prompt]). */
    val visible: Boolean get() = visibleState.value

    /** The stars picked so far: 0 (none yet) to 5. */
    val stars: Int get() = starsState.intValue

    /** Picks [stars] (1 to 5; 0 clears them). Nothing is answered until [confirm]. */
    fun setStars(stars: Int) {
        starsState.intValue = stars.coerceIn(0, 5)
    }

    /** Where [confirm] goes with the stars picked: Play, feedback, or a rest; null before the first star. */
    val answer: Answer? get() = if (stars == 0) null else reviewAsk.answerFor(stars)

    /** False until a star is picked; the kit's own prompts grey out their main button until then. */
    val canConfirm: Boolean get() = stars > 0

    /** "Enjoying Sample App?" */
    val title: String = context.getString(R.string.kit_review_title, reviewAsk.appName)

    /** The line above the stars. */
    val message: String get() = context.getString(R.string.kit_review_message)

    /** The line under the stars: "Tap a star", then a word on where this rating goes. */
    val hint: String
        get() = context.getString(
            when (answer) {
                null -> R.string.kit_review_tap_star
                Answer.RATE -> R.string.kit_review_hint_good
                else -> R.string.kit_review_hint_low
            },
        )

    /**
     * The main button: "Rate it" before a star is picked, then "Rate on Play" or "Send feedback"
     * (or "Not now" for a low rating when there is no feedback address).
     */
    val confirmLabel: String
        get() = context.getString(
            when (answer) {
                null -> R.string.kit_review_rate
                Answer.RATE -> R.string.kit_review_rate_play
                Answer.FEEDBACK -> R.string.kit_review_send_feedback
                else -> R.string.kit_review_later
            },
        )

    /** "3 of 5 stars": what TalkBack reads for star [n] of a row you draw yourself. */
    fun starLabel(n: Int): String = context.getString(R.string.kit_review_star, n)

    /** "Not now", for [later]. */
    val laterLabel: String get() = context.getString(R.string.kit_review_later)

    /** "Don't ask again", for [never]. */
    val neverLabel: String get() = context.getString(R.string.kit_review_never)

    /** The main button: Play's sheet for a good rating, the mail app for a low one. With no star picked, Play. */
    fun confirm() {
        if (stars > 0) reviewAsk.answerRating(activity, stars) else reviewAsk.answer(activity, Answer.RATE)
    }

    /** "Not now": rests until the rule says it's due again. Back, a tap outside or a swipe away should do the same. */
    fun later() = reviewAsk.answer(activity, Answer.LATER)

    /** "Don't ask again": never again on this phone. */
    fun never() = reviewAsk.answer(activity, Answer.NEVER)
}

/**
 * The [ReviewPromptState] of [reviewAsk], for a prompt of your own: show it while
 * [ReviewPromptState.visible], call its answers from your buttons. Each new ask starts with no stars.
 */
@Composable
fun rememberReviewPromptState(reviewAsk: ReviewAsk): ReviewPromptState {
    val context = LocalContext.current
    val visible = reviewAsk.prompt.collectAsStateWithLifecycle()
    val stars = rememberSaveable(reviewAsk) { mutableIntStateOf(0) }
    LaunchedEffect(visible.value) { if (!visible.value) stars.intValue = 0 }
    return remember(reviewAsk, context, stars, visible) {
        ReviewPromptState(reviewAsk, context, context.findActivity(), stars, visible)
    }
}

/** The message, the stars and the hint under them: shared by the dialog and the sheet. */
@Composable
internal fun ReviewPromptBody(s: ReviewPromptState, style: PromptStyle) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(s.message, style = style.body, modifier = Modifier.fillMaxWidth())
        StarRating(s.stars, s::setStars, color = style.accentColor, emptyColor = style.starEmptyColor)
        Text(
            text = s.hint,
            style = MaterialTheme.typography.bodySmall,
            color = style.bodyColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Rate it" before a star is picked, then "Rate on Google Play" or "Send feedback". */
@Composable
internal fun ReviewConfirmButton(s: ReviewPromptState, filled: Boolean, style: PromptStyle, modifier: Modifier = Modifier) {
    val label = s.confirmLabel
    if (filled) PromptButton(style, onClick = s::confirm, enabled = s.canConfirm, modifier = modifier) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    else PromptTextButton(style, onClick = s::confirm, enabled = s.canConfirm, modifier = modifier) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
            val label = stringResource(R.string.kit_review_star, i)
            Star(
                filled = i <= rating,
                color = color,
                emptyColor = emptyColor,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .semantics {
                        contentDescription = label
                        role = Role.RadioButton
                        selected = i <= rating
                    }
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
