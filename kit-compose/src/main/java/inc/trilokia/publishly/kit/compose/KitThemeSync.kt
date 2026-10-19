package inc.trilokia.publishly.kit.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import inc.trilokia.publishly.kit.KitColors
import inc.trilokia.publishly.kit.KitTheme

/**
 * Gives PublishlyKit's plain-View screens and dialogs the colours of the surrounding [MaterialTheme],
 * so they match the app's Compose UI, light and dark. Put it inside your theme, once:
 *
 * ```kotlin
 * MaterialTheme(colorScheme = scheme) {
 *     SyncKitTheme()
 *     ...
 * }
 * ```
 */
@Composable
fun SyncKitTheme() {
    val cs = MaterialTheme.colorScheme
    val colors = KitColors(
        // Material 3's top app bar sits on the surface, not on the primary colour
        topBar = cs.surfaceContainer.toArgb(),
        onTopBar = cs.onSurface.toArgb(),
        accent = cs.primary.toArgb(),
        background = cs.background.toArgb(),
        text = cs.onSurface.toArgb(),
        textSecondary = cs.onSurfaceVariant.toArgb(),
        surface = cs.surfaceContainerHigh.toArgb(),
    )
    SideEffect { KitTheme.colors = colors }
}
