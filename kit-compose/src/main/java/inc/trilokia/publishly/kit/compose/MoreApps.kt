package inc.trilokia.publishly.kit.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.promo.FeaturedPromo
import inc.trilokia.publishly.kit.promo.PromoApp
import inc.trilokia.publishly.kit.promo.PromoAppEntry
import inc.trilokia.publishly.kit.promo.PromoIcons

/**
 * How [MoreAppsRow] and [MoreAppsList] draw each element. Start from [MoreAppsDefaults.style] and
 * change only what you want; everything defaults to the app's MaterialTheme. For a layout of your
 * own, pass a `row` to [MoreAppsList] or an `item` to [MoreAppsRow] instead.
 */
@Immutable
class MoreAppsStyle(
    /** The "More apps from us" heading of the row card. */
    val title: TextStyle,
    val titleColor: Color,
    /** The row card itself. */
    val cardColors: CardColors,
    val cardShape: Shape,
    /** Icon size in the row card, and in the list. */
    val rowIconSize: Dp,
    val listIconSize: Dp,
    /** Corner radius of every icon, as a fraction of its size (0.25 = Play-store rounded square). */
    val iconCorner: Float,
    /** The app name under the icon in the row card. */
    val rowName: TextStyle,
    /** The app name as the headline of a list entry. */
    val listName: TextStyle,
    val nameColor: Color,
    val tagline: TextStyle,
    val taglineColor: Color,
    val badge: TextStyle,
    val badgeContainer: Color,
    val badgeContent: Color,
    /** The "Get" button in the list; false hides it (the whole row still opens the store). */
    val showGetButton: Boolean,
    val rowItemWidth: Dp,
    val listItemColors: Color,
    val showDivider: Boolean,
    val dividerColor: Color,
    /** The "Installed · Thanks!" label on apps the phone already has; false leaves only the Open button. */
    val showInstalledLabel: Boolean,
    val installedContainer: Color,
    val installedContent: Color,
)

object MoreAppsDefaults {
    @Composable
    fun style(
        title: TextStyle = MaterialTheme.typography.titleMedium,
        titleColor: Color = MaterialTheme.colorScheme.onSurface,
        cardColors: CardColors = CardDefaults.cardColors(),
        cardShape: Shape = CardDefaults.shape,
        rowIconSize: Dp = 56.dp,
        listIconSize: Dp = 48.dp,
        iconCorner: Float = 0.25f,
        rowName: TextStyle = MaterialTheme.typography.labelMedium,
        listName: TextStyle = MaterialTheme.typography.bodyLarge,
        nameColor: Color = MaterialTheme.colorScheme.onSurface,
        tagline: TextStyle = MaterialTheme.typography.bodyMedium,
        taglineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        badge: TextStyle = MaterialTheme.typography.labelSmall,
        badgeContainer: Color = MaterialTheme.colorScheme.tertiaryContainer,
        badgeContent: Color = MaterialTheme.colorScheme.onTertiaryContainer,
        showGetButton: Boolean = true,
        rowItemWidth: Dp = 92.dp,
        listItemColors: Color = Color.Transparent,
        showDivider: Boolean = true,
        dividerColor: Color = DividerDefaults.color,
        showInstalledLabel: Boolean = true,
        installedContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
        installedContent: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    ): MoreAppsStyle = MoreAppsStyle(
        title, titleColor, cardColors, cardShape, rowIconSize, listIconSize, iconCorner, rowName, listName, nameColor,
        tagline, taglineColor, badge, badgeContainer, badgeContent, showGetButton, rowItemWidth,
        listItemColors, showDivider, dividerColor, showInstalledLabel, installedContainer, installedContent,
    )
}

/** How [MoreAppsList] arranges the apps. */
enum class MoreAppsLayout {
    /** One row per app: icon, name, tagline, a Get button. */
    List,
    /** Cards in columns: icon on top, name, tagline, Get. */
    Grid,
    /** One card per app, with a little air between them. */
    Cards,
    /** Icon and name only, dense; the whole row opens the store. */
    Compact,
}

/** How [MoreAppsRow] arranges the apps inside its card. */
enum class MoreAppsRowLayout {
    /** One line that scrolls sideways. */
    Scroll,
    /** As many lines as needed, so every app is visible. */
    Wrap,
}

/** Your apps this phone doesn't have yet, from [promos]; empty until the feed is read. */
@Composable
fun rememberPromoApps(promos: AppPromos): List<PromoApp> {
    val apps by produceState(initialValue = emptyList<PromoApp>(), promos) { value = promos.apps() }
    return apps
}

/**
 * Every app of the feed with its installed flag ([AppPromos.allApps]), or only the ones not
 * installed when [showInstalled] is false; empty until the feed is read.
 */
@Composable
fun rememberPromoEntries(promos: AppPromos, showInstalled: Boolean = true): List<PromoAppEntry> {
    val entries by produceState(initialValue = emptyList<PromoAppEntry>(), promos, showInstalled) {
        value = if (showInstalled) promos.allApps() else promos.apps().map { PromoAppEntry(it, installed = false) }
    }
    return entries
}

/** A promo app's icon, rounded; a soft square while it loads or when there is none. */
@Composable
fun PromoIcon(url: String?, modifier: Modifier = Modifier, size: Dp = 56.dp, corner: Float = 0.25f) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, url) { value = PromoIcons.load(context, url) }
    val shape = RoundedCornerShape(size * corner)
    val ready = bitmap
    if (ready != null) {
        Image(ready.asImageBitmap(), contentDescription = null, modifier = modifier.size(size).clip(shape))
    } else {
        Box(modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/**
 * A card with a scrolling row of your other apps (icon + name; tap opens Play, or the app when it
 * is installed). Draws nothing at all when there is nothing to show, so it can sit on the home
 * screen unconditionally. [showInstalled] keeps the apps the phone already has, after the others,
 * with an "Installed" label. [style] changes each element; [item] replaces one app's cell with
 * your own composable and gets the installed flag.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreAppsRow(
    promos: AppPromos,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.kit_more_apps),
    style: MoreAppsStyle = MoreAppsDefaults.style(),
    layout: MoreAppsRowLayout = MoreAppsRowLayout.Scroll,
    showInstalled: Boolean = true,
    item: (@Composable (app: PromoApp, installed: Boolean) -> Unit)? = null,
) {
    val entries = rememberPromoEntries(promos, showInstalled)
    if (entries.isEmpty()) return
    val context = LocalContext.current
    val cell: @Composable (PromoAppEntry) -> Unit = { entry ->
        if (item != null) item(entry.app, entry.installed)
        else PromoAppCell(entry.app, style, entry.installed) { promos.openOrLaunch(context, entry) }
    }
    Card(modifier.fillMaxWidth(), colors = style.cardColors, shape = style.cardShape) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(title, style = style.title, color = style.titleColor, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            when (layout) {
                MoreAppsRowLayout.Scroll -> LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(entries, key = { it.app.packageName }) { entry -> cell(entry) }
                }
                MoreAppsRowLayout.Wrap -> FlowRow(Modifier.padding(horizontal = 8.dp)) {
                    entries.forEach { entry -> cell(entry) }
                }
            }
        }
    }
}

/** The default cell of [MoreAppsRow]: icon with its badge, the name under it, the "Installed" label when it is. */
@Composable
fun PromoAppCell(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), installed: Boolean = false, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(style.rowItemWidth)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            PromoIcon(app.iconUrl, size = style.rowIconSize, corner = style.iconCorner)
            app.badge?.let { PromoBadge(it, style, Modifier.align(Alignment.TopEnd)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.name,
            style = style.rowName,
            color = style.nameColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        // The cell is narrow, so just "Installed" here; the lists have room for the thanks
        if (installed && style.showInstalledLabel) InstalledLabel(style, Modifier.padding(top = 4.dp), short = true)
    }
}

/**
 * The full-screen list of your apps. Put it under your own top bar. [layout] picks the
 * arrangement, [style] changes each element, [showInstalled] keeps the apps the phone already has
 * (after the others, with an "Installed" label and an Open button), and [row] replaces the whole
 * entry with your own composable, which gets the installed flag (in [MoreAppsLayout.Grid] it
 * becomes the cell).
 */
@Composable
fun MoreAppsList(
    promos: AppPromos,
    modifier: Modifier = Modifier,
    style: MoreAppsStyle = MoreAppsDefaults.style(),
    layout: MoreAppsLayout = MoreAppsLayout.List,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showInstalled: Boolean = true,
    row: (@Composable (app: PromoApp, installed: Boolean) -> Unit)? = null,
) {
    val entries = rememberPromoEntries(promos, showInstalled)
    val context = LocalContext.current
    val open: (PromoAppEntry) -> Unit = { promos.openOrLaunch(context, it) }
    when (layout) {
        MoreAppsLayout.List -> LazyColumn(modifier, contentPadding = contentPadding) {
            items(entries, key = { it.app.packageName }) { e ->
                if (row != null) row(e.app, e.installed) else PromoAppRow(e.app, style, e.installed) { open(e) }
                if (style.showDivider) HorizontalDivider(color = style.dividerColor)
            }
        }
        MoreAppsLayout.Compact -> LazyColumn(modifier, contentPadding = contentPadding) {
            items(entries, key = { it.app.packageName }) { e ->
                if (row != null) row(e.app, e.installed) else PromoAppCompactRow(e.app, style, e.installed) { open(e) }
                if (style.showDivider) HorizontalDivider(color = style.dividerColor)
            }
        }
        MoreAppsLayout.Cards -> LazyColumn(
            modifier,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries, key = { it.app.packageName }) { e ->
                if (row != null) row(e.app, e.installed) else PromoAppCard(e.app, style, e.installed) { open(e) }
            }
        }
        MoreAppsLayout.Grid -> LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries, key = { it.app.packageName }) { e ->
                if (row != null) row(e.app, e.installed) else PromoAppGridCell(e.app, style, e.installed) { open(e) }
            }
        }
    }
}

/** The [MoreAppsLayout.Compact] entry: icon and name, nothing else (plus the "Installed" label when it is). */
@Composable
fun PromoAppCompactRow(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), installed: Boolean = false, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PromoIcon(app.iconUrl, size = 40.dp, corner = style.iconCorner)
        Column(Modifier.weight(1f)) {
            Text(app.name, style = style.listName, color = style.nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (installed && style.showInstalledLabel) InstalledLabel(style, Modifier.padding(top = 2.dp))
        }
        app.badge?.let { PromoBadge(it, style) }
    }
}

/** The [MoreAppsLayout.Cards] entry: one elevated card per app; Open instead of Get when it is installed. */
@Composable
fun PromoAppCard(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), installed: Boolean = false, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen, shape = style.cardShape) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PromoIcon(app.iconUrl, size = style.rowIconSize, corner = style.iconCorner)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.name, style = style.listName, color = style.nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    app.badge?.let {
                        Spacer(Modifier.width(8.dp))
                        PromoBadge(it, style)
                    }
                }
                if (app.tagline.isNotBlank()) {
                    // One line for an installed app, so its label fits under it
                    Text(app.tagline, style = style.tagline, color = style.taglineColor, maxLines = if (installed) 1 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                if (installed && style.showInstalledLabel) InstalledLabel(style, Modifier.padding(top = 6.dp))
            }
            if (style.showGetButton) GetOrOpenButton(installed, onOpen)
        }
    }
}

/** The [MoreAppsLayout.Grid] cell: icon on top, name, tagline, Get (or the "Installed" label and Open). */
@Composable
fun PromoAppGridCell(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), installed: Boolean = false, onOpen: () -> Unit) {
    Card(onClick = onOpen, colors = style.cardColors, shape = style.cardShape) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                PromoIcon(app.iconUrl, size = 64.dp, corner = style.iconCorner)
                app.badge?.let { PromoBadge(it, style, Modifier.align(Alignment.TopEnd)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(app.name, style = style.listName, color = style.nameColor, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (app.tagline.isNotBlank()) {
                Text(app.tagline, style = style.tagline, color = style.taglineColor, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            if (installed && style.showInstalledLabel) InstalledLabel(style, Modifier.padding(top = 6.dp))
            if (style.showGetButton) {
                Spacer(Modifier.height(10.dp))
                GetOrOpenButton(installed, onOpen)
            }
        }
    }
}

/** The default entry of [MoreAppsList], for reuse inside your own list. */
@Composable
fun PromoAppRow(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), installed: Boolean = false, onOpen: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = { PromoIcon(app.iconUrl, size = style.listIconSize, corner = style.iconCorner) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.name,
                    style = style.listName,
                    color = style.nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                app.badge?.let {
                    Spacer(Modifier.width(8.dp))
                    PromoBadge(it, style)
                }
            }
        },
        supportingContent = if (app.tagline.isBlank() && !(installed && style.showInstalledLabel)) null else {
            {
                Column {
                    if (app.tagline.isNotBlank()) {
                        Text(app.tagline, style = style.tagline, color = style.taglineColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (installed && style.showInstalledLabel) InstalledLabel(style, Modifier.padding(top = 4.dp))
                }
            }
        },
        trailingContent = if (!style.showGetButton) null else {
            { GetOrOpenButton(installed, onOpen) }
        },
        colors = ListItemDefaults.colors(containerColor = style.listItemColors),
    )
}

/** The small "Installed · Thanks!" chip: a check and the text, on the style's installed colours. [short] drops the thanks, for narrow cells. */
@Composable
fun InstalledLabel(style: MoreAppsStyle = MoreAppsDefaults.style(), modifier: Modifier = Modifier, short: Boolean = false) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = style.installedContainer, contentColor = style.installedContent) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(stringResource(if (short) R.string.kit_installed else R.string.kit_installed_thanks), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Get" (filled tonal) for an app to install, "Open" (outlined) for one already on the phone. */
@Composable
private fun GetOrOpenButton(installed: Boolean, onClick: () -> Unit) {
    if (installed) OutlinedButton(onClick = onClick) { Text(stringResource(R.string.kit_open)) }
    else FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.kit_get)) }
}

/** The Play page for an app to install, the app itself when it is installed. */
private fun AppPromos.openOrLaunch(context: android.content.Context, entry: PromoAppEntry) {
    if (entry.installed) launch(context, entry.app) else open(context, entry.app)
}

/**
 * Shows the feed's featured app in a dialog when it is due (see [AppPromos.featured]), once per
 * composition. Place it on the home screen; it decides for itself whether to appear.
 */
@Composable
fun FeaturedPromoDialog(promos: AppPromos) {
    val context = LocalContext.current
    var featured by remember(promos) { mutableStateOf<FeaturedPromo?>(null) }
    LaunchedEffect(promos) { featured = promos.featured() }
    val f = featured ?: return
    // Counted as shown only now that it is really on screen
    LaunchedEffect(f) { promos.markFeaturedShown(f.app) }
    AlertDialog(
        onDismissRequest = { featured = null },
        icon = { PromoIcon(f.app.iconUrl, size = 64.dp) },
        title = { Text(f.title, textAlign = TextAlign.Center) },
        text = { Text(f.message) },
        confirmButton = {
            TextButton(onClick = { promos.open(context, f.app); featured = null }) { Text(stringResource(R.string.kit_get)) }
        },
        dismissButton = {
            TextButton(onClick = { featured = null }) { Text(stringResource(R.string.kit_not_now)) }
        },
    )
}

@Composable
private fun PromoBadge(text: String, style: MoreAppsStyle, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = style.badgeContainer,
        contentColor = style.badgeContent,
    ) {
        Text(text, style = style.badge, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
    }
}
