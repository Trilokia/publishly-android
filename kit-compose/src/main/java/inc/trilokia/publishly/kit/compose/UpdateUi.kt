package inc.trilokia.publishly.kit.compose

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
    val settled by rememberUpdatedState(onSettled)
    var pending by remember(updates) { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(updates) {
        val outcome = updates.check()
        if (outcome is Updates.Outcome.JsonUpdate) pending = outcome.info else settled()
    }
    pending?.let { info ->
        UpdatePrompt(info, presentation, cancelable, snackbarHostState, onDismiss = { pending = null; settled() })
    }
}

/** [UpdateReadyBar] for the combined flow: the same bar, fed by `updates.play`. */
@Composable
fun UpdateReadyBar(updates: Updates, modifier: Modifier = Modifier) = UpdateReadyBar(updates.play, modifier)

/**
 * "An update has been downloaded. Restart": shows itself once [PlayUpdates.downloaded] turns
 * true and nothing before. Put it above the bottom bar, or anywhere it won't cover a button.
 */
@Composable
fun UpdateReadyBar(updates: PlayUpdates, modifier: Modifier = Modifier) {
    val downloaded by updates.downloaded.collectAsStateWithLifecycle()
    if (!downloaded) return
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
