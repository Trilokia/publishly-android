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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 */
@Composable
fun ReviewPrompt(
    reviewAsk: ReviewAsk,
    presentation: Presentation = Presentation.Dialog,
    snackbarHostState: SnackbarHostState? = null,
) {
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

/** The bottom-sheet version of the review prompt, with all three answers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPromptSheet(reviewAsk: ReviewAsk) {
    val visible by reviewAsk.prompt.collectAsStateWithLifecycle()
    if (!visible) return
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var stars by rememberSaveable { mutableIntStateOf(0) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { reviewAsk.answer(activity, Answer.LATER) }, sheetState = state) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.kit_review_title, reviewAsk.appName), style = MaterialTheme.typography.titleLarge)
            ReviewPromptBody(reviewAsk, stars) { stars = it }
            // The answer that matters gets the full width, so "Rate on Play" stays on one line at any
            // font size; the two ways out share the line under it
            ReviewConfirmButton(reviewAsk, stars, filled = true, modifier = Modifier.fillMaxWidth()) { reviewAsk.answerRating(activity, stars) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { reviewAsk.answer(activity, Answer.NEVER) }) { Text(stringResource(R.string.kit_review_never), maxLines = 1) }
                TextButton(onClick = { reviewAsk.answer(activity, Answer.LATER) }) { Text(stringResource(R.string.kit_review_later), maxLines = 1) }
            }
        }
    }
}
