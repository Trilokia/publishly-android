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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.R
import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.promo.FeaturedPromo
import inc.trilokia.publishly.kit.promo.PromoApp
import inc.trilokia.publishly.kit.promo.PromoIcons

/** Your apps this phone doesn't have yet, from [promos]; empty until the feed is read. */
@Composable
fun rememberPromoApps(promos: AppPromos): List<PromoApp> {
    val apps by produceState(initialValue = emptyList<PromoApp>(), promos) { value = promos.apps() }
    return apps
}

/** A promo app's icon, rounded; a soft square while it loads or when there is none. */
@Composable
fun PromoIcon(url: String?, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, url) { value = PromoIcons.load(context, url) }
    val shape = RoundedCornerShape(size / 4)
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
 */
@Composable
fun MoreAppsRow(
    promos: AppPromos,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.kit_more_apps),
) {
    val apps = rememberPromoApps(promos)
    if (apps.isEmpty()) return
    val context = LocalContext.current
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .width(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { promos.open(context, app) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            PromoIcon(app.iconUrl)
                            app.badge?.let { PromoBadge(it, Modifier.align(Alignment.TopEnd)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            app.name,
                            style = MaterialTheme.typography.labelMedium,
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

/** The full-screen list: icon, name, tagline and a "Get" button per app. Put it under your own top bar. */
@Composable
fun MoreAppsList(promos: AppPromos, modifier: Modifier = Modifier) {
    val apps = rememberPromoApps(promos)
    val context = LocalContext.current
    LazyColumn(modifier) {
        items(apps, key = { it.packageName }) { app ->
            ListItem(
                modifier = Modifier.clickable { promos.open(context, app) },
                leadingContent = { PromoIcon(app.iconUrl, size = 48.dp) },
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        app.badge?.let {
                            Spacer(Modifier.width(8.dp))
                            PromoBadge(it)
                        }
                    }
                },
                supportingContent = if (app.tagline.isBlank()) null else {
                    { Text(app.tagline, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                },
                trailingContent = {
                    FilledTonalButton(onClick = { promos.open(context, app) }) { Text(stringResource(R.string.kit_get)) }
                },
            )
            HorizontalDivider()
        }
    }
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
private fun PromoBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
    }
}
