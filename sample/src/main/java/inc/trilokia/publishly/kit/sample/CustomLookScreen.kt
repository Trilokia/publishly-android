package inc.trilokia.publishly.kit.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import inc.trilokia.publishly.kit.R as KitR
import inc.trilokia.publishly.kit.compose.FeaturedPromoDialog
import inc.trilokia.publishly.kit.compose.PromoIcon
import inc.trilokia.publishly.kit.compose.PromptDefaults
import inc.trilokia.publishly.kit.compose.PromptStyle
import inc.trilokia.publishly.kit.compose.ReviewPrompt
import inc.trilokia.publishly.kit.compose.ReviewPromptState
import inc.trilokia.publishly.kit.compose.StarRating
import inc.trilokia.publishly.kit.compose.UpdateFlow
import inc.trilokia.publishly.kit.compose.UpdatePrompt
import inc.trilokia.publishly.kit.compose.UpdateReadyBar
import inc.trilokia.publishly.kit.compose.WhatsNewDialog
import inc.trilokia.publishly.kit.compose.rememberReviewPromptState
import inc.trilokia.publishly.kit.compose.rememberWhatsNewDialogState
import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.promo.FeaturedPromo
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.update.UpdateInfo
import inc.trilokia.publishly.kit.update.UpdateState
import inc.trilokia.publishly.kit.update.Updates
import inc.trilokia.publishly.kit.whatsnew.ReleaseNote
import inc.trilokia.publishly.kit.whatsnew.WhatsNew
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * "Custom look": the two ways an app gives the kit's prompts its own look, side by side. The kit
 * keeps the logic either way (when each prompt shows, what each button does, the rests and marks).
 *
 * - **Kit dialogs, your style:** `PromptDefaults.style(...)` on the kit's own dialogs. Colours,
 *   shape and type change; the layout is the kit's.
 * - **Your own dialogs:** a `content` lambda on every prompt. The kit draws nothing; the lambda
 *   gets the data and the answers. Here each one is a deliberately plain box, so the point is the
 *   mechanism and not any one app's design.
 *
 * [reviewAsk] and [promos] are this screen's own (always due), so the prompts of the other screens
 * don't open at the same time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLookScreen(updates: Updates, reviewAsk: ReviewAsk, promos: AppPromos, whatsNew: WhatsNew, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var own by rememberSaveable { mutableStateOf(false) }
    val style = sampleStyle()

    // The prompts. Each draws nothing until the kit says it is due; the switch picks how it is drawn
    if (own) ReviewPrompt(reviewAsk) { s -> OwnReviewDialog(s) } else ReviewPrompt(reviewAsk, style = style)

    var forcedUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    forcedUpdate?.let { info ->
        if (own) UpdatePrompt(info, onDismiss = { forcedUpdate = null }) { i, cancelable, onUpdate, onLater -> OwnUpdateDialog(i, cancelable, onUpdate, onLater) }
        else UpdatePrompt(info, style = style, onDismiss = { forcedUpdate = null })
    }

    // UpdateFlow runs its check when it enters the composition; each tap mounts a fresh one
    var flowRuns by remember { mutableIntStateOf(0) }
    if (flowRuns > 0) key(flowRuns) {
        if (own) UpdateFlow(updates) { info, cancelable, onUpdate, onLater -> OwnUpdateDialog(info, cancelable, onUpdate, onLater) }
        else UpdateFlow(updates, style = style)
    }

    var featuredRuns by remember { mutableIntStateOf(0) }
    if (featuredRuns > 0) key(featuredRuns) {
        if (own) FeaturedPromoDialog(promos) { f, onGet, onNotNow -> OwnFeaturedDialog(f, onGet, onNotNow) }
        else FeaturedPromoDialog(promos, style = style)
    }

    // After an update this shows by itself; the button opens it on demand
    val news = rememberWhatsNewDialogState(whatsNew)
    if (own) WhatsNewDialog(news) { notes, onClose -> OwnWhatsNewDialog(news.title, notes, onClose) } else WhatsNewDialog(news, style = style)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom look") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choices(listOf("Kit dialogs, your style", "Your own dialogs"), if (own) 1 else 0) { own = it == 1 }
                    Text(
                        if (own) "Every prompt below is drawn by this app through its content slot. The kit only decides when it shows and what each button does."
                        else "The kit's own dialogs, with PromptDefaults.style(): a dark container, square corners, an amber accent and a serif title.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            item {
                Group("Prompts") {
                    Line("Rate this app", "ReviewPrompt") { reviewAsk.requestPrompt() }
                    HorizontalDivider()
                    Line("Update available", "UpdatePrompt, from the bundled update file") { scope.launch { forcedUpdate = updates.json?.check() } }
                    HorizontalDivider()
                    Line("Run the update flow", "UpdateFlow: Play first, the file when Play can't answer") { flowRuns++ }
                    HorizontalDivider()
                    Line("Featured app", "FeaturedPromoDialog; shows only while the phone doesn't have it") { featuredRuns++ }
                    HorizontalDivider()
                    Line("What's new", "WhatsNewDialog, opened on demand through its state") { scope.launch { news.show(whatsNew.notes().take(1)) } }
                }
            }
            item {
                Group("Restart bar") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("UpdateReadyBar appears here once Play has downloaded an update (a Play install only). The same bar, as a preview:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (own) UpdateReadyBar(updates) { onRestart -> OwnRestartBar(onRestart) } else UpdateReadyBar(updates, style = style)
                        if (own) OwnRestartBar(onRestart = {}) else PreviewRestartBar(style)
                    }
                }
            }
            item { UpdateStateCard(updates) { forcedUpdate = it } }
            item {
                Group("A rating, from scratch") {
                    InlineRating(reviewAsk)
                }
            }
        }
    }
}

/** The sample's style for the kit's dialogs: nothing like Material's defaults, so the change is easy to see. */
@Composable
private fun sampleStyle(): PromptStyle = PromptDefaults.style(
    containerColor = Color(0xFF1F2933),
    titleColor = Color(0xFFF5F7FA),
    bodyColor = Color(0xFFCBD2D9),
    accentColor = Color(0xFFF6B73C),
    shape = RoundedCornerShape(4.dp),
    title = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    starEmptyColor = Color(0xFF52606D),
    sheetContainerColor = Color(0xFF1F2933),
    sheetContentColor = Color(0xFFF5F7FA),
    sheetShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    sheetTitle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
    barContainerColor = Color(0xFFF6B73C),
    barContentColor = Color(0xFF1F2933),
)

/** The kit's bar as it would look, for the screen; the real one draws itself only when Play has downloaded. */
@Composable
private fun PreviewRestartBar(style: PromptStyle) {
    Row(
        Modifier.fillMaxWidth().background(style.barContainerColor, RoundedCornerShape(4.dp)).padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(KitR.string.kit_update_ready), style = style.barText, color = style.barContentColor, modifier = Modifier.weight(1f))
        Text(stringResource(KitR.string.kit_restart), style = style.label, color = style.barContentColor, modifier = Modifier.padding(12.dp))
    }
}

// ---- "Your own dialogs": plain boxes drawn with foundation only, no Material, no particular brand ----

private val Ink = Color(0xFF111111)
private val Paper = Color(0xFFFFFFFF)
private val Muted = Color(0xFF666666)

@Composable
private fun OwnDialog(onDismissRequest: () -> Unit, dismissible: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest, DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)) {
        Column(
            Modifier.fillMaxWidth().background(Paper).border(2.dp, Ink).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun OwnTitle(text: String, align: TextAlign = TextAlign.Start) =
    BasicText(text, Modifier.fillMaxWidth(), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = align))

@Composable
private fun OwnBody(text: String, color: Color = Ink, align: TextAlign = TextAlign.Start, modifier: Modifier = Modifier) =
    BasicText(text, modifier, style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, color = color, textAlign = align))

/** A boxed button; [main] fills it. */
@Composable
private fun OwnButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, main: Boolean = false, enabled: Boolean = true) {
    val ink = if (enabled) Ink else Muted
    Box(
        modifier
            .border(2.dp, ink)
            .background(if (main && enabled) Ink else Paper)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (main && enabled) Paper else ink), maxLines = 1)
    }
}

@Composable
private fun OwnLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    BasicText(text, modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp), style = TextStyle(fontSize = 15.sp, color = Muted), maxLines = 1)

@Composable
private fun OwnReviewDialog(s: ReviewPromptState) {
    OwnDialog(onDismissRequest = s::later) {
        OwnTitle(s.title)
        OwnBody(s.message)
        // The kit's star row, in plain colours; any star widget works, StarRating is just at hand
        StarRating(s.stars, s::setStars, Modifier.align(Alignment.CenterHorizontally), color = Ink, emptyColor = Muted)
        OwnBody(s.hint, color = Muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        OwnButton(s.confirmLabel, s::confirm, Modifier.fillMaxWidth(), main = true, enabled = s.canConfirm)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OwnLink(s.neverLabel, s::never)
            OwnLink(s.laterLabel, s::later)
        }
    }
}

@Composable
private fun OwnUpdateDialog(info: UpdateInfo, cancelable: Boolean, onUpdate: () -> Unit, onLater: () -> Unit) {
    OwnDialog(onDismissRequest = onLater, dismissible = cancelable) {
        OwnTitle(stringResource(KitR.string.kit_update_title))
        OwnBody(info.releaseNotes.ifBlank { stringResource(KitR.string.kit_update_version, info.versionName) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (cancelable) OwnLink(stringResource(KitR.string.kit_update_later), onLater, Modifier.padding(end = 8.dp))
            OwnButton(stringResource(KitR.string.kit_update_now), onUpdate, main = true)
        }
    }
}

@Composable
private fun OwnFeaturedDialog(featured: FeaturedPromo, onGet: () -> Unit, onNotNow: () -> Unit) {
    OwnDialog(onDismissRequest = onNotNow) {
        Box(Modifier.align(Alignment.CenterHorizontally).border(2.dp, Ink).padding(6.dp)) { PromoIcon(featured.app, size = 64.dp, corner = 0f) }
        OwnTitle(featured.title, align = TextAlign.Center)
        OwnBody(featured.message)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OwnLink(stringResource(KitR.string.kit_not_now), onNotNow, Modifier.padding(end = 8.dp))
            OwnButton(stringResource(KitR.string.kit_get), onGet, main = true)
        }
    }
}

@Composable
private fun OwnWhatsNewDialog(title: String, notes: List<ReleaseNote>, onClose: () -> Unit) {
    OwnDialog(onDismissRequest = onClose) {
        OwnTitle(title)
        notes.forEach { note ->
            if (note.heading.isNotBlank()) OwnBody(note.heading, color = Muted)
            OwnBody(note.notes)
        }
        OwnButton(stringResource(KitR.string.kit_ok), onClose, Modifier.align(Alignment.End), main = true)
    }
}

@Composable
private fun OwnRestartBar(onRestart: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Paper).border(2.dp, Ink).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        OwnBody(stringResource(KitR.string.kit_update_ready), modifier = Modifier.weight(1f))
        OwnButton(stringResource(KitR.string.kit_restart), onRestart, main = true)
    }
}

// ---- The state holders on the page itself ----

/**
 * `updates.state` as a "Check for updates" row would show it: one line per state, a progress bar
 * while Play downloads. "Check now" is `updates.checkNow()`; when the JSON file has the news,
 * [onServerUpdate] opens the update prompt in whichever way the switch says.
 */
@Composable
private fun UpdateStateCard(updates: Updates, onServerUpdate: (UpdateInfo) -> Unit) {
    val scope = rememberCoroutineScope()
    val state by updates.state.collectAsStateWithLifecycle()
    Group("Update state") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                when (val s = state) {
                    UpdateState.Idle -> "Not checked yet."
                    UpdateState.Checking -> "Checking…"
                    UpdateState.UpToDate -> "This is the latest version."
                    is UpdateState.Downloading -> s.fraction?.let { "Downloading the update: ${(it * 100).roundToInt()}%" } ?: "Downloading the update…"
                    UpdateState.Downloaded -> "Downloaded. Restart to finish."
                    is UpdateState.ServerUpdate -> "Version ${s.info.versionName} is available."
                    is UpdateState.Failed -> "Couldn't check. (${s.reason})"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            (state as? UpdateState.Downloading)?.let { d ->
                d.fraction?.let { LinearProgressIndicator(progress = { it }, Modifier.fillMaxWidth()) } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state == UpdateState.Checking) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    val s = updates.checkNow()
                    if (s is UpdateState.ServerUpdate) onServerUpdate(s.info)
                }
            }) { Text("Check now") }
        }
    }
}

/** The review prompt's state on a card of the page: the same rules and answers, no dialog at all. */
@Composable
private fun InlineRating(reviewAsk: ReviewAsk) {
    val s = rememberReviewPromptState(reviewAsk)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("rememberReviewPromptState(): the same stars, labels and answers, on the page instead of in a dialog.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(s.title, style = MaterialTheme.typography.titleMedium)
        StarRating(s.stars, s::setStars)
        Text(s.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = s::confirm, enabled = s.canConfirm) { Text(s.confirmLabel) }
    }
}

// ---- Page chrome, in the Home screen's manner ----

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Column(content = content) }
    }
}

@Composable
private fun Line(title: String, detail: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
