package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.chipColors
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import inc.trilokia.publishly.kit.oss.OpenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * How [OpenSourceScreen] draws each element. Start from [OpenSourceDefaults.style] and change only
 * what you want. A null text style means the list's own default for that element (taken from the
 * app's MaterialTheme typography).
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

/**
 * The "Open source licences" list for Compose apps: AboutLibraries' Material 3 list, fed from the
 * `aboutlibraries.json` the Gradle plugin wrote for this app (see [OpenSource]). Tap a row for the
 * licence text. Put it on its own screen with your app's top bar. [style] changes each element;
 * [onLibraryClick] replaces the licence dialog with your own handling.
 */
@Composable
fun OpenSourceScreen(
    modifier: Modifier = Modifier,
    style: OpenSourceStyle = OpenSourceDefaults.style(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLibraryClick: ((Library) -> Unit)? = null,
) {
    val context = LocalContext.current
    val libraries by produceState<Libs?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { OpenSource.libraries(context) }
    }
    val libs = libraries
    if (libs == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
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
}
