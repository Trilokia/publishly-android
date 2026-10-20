package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
 * How the kit's own prompts are drawn: the update and review dialogs and sheets, the featured-app
 * and what's-new dialogs, the restart bar. Start from [PromptDefaults.style] and change only what
 * you want; everything defaults to the app's MaterialTheme, so a style with no arguments is the
 * look the kit has always had. For a look the style can't reach, give the prompt a `content`
 * lambda instead and draw it yourself (see the README, "Draw the prompts yourself").
 */
@Immutable
class PromptStyle(
    /** The dialog window. */
    val containerColor: Color,
    val titleColor: Color,
    val bodyColor: Color,
    /** Buttons, and the stars. */
    val accentColor: Color,
    val shape: Shape,
    val tonalElevation: Dp,
    val title: TextStyle,
    val body: TextStyle,
    /** Button labels. */
    val label: TextStyle,
    val starEmptyColor: Color,
    /** The bottom-sheet versions. */
    val sheetContainerColor: Color,
    val sheetContentColor: Color,
    val sheetShape: Shape,
    val sheetTitle: TextStyle,
    /** The "update downloaded, restart" bar. */
    val barContainerColor: Color,
    val barContentColor: Color,
    val barText: TextStyle,
)

object PromptDefaults {
    /** The default look: Material 3's dialog, sheet and buttons, in the app's colour scheme and type. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun style(
        containerColor: Color = AlertDialogDefaults.containerColor,
        titleColor: Color = AlertDialogDefaults.titleContentColor,
        bodyColor: Color = AlertDialogDefaults.textContentColor,
        accentColor: Color = MaterialTheme.colorScheme.primary,
        shape: Shape = AlertDialogDefaults.shape,
        tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
        title: TextStyle = MaterialTheme.typography.headlineSmall,
        body: TextStyle = MaterialTheme.typography.bodyMedium,
        label: TextStyle = MaterialTheme.typography.labelLarge,
        starEmptyColor: Color = MaterialTheme.colorScheme.outlineVariant,
        sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
        sheetContentColor: Color = MaterialTheme.colorScheme.onSurface,
        sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
        sheetTitle: TextStyle = MaterialTheme.typography.titleLarge,
        barContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        barContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        barText: TextStyle = MaterialTheme.typography.bodyMedium,
    ): PromptStyle = PromptStyle(
        containerColor, titleColor, bodyColor, accentColor, shape, tonalElevation, title, body, label, starEmptyColor,
        sheetContainerColor, sheetContentColor, sheetShape, sheetTitle, barContainerColor, barContentColor, barText,
    )
}

/** A text button in the style's accent, for the prompts' secondary answers. */
@Composable
internal fun PromptTextButton(style: PromptStyle, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, text: @Composable () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier, colors = ButtonDefaults.textButtonColors(contentColor = style.accentColor)) {
        ProvideTextStyle(style.label, text)
    }
}

/** A filled button in the style's accent, for the prompts' main answer. */
@Composable
internal fun PromptButton(style: PromptStyle, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, text: @Composable () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = style.accentColor, contentColor = contentColorFor(style.accentColor)),
    ) {
        ProvideTextStyle(style.label, text)
    }
}

/**
 * The "update available" prompt for a JSON update, in the chosen [presentation]. [onDismiss]
 * runs when the user puts it away ("Later", a swipe, the snackbar timing out) or after "Update".
 * [UpdateFlow] shows this for you; use it directly when you drive the check yourself. [style]
 * changes the dialog's and sheet's look (the snackbar is the Scaffold's own).
 */
@Composable
fun UpdatePrompt(
    info: UpdateInfo,
    presentation: Presentation = Presentation.Dialog,
    cancelable: Boolean = true,
    snackbarHostState: SnackbarHostState? = null,
    style: PromptStyle = PromptDefaults.style(),
    onDismiss: () -> Unit,
) {
    when (presentation) {
        Presentation.Dialog -> UpdateDialog(info, onDismiss, cancelable, style)
        Presentation.BottomSheet -> UpdateSheet(info, onDismiss, cancelable, style)
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
fun UpdateSheet(info: UpdateInfo, onDismiss: () -> Unit, cancelable: Boolean = true, style: PromptStyle = PromptDefaults.style()) {
    val context = LocalContext.current
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { cancelable || it != SheetValue.Hidden },
    )
    ModalBottomSheet(
        onDismissRequest = { if (cancelable) onDismiss() },
        sheetState = state,
        containerColor = style.sheetContainerColor,
        contentColor = style.sheetContentColor,
        shape = style.sheetShape,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.kit_update_title), style = style.sheetTitle)
            Text(info.releaseNotes.ifBlank { stringResource(R.string.kit_update_version, info.versionName) }, style = style.body)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (cancelable) PromptTextButton(style, onClick = onDismiss) { Text(stringResource(R.string.kit_update_later)) }
                PromptButton(style, onClick = { JsonUpdates.open(context, info.url) }) { Text(stringResource(R.string.kit_update_now)) }
            }
        }
    }
}

/**
 * Our own "Enjoying the app?" prompt in the chosen [presentation], shown while [ReviewAsk.prompt]
 * is true (after [ReviewAsk.requestIfDue] or [ReviewAsk.requestPrompt]). The snackbar has room
 * for "Rate it" only: putting it away counts as "Not now".
 *
 * [style] changes the dialog's or sheet's colours, shape and type. [content] draws the prompt your
 * way instead, whatever the [presentation]: the kit draws nothing of its own and calls it while the
 * prompt is up, with the [ReviewPromptState] (stars, labels, `confirm()`, `later()`, `never()`).
 * Wrap it in your own `Dialog`, sheet or banner.
 */
@Composable
fun ReviewPrompt(
    reviewAsk: ReviewAsk,
    presentation: Presentation = Presentation.Dialog,
    snackbarHostState: SnackbarHostState? = null,
    style: PromptStyle = PromptDefaults.style(),
    content: (@Composable (ReviewPromptState) -> Unit)? = null,
) {
    if (content != null) {
        ReviewPromptDialog(reviewAsk, content = content)
        return
    }
    when (presentation) {
        Presentation.Dialog -> ReviewPromptDialog(reviewAsk, style)
        Presentation.BottomSheet -> ReviewPromptSheet(reviewAsk, style)
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
fun ReviewPromptSheet(reviewAsk: ReviewAsk, style: PromptStyle = PromptDefaults.style(), content: (@Composable (ReviewPromptState) -> Unit)? = null) {
    val s = rememberReviewPromptState(reviewAsk)
    if (!s.visible) return
    if (content != null) {
        content(s)
        return
    }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = s::later,
        sheetState = state,
        containerColor = style.sheetContainerColor,
        contentColor = style.sheetContentColor,
        shape = style.sheetShape,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(s.title, style = style.sheetTitle)
            ReviewPromptBody(s, style)
            // The answer that matters gets the full width, so "Rate on Play" stays on one line at any
            // font size; the two ways out share the line under it
            ReviewConfirmButton(s, filled = true, style = style, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PromptTextButton(style, onClick = s::never) { Text(s.neverLabel, maxLines = 1) }
                PromptTextButton(style, onClick = s::later) { Text(s.laterLabel, maxLines = 1) }
            }
        }
    }
}
