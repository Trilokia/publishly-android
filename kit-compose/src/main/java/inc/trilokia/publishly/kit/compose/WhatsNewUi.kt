package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.whatsnew.ReleaseNote
import inc.trilokia.publishly.kit.whatsnew.WhatsNew

/**
 * How [WhatsNewList] draws each element. Start from [WhatsNewDefaults.style] and change only what
 * you want; everything defaults to the app's MaterialTheme. For a layout of your own, pass a `row`
 * to [WhatsNewList] instead.
 */
@Immutable
class WhatsNewStyle(
    val version: TextStyle,
    val versionColor: Color,
    val date: TextStyle,
    val dateColor: Color,
    val notes: TextStyle,
    val notesColor: Color,
    /** Space between the version and the date on the heading line. */
    val headingGap: Dp,
    /** Space between the heading line and the notes. */
    val notesGap: Dp,
    val itemPadding: PaddingValues,
    val showDivider: Boolean,
    val dividerColor: Color,
    val empty: TextStyle,
    val emptyColor: Color,
)

object WhatsNewDefaults {
    /** The default look: version as a title, date small and quiet beside it, notes as body text. */
    @Composable
    fun style(
        version: TextStyle = MaterialTheme.typography.titleMedium,
        versionColor: Color = MaterialTheme.colorScheme.onSurface,
        date: TextStyle = MaterialTheme.typography.labelMedium,
        dateColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        notes: TextStyle = MaterialTheme.typography.bodyMedium,
        notesColor: Color = MaterialTheme.colorScheme.onSurface,
        headingGap: Dp = 10.dp,
        notesGap: Dp = 4.dp,
        itemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        showDivider: Boolean = true,
        dividerColor: Color = DividerDefaults.color,
        empty: TextStyle = MaterialTheme.typography.bodyLarge,
        emptyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): WhatsNewStyle = WhatsNewStyle(
        version, versionColor, date, dateColor, notes, notesColor,
        headingGap, notesGap, itemPadding, showDivider, dividerColor, empty, emptyColor,
    )
}

/** Every note from [whatsNew], newest first; null while loading, empty when nothing could be read. */
@Composable
fun rememberReleaseNotes(whatsNew: WhatsNew): List<ReleaseNote>? {
    val notes by produceState<List<ReleaseNote>?>(initialValue = null, whatsNew) { value = whatsNew.notes() }
    return notes
}

/**
 * The "What's new" list: version and date on one line, the notes under them; a tap opens the
 * entry's page when it has one. [style] changes each element; [row] replaces the whole entry with
 * your own composable (the divider still follows [WhatsNewStyle.showDivider]).
 */
@Composable
fun WhatsNewList(
    whatsNew: WhatsNew,
    modifier: Modifier = Modifier,
    style: WhatsNewStyle = WhatsNewDefaults.style(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    row: (@Composable (ReleaseNote) -> Unit)? = null,
) {
    val context = LocalContext.current
    val notes = rememberReleaseNotes(whatsNew)
    when {
        notes == null -> Box(modifier.fillMaxSize())
        notes.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.kit_whats_new_empty), style = style.empty, color = style.emptyColor)
        }
        else -> LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
            items(notes) { note ->
                if (row != null) {
                    row(note)
                } else {
                    ReleaseNoteRow(note, style, onClick = note.url?.let { url -> { JsonUpdates.open(context, url) } })
                }
                if (style.showDivider) HorizontalDivider(color = style.dividerColor)
            }
        }
    }
}

/** The default entry of [WhatsNewList], for reuse inside your own list. */
@Composable
fun ReleaseNoteRow(note: ReleaseNote, style: WhatsNewStyle = WhatsNewDefaults.style(), onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(style.itemPadding),
    ) {
        if (note.version != null || note.date != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(style.headingGap)) {
                note.version?.let { Text(it, style = style.version, color = style.versionColor) }
                note.date?.let { Text(it, style = style.date, color = style.dateColor) }
            }
        }
        Text(note.notes, style = style.notes, color = style.notesColor, modifier = Modifier.padding(top = style.notesGap))
    }
}

/**
 * Shows this version's notes once after an update ([WhatsNew.pending]) and marks them seen on
 * close. Draws nothing when nothing is pending. Put it anywhere in the composition.
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
