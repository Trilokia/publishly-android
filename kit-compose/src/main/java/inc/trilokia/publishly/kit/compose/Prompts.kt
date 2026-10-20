package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.review.ReviewAsk.Answer
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.update.UpdateInfo

/**
 * How a prompt reaches the user. [Dialog] is the default. [Snackbar] needs the app's
 * `SnackbarHostState` (the one its Scaffold draws); without one the prompt falls back to a dialog.
 */
enum class Presentation { Dialog, BottomSheet, Snackbar }

/**
 * The "update available" prompt for a JSON update, in the chosen [presentation]. [onDismiss]
 * runs when the user puts it away ("Later", a swipe, the snackbar timing out) or after "Update".
 * [UpdateFlow] shows this for you; use it directly when you drive the check yourself.
 */
@Composable
fun UpdatePrompt(
    info: UpdateInfo,
    presentation: Presentation = Presentation.Dialog,
    cancelable: Boolean = true,
    snackbarHostState: SnackbarHostState? = null,
    onDismiss: () -> Unit,
) {
    when (presentation) {
        Presentation.Dialog -> UpdateDialog(info, onDismiss, cancelable)
        Presentation.BottomSheet -> UpdateSheet(info, onDismiss, cancelable)
        Presentation.Snackbar -> {
            if (snackbarHostState == null) {
                UpdateDialog(info, onDismiss, cancelable)
                return
            }
            val context = LocalContext.current
            val message = stringResource(R.string.kit_update_version, info.versionName)
            val action = stringResource(R.string.kit_update_now)
            LaunchedEffect(info) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = action,
                    withDismissAction = cancelable,
                    duration = if (cancelable) SnackbarDuration.Long else SnackbarDuration.Indefinite,
                )
                if (result == SnackbarResult.ActionPerformed) JsonUpdates.open(context, info.url)
                onDismiss()
            }
        }
    }
}

/**
 * The update prompt drawn by you. The kit draws nothing of its own (no dialog window, no Material
 * surface); [content] gets the version, whether it may be put away, and the two answers: `onUpdate`
 * opens the version's url (a Play page or an APK) and leaves the prompt up, `onLater` is
 * [onDismiss]. Wrap it in your own `Dialog` or sheet, and let back or a tap outside call `onLater`
 * only when `cancelable`.
 */
@Composable
fun UpdatePrompt(
    info: UpdateInfo,
    cancelable: Boolean = true,
    onDismiss: () -> Unit,
    content: @Composable (info: UpdateInfo, cancelable: Boolean, onUpdate: () -> Unit, onLater: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    content(info, cancelable, { JsonUpdates.open(context, info.url) }, onDismiss)
}

/** The bottom-sheet version of the update prompt: title, release notes, Later / Update. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(info: UpdateInfo, onDismiss: () -> Unit, cancelable: Boolean = true) {
    val context = LocalContext.current
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { cancelable || it != SheetValue.Hidden },
    )
    ModalBottomSheet(onDismissRequest = { if (cancelable) onDismiss() }, sheetState = state) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.kit_update_title), style = MaterialTheme.typography.titleLarge)
            Text(
                info.releaseNotes.ifBlank { stringResource(R.string.kit_update_version, info.versionName) },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (cancelable) TextButton(onClick = onDismiss) { Text(stringResource(R.string.kit_update_later)) }
                Button(onClick = { JsonUpdates.open(context, info.url) }) { Text(stringResource(R.string.kit_update_now)) }
            }
        }
    }
}

/**
 * Our own "Enjoying the app?" prompt in the chosen [presentation], shown while [ReviewAsk.prompt]
 * is true (after [ReviewAsk.requestIfDue] or [ReviewAsk.requestPrompt]). The snackbar has room
 * for "Rate it" only: putting it away counts as "Not now".
 *
 * [content] draws the prompt your way instead, whatever the [presentation]: the kit draws nothing
 * of its own and calls it while the prompt is up, with the [ReviewPromptState] (stars, labels,
 * `confirm()`, `later()`, `never()`). Wrap it in your own `Dialog`, sheet or banner.
 */
@Composable
fun ReviewPrompt(
    reviewAsk: ReviewAsk,
    presentation: Presentation = Presentation.Dialog,
    snackbarHostState: SnackbarHostState? = null,
    content: (@Composable (ReviewPromptState) -> Unit)? = null,
) {
    if (content != null) {
        ReviewPromptDialog(reviewAsk, content)
        return
    }
    when (presentation) {
        Presentation.Dialog -> ReviewPromptDialog(reviewAsk)
        Presentation.BottomSheet -> ReviewPromptSheet(reviewAsk)
        Presentation.Snackbar -> {
            if (snackbarHostState == null) {
                ReviewPromptDialog(reviewAsk)
                return
            }
            val visible by reviewAsk.prompt.collectAsStateWithLifecycle()
            if (!visible) return
            val context = LocalContext.current
            val activity = remember(context) { context.findActivity() }
            val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
            val message = stringResource(R.string.kit_review_title, appName)
            val action = stringResource(R.string.kit_review_rate)
            LaunchedEffect(visible) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = action,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                reviewAsk.answer(activity, if (result == SnackbarResult.ActionPerformed) Answer.RATE else Answer.LATER)
            }
        }
    }
}

/**
 * The bottom-sheet version of the review prompt, with all three answers. [content] works as in
 * [ReviewPromptDialog]: with it the kit draws no sheet, only calls it while the prompt is up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPromptSheet(reviewAsk: ReviewAsk, content: (@Composable (ReviewPromptState) -> Unit)? = null) {
    val s = rememberReviewPromptState(reviewAsk)
    if (!s.visible) return
    if (content != null) {
        content(s)
        return
    }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = s::later, sheetState = state) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(s.title, style = MaterialTheme.typography.titleLarge)
            ReviewPromptBody(s)
            // The answer that matters gets the full width, so "Rate on Play" stays on one line at any
            // font size; the two ways out share the line under it
            ReviewConfirmButton(s, filled = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = s::never) { Text(s.neverLabel, maxLines = 1) }
                TextButton(onClick = s::later) { Text(s.laterLabel, maxLines = 1) }
            }
        }
    }
}
