package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.whatsnew.ReleaseNote
import inc.trilokia.publishly.kit.whatsnew.WhatsNew

/** Every note from [whatsNew], newest first; null while loading, empty when nothing could be read. */
@Composable
fun rememberReleaseNotes(whatsNew: WhatsNew): List<ReleaseNote>? {
    val notes by produceState<List<ReleaseNote>?>(initialValue = null, whatsNew) { value = whatsNew.notes() }
    return notes
}

/** The "What's new" list: heading (version, date) and notes per entry; a tap opens the entry's page when it has one. */
@Composable
fun WhatsNewList(whatsNew: WhatsNew, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notes = rememberReleaseNotes(whatsNew)
    when {
        notes == null -> Box(modifier.fillMaxSize())
        notes.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.kit_whats_new_empty), style = MaterialTheme.typography.bodyLarge)
        }
        else -> LazyColumn(modifier.fillMaxSize()) {
            items(notes) { note ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(if (note.url != null) Modifier.clickable { JsonUpdates.open(context, note.url!!) } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Version in the heading weight; the date smaller and quieter, so the version stands out
                    if (note.version != null || note.date != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            note.version?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            note.date?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(note.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                HorizontalDivider()
            }
        }
    }
}

/**
 * Shows this version's notes once after an update ([WhatsNew.pending]) and marks them seen on
 * "OK". Draws nothing the rest of the time, so it can sit in the screen unconditionally.
 */
@Composable
fun WhatsNewDialog(whatsNew: WhatsNew) {
    val context = LocalContext.current
    var pending by remember(whatsNew) { mutableStateOf<List<ReleaseNote>>(emptyList()) }
    LaunchedEffect(whatsNew) { pending = whatsNew.pending() }
    if (pending.isEmpty()) return
    val close = {
        whatsNew.markSeen()
        pending = emptyList()
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.kit_whats_new_in, WhatsNew.currentVersionName(context))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(WhatsNew.text(pending))
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.kit_ok)) } },
    )
}
