package inc.trilokia.publishly.kit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import inc.trilokia.publishly.kit.oss.OpenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The "Open source licences" list for Compose apps: AboutLibraries' Material 3 list, fed from the
 * `aboutlibraries.json` the Gradle plugin wrote for this app (see [OpenSource]). Tap a row for the
 * licence text. Put it on its own screen with your app's top bar.
 */
@Composable
fun OpenSourceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val libraries by produceState<Libs?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { OpenSource.libraries(context) }
    }
    val libs = libraries
    if (libs == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LibrariesContainer(libs, modifier.fillMaxSize())
    }
}
