package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.chipColors
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import inc.trilokia.publishly.kit.oss.R
import inc.trilokia.publishly.kit.R as KitR
import inc.trilokia.publishly.kit.oss.OpenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * How [OpenSourceScreen] draws each element. Start from [OpenSourceDefaults.style] and change only
 * what you want. A null text style means the default for that element (taken from the app's
 * MaterialTheme typography).
 */
@Immutable
class OpenSourceStyle(
    val name: TextStyle?,
    val nameMaxLines: Int,
    val version: TextStyle?,
    val author: TextStyle?,
    val description: TextStyle?,
    val licence: TextStyle?,
    val background: Color,
    val content: Color,
    /** The licence badge ("Apache License 2.0"). */
    val licenceBadge: Color,
    val onLicenceBadge: Color,
    /** The version, drawn as a quiet badge on the right. */
    val versionBadge: Color,
    val onVersionBadge: Color,
    val showAuthor: Boolean,
    val showVersion: Boolean,
    val showDescription: Boolean,
    val showLicenceBadges: Boolean,
    val itemPadding: PaddingValues,
    val itemSpacing: Dp,
    val showDivider: Boolean,
    val dividerColor: Color,
)

object OpenSourceDefaults {
    @Composable
    fun style(
        name: TextStyle? = null,
        nameMaxLines: Int = 1,
        version: TextStyle? = null,
        author: TextStyle? = null,
        description: TextStyle? = null,
        licence: TextStyle? = null,
        background: Color = MaterialTheme.colorScheme.background,
        content: Color = contentColorFor(background),
        licenceBadge: Color = MaterialTheme.colorScheme.primary,
        onLicenceBadge: Color = contentColorFor(licenceBadge),
        versionBadge: Color = background,
        onVersionBadge: Color = content,
        showAuthor: Boolean = true,
        showVersion: Boolean = true,
        showDescription: Boolean = false,
        showLicenceBadges: Boolean = true,
        itemPadding: PaddingValues = PaddingValues(16.dp),
        itemSpacing: Dp = 0.dp,
        showDivider: Boolean = false,
        dividerColor: Color = DividerDefaults.color,
    ): OpenSourceStyle = OpenSourceStyle(
        name, nameMaxLines, version, author, description, licence, background, content,
        licenceBadge, onLicenceBadge, versionBadge, onVersionBadge,
        showAuthor, showVersion, showDescription, showLicenceBadges, itemPadding, itemSpacing, showDivider, dividerColor,
    )
}

/** How [OpenSourceScreen] arranges the libraries. */
enum class OpenSourceLayout {
    /** AboutLibraries' own list: name, author, version badge, licence badge. */
    List,
    /** Name and licence on one line, dense. */
    Compact,
    /** One card per library. */
    Cards,
    /** Sections per licence, the libraries under each. */
    ByLicence,
}

/** The libraries of this build; null while loading. */
@Composable
fun rememberLibraries(): Libs? {
    val context = LocalContext.current
    val libraries by produceState<Libs?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { OpenSource.libraries(context) }
    }
    return libraries
}

/**
 * The "Open source licences" list for Compose apps, fed from the `aboutlibraries.json` the Gradle
 * plugin wrote for this app (see [OpenSource]). Tap a row for the licence text. Put it on its own
 * screen with your app's top bar. In the `kit-oss` module. [layout] picks the arrangement, [style] changes each element,
 * [onLibraryClick] replaces the licence dialog with your own handling, and [row] replaces the whole
 * entry in every layout but [OpenSourceLayout.List].
 */
@Composable
fun OpenSourceScreen(
    modifier: Modifier = Modifier,
    style: OpenSourceStyle = OpenSourceDefaults.style(),
    layout: OpenSourceLayout = OpenSourceLayout.List,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLibraryClick: ((Library) -> Unit)? = null,
    row: (@Composable (Library) -> Unit)? = null,
) {
    val libs = rememberLibraries()
    if (libs == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (layout == OpenSourceLayout.List) {
        LibrariesContainer(
            libraries = libs,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            showAuthor = style.showAuthor,
            showVersion = style.showVersion,
            showDescription = style.showDescription,
            showLicenseBadges = style.showLicenceBadges,
            colors = LibraryDefaults.libraryColors(
                libraryBackgroundColor = style.background,
                libraryContentColor = style.content,
                versionChipColors = LibraryDefaults.chipColors(containerColor = style.versionBadge, contentColor = style.onVersionBadge),
                licenseChipColors = LibraryDefaults.chipColors(containerColor = style.licenceBadge, contentColor = style.onLicenceBadge),
            ),
            padding = LibraryDefaults.libraryPadding(contentPadding = style.itemPadding),
            dimensions = LibraryDefaults.libraryDimensions(itemSpacing = style.itemSpacing),
            textStyles = LibraryDefaults.libraryTextStyles(
                nameTextStyle = style.name,
                nameMaxLines = style.nameMaxLines,
                versionTextStyle = style.version,
                authorTextStyle = style.author,
                descriptionTextStyle = style.description,
                licensesTextStyle = style.licence,
            ),
            onLibraryClick = onLibraryClick,
            divider = if (style.showDivider) {
                { HorizontalDivider(color = style.dividerColor) }
            } else {
                null
            },
        )
        return
    }

    // The other layouts are drawn here, with the same licence dialog on tap
    var open by remember { mutableStateOf<Library?>(null) }
    open?.let { LicenceDialog(it, style, onDismiss = { open = null }) }
    val click: (Library) -> Unit = onLibraryClick ?: { open = it }
    val libraries = libs.libraries
    Surface(modifier.fillMaxSize(), color = style.background, contentColor = style.content) {
        when (layout) {
            OpenSourceLayout.Compact -> LazyColumn(contentPadding = contentPadding) {
                items(libraries, key = { it.uniqueId }) { lib ->
                    if (row != null) row(lib) else LibraryCompactRow(lib, style) { click(lib) }
                    if (style.showDivider) HorizontalDivider(color = style.dividerColor)
                }
            }
            OpenSourceLayout.Cards -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(libraries, key = { it.uniqueId }) { lib ->
                    if (row != null) row(lib) else LibraryCard(lib, style) { click(lib) }
                }
            }
            OpenSourceLayout.ByLicence -> {
                val groups = remember(libraries) {
                    libraries.groupBy { it.licenses.firstOrNull()?.name ?: "" }
                        .toSortedMap(compareBy({ it.isEmpty() }, { it }))
                }
                LazyColumn(contentPadding = contentPadding) {
                    groups.forEach { (licence, libs) ->
                        item(key = "licence:$licence") {
                            Text(
                                text = if (licence.isEmpty()) stringResource(R.string.kit_licence_unknown) else licence,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
                            )
                        }
                        items(libs, key = { it.uniqueId }) { lib ->
                            if (row != null) row(lib) else LibraryCompactRow(lib, style, showLicence = false) { click(lib) }
                        }
                    }
                }
            }
            OpenSourceLayout.List -> {}
        }
    }
}

/** The [OpenSourceLayout.Compact] entry: name, author, and the licence at the end of the line. */
@Composable
fun LibraryCompactRow(library: Library, style: OpenSourceStyle = OpenSourceDefaults.style(), showLicence: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(library.name, library.artifactVersion.takeIf { style.showVersion }).joinToString("  "),
                style = style.name ?: MaterialTheme.typography.bodyLarge,
                maxLines = style.nameMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            val author = library.developers.mapNotNull { it.name }.joinToString(", ").ifBlank { library.organization?.name.orEmpty() }
            if (style.showAuthor && author.isNotBlank()) {
                Text(author, style = style.author ?: MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showLicence && style.showLicenceBadges) {
            library.licenses.firstOrNull()?.let { LicenceBadge(it.name, style) }
        }
    }
}

/** The [OpenSourceLayout.Cards] entry. */
@Composable
fun LibraryCard(library: Library, style: OpenSourceStyle = OpenSourceDefaults.style(), onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    library.name,
                    style = style.name ?: MaterialTheme.typography.titleMedium,
                    maxLines = style.nameMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (style.showVersion) {
                    library.artifactVersion?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = style.version ?: MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val author = library.developers.mapNotNull { it.name }.joinToString(", ").ifBlank { library.organization?.name.orEmpty() }
            if (style.showAuthor && author.isNotBlank()) {
                Text(author, style = style.author ?: MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (style.showDescription) {
                library.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = style.description ?: MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (style.showLicenceBadges && library.licenses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    library.licenses.forEach { LicenceBadge(it.name, style) }
                }
            }
        }
    }
}

/** The licence text of one library, the same dialog in every layout. */
@Composable
fun LicenceDialog(library: Library, style: OpenSourceStyle = OpenSourceDefaults.style(), onDismiss: () -> Unit) {
    val licence = library.licenses.firstOrNull()
    val text = licence?.licenseContent?.takeIf { it.isNotBlank() } ?: licence?.url ?: library.description.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(library.name) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(text, style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(KitR.string.kit_ok)) } },
        containerColor = style.background,
        textContentColor = style.content,
        titleContentColor = style.content,
    )
}

@Composable
private fun LicenceBadge(text: String, style: OpenSourceStyle) {
    Surface(shape = RoundedCornerShape(50), color = style.licenceBadge, contentColor = style.onLicenceBadge) {
        Text(text, style = style.licence ?: MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
