package inc.trilokia.publishly.kit.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
    ): MoreAppsStyle = MoreAppsStyle(
        title, titleColor, cardColors, cardShape, rowIconSize, listIconSize, iconCorner, rowName, listName, nameColor,
        tagline, taglineColor, badge, badgeContainer, badgeContent, showGetButton, rowItemWidth,
        listItemColors, showDivider, dividerColor,
    )
}

/** Your apps this phone doesn't have yet, from [promos]; empty until the feed is read. */
@Composable
fun rememberPromoApps(promos: AppPromos): List<PromoApp> {
    val apps by produceState(initialValue = emptyList<PromoApp>(), promos) { value = promos.apps() }
    return apps
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
 * A card with a scrolling row of your other apps (icon + name; tap opens Play). Draws nothing
 * at all when there is nothing to show, so it can sit on the home screen unconditionally.
 * [style] changes each element; [item] replaces one app's cell with your own composable.
 */
@Composable
fun MoreAppsRow(
    promos: AppPromos,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.kit_more_apps),
    style: MoreAppsStyle = MoreAppsDefaults.style(),
    item: (@Composable (PromoApp) -> Unit)? = null,
) {
    val apps = rememberPromoApps(promos)
    if (apps.isEmpty()) return
    val context = LocalContext.current
    Card(modifier.fillMaxWidth(), colors = style.cardColors, shape = style.cardShape) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(title, style = style.title, color = style.titleColor, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    if (item != null) {
                        item(app)
                    } else {
                        Column(
                            modifier = Modifier
                                .width(style.rowItemWidth)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { promos.open(context, app) }
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
                        }
                    }
                }
            }
        }
    }
}

/**
 * The full-screen list: icon, name, tagline and a "Get" button per app. Put it under your own top
 * bar. [style] changes each element; [row] replaces the whole entry with your own composable.
 */
@Composable
fun MoreAppsList(
    promos: AppPromos,
    modifier: Modifier = Modifier,
    style: MoreAppsStyle = MoreAppsDefaults.style(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    row: (@Composable (PromoApp) -> Unit)? = null,
) {
    val apps = rememberPromoApps(promos)
    val context = LocalContext.current
    LazyColumn(modifier, contentPadding = contentPadding) {
        items(apps, key = { it.packageName }) { app ->
            if (row != null) {
                row(app)
            } else {
                PromoAppRow(app, style, onOpen = { promos.open(context, app) })
            }
            if (style.showDivider) HorizontalDivider(color = style.dividerColor)
        }
    }
}

/** The default entry of [MoreAppsList], for reuse inside your own list. */
@Composable
fun PromoAppRow(app: PromoApp, style: MoreAppsStyle = MoreAppsDefaults.style(), onOpen: () -> Unit) {
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
        supportingContent = if (app.tagline.isBlank()) null else {
            { Text(app.tagline, style = style.tagline, color = style.taglineColor, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = if (!style.showGetButton) null else {
            { FilledTonalButton(onClick = onOpen) { Text(stringResource(R.string.kit_get)) } }
        },
        colors = ListItemDefaults.colors(containerColor = style.listItemColors),
    )
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
