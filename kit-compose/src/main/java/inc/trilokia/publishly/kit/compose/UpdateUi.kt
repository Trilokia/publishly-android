package inc.trilokia.publishly.kit.compose

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.update.PlayUpdates
import inc.trilokia.publishly.kit.update.UpdateInfo
import inc.trilokia.publishly.kit.update.Updates

/**
 * The whole update flow for a Compose app: runs [Updates.check] once (Play first, the JSON file
 * when Play can't answer) and shows the update dialog when the JSON file is the one with news.
 * Play's side needs nothing here beyond [UpdateReadyBar]. [presentation] picks a dialog, a bottom sheet
 * or a snackbar (which needs the app's [snackbarHostState]). [onSettled] fires once there is nothing
 * (more) to show: at once for Play outcomes, after "Later" for the dialog. Use it to queue the
 * next start-up dialog (the featured app, what's new) behind this one.
 */
@Composable
fun UpdateFlow(
    updates: Updates,
    cancelable: Boolean = true,
    presentation: Presentation = Presentation.Dialog,
    snackbarHostState: SnackbarHostState? = null,
    onSettled: () -> Unit = {},
) {
    val s = rememberUpdateFlowState(updates, onSettled)
    s.info?.let { info ->
        UpdatePrompt(info, presentation, cancelable, snackbarHostState, onDismiss = s::later)
    }
}

/**
 * [UpdateFlow] with the prompt drawn by you: the same check, the same [onSettled], and [content]
 * instead of the kit's dialog. The kit draws nothing of its own; [content] is called while the JSON
 * file's newer version waits for an answer, with the version, whether it may be put away, and the
 * two answers (`onUpdate` opens the version's url and leaves the prompt up, `onLater` puts it away).
 *
 * ```kotlin
 * UpdateFlow(updates) { info, cancelable, onUpdate, onLater ->
 *     Dialog(onDismissRequest = { if (cancelable) onLater() }) { MyUpdateCard(info, onUpdate, onLater) }
 * }
 * ```
 */
@Composable
fun UpdateFlow(
    updates: Updates,
    cancelable: Boolean = true,
    onSettled: () -> Unit = {},
    content: @Composable (info: UpdateInfo, cancelable: Boolean, onUpdate: () -> Unit, onLater: () -> Unit) -> Unit,
) {
    val s = rememberUpdateFlowState(updates, onSettled)
    s.info?.let { info -> content(info, cancelable, s::update, s::later) }
}

/**
 * The update flow's state, for a prompt built from scratch: [rememberUpdateFlowState] runs the check
 * once, and [info] is the newer version while its prompt should be up.
 */
@Stable
class UpdateFlowState internal constructor(private val context: Context, private val onSettled: State<() -> Unit>) {
    /** The newer version your JSON file names, while its prompt should be up; null before the check, when there is no news, and after [later]. */
    var info: UpdateInfo? by mutableStateOf(null)
        internal set

    /** "Update": opens the version's url (a Play page or an APK). The prompt stays up, so a forced update can't be skipped. */
    fun update() {
        info?.let { JsonUpdates.open(context, it.url) }
    }

    /** "Later": puts the prompt away, and the flow is settled. */
    fun later() {
        if (info == null) return
        info = null
        onSettled.value()
    }
}

/**
 * Runs [Updates.check] once and keeps the answer in an [UpdateFlowState], for your own prompt.
 * [onSettled] fires once there is nothing (more) to show, as in [UpdateFlow].
 */
@Composable
fun rememberUpdateFlowState(updates: Updates, onSettled: () -> Unit = {}): UpdateFlowState {
    val context = LocalContext.current
    val settled = rememberUpdatedState(onSettled)
    val state = remember(updates) { UpdateFlowState(context, settled) }
    LaunchedEffect(updates) {
        val outcome = updates.check()
        if (outcome is Updates.Outcome.JsonUpdate) state.info = outcome.info else settled.value()
    }
    return state
}

/** [UpdateReadyBar] for the combined flow: the same bar, fed by `updates.play`. */
@Composable
fun UpdateReadyBar(updates: Updates, modifier: Modifier = Modifier, content: (@Composable (onRestart: () -> Unit) -> Unit)? = null) =
    UpdateReadyBar(updates.play, modifier, content)

/**
 * "An update has been downloaded. Restart": shows itself once [PlayUpdates.downloaded] turns
 * true and nothing before. Put it above the bottom bar, or anywhere it won't cover a button.
 * [content] draws the bar your way: called at the same moment, inside [modifier], with the
 * restart action (which installs the update and reopens the app).
 */
@Composable
fun UpdateReadyBar(updates: PlayUpdates, modifier: Modifier = Modifier, content: (@Composable (onRestart: () -> Unit) -> Unit)? = null) {
    val downloaded by updates.downloaded.collectAsStateWithLifecycle()
    if (!downloaded) return
    if (content != null) {
        Box(modifier) { content(updates::complete) }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.kit_update_ready),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { updates.complete() }) {
                Text(stringResource(R.string.kit_restart))
            }
        }
    }
}

/**
 * The Compose version of the JSON update prompt. Show it while [JsonUpdates.check] has returned an
 * [UpdateInfo]; [onDismiss] runs on "Later" or a tap outside (not when [cancelable] is false).
 */
@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit, cancelable: Boolean = true) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (cancelable) onDismiss() },
        title = { Text(stringResource(R.string.kit_update_title)) },
        text = {
            Text(info.releaseNotes.ifBlank { stringResource(R.string.kit_update_version, info.versionName) })
        },
        confirmButton = {
            TextButton(onClick = { JsonUpdates.open(context, info.url) }) {
                Text(stringResource(R.string.kit_update_now))
            }
        },
        dismissButton = if (cancelable) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.kit_update_later)) } }
        } else {
            null
        },
    )
}
