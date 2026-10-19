package inc.trilokia.publishly.kit.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.compose.FeaturedPromoDialog
import inc.trilokia.publishly.kit.compose.MoreAppsList
import inc.trilokia.publishly.kit.compose.MoreAppsRow
import inc.trilokia.publishly.kit.compose.OpenSourceScreen
import inc.trilokia.publishly.kit.compose.SyncKitTheme
import inc.trilokia.publishly.kit.compose.UpdateFlow
import inc.trilokia.publishly.kit.compose.UpdateReadyBar
import inc.trilokia.publishly.kit.compose.WhatsNewDialog
import inc.trilokia.publishly.kit.compose.WhatsNewList
import inc.trilokia.publishly.kit.oss.OpenSource
import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.review.AppReview
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.update.Updates
import inc.trilokia.publishly.kit.whatsnew.WhatsNew
import inc.trilokia.publishly.kit.whatsnew.WhatsNewSource
import kotlinx.coroutines.launch

/** Every feature of the library on one screen. */
class MainActivity : ComponentActivity() {
    private lateinit var updates: Updates

    private enum class Page { HOME, LICENCES, MORE_APPS, WHATS_NEW_PLAY, WHATS_NEW_GITHUB }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The whole update flow: Play first, the JSON file when Play can't answer. Built here because
        // it registers an activity-result launcher, which must happen before onStart.
        // The PGT apps read their JSON from trilokiainc.com; it says 193 > 1, so the dialog shows
        updates = Updates(this, jsonUrl = "https://www.trilokiainc.com/oldfree-update.json")

        val reviewAsk = ReviewAsk(this, minEvents = 1, minAgeMs = 0L)
        // Your own apps: the server copy when it exists, the bundled template (promo/apps.json) until then
        val promos = AppPromos(
            this,
            url = "https://www.trilokiainc.com/apps.json",
            bundledJson = assets.open("apps.json").bufferedReader().use { it.readText() },
        )

        // "What's new": PGT Free's Play listing (this sample isn't on Play), and a GitHub repo's commits
        val whatsNewPlay = WhatsNew(this, WhatsNewSource.Play("inc.trilokia.pubgfxtool.free"))
        val whatsNewGitHub = WhatsNew(this, WhatsNewSource.GitHubCommits("mikepenz", "AboutLibraries"))

        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                // The View screens and dialogs take this MaterialTheme's colours, light or dark
                SyncKitTheme()
                var page by remember { mutableStateOf(Page.HOME) }
                BackHandler(enabled = page != Page.HOME) { page = Page.HOME }

                // Dialogs take turns: the update flow first, then the featured app
                var updatesSettled by remember { mutableStateOf(false) }
                UpdateFlow(updates) { updatesSettled = true }
                if (updatesSettled) FeaturedPromoDialog(promos)
                // Shows once after a versionCode change (never on the first run); nothing otherwise
                WhatsNewDialog(whatsNewPlay)

                Scaffold(
                    bottomBar = { UpdateReadyBar(updates) },
                ) { padding ->
                    when (page) {
                        Page.LICENCES -> OpenSourceScreen(Modifier.padding(padding))
                        Page.MORE_APPS -> MoreAppsList(promos, Modifier.padding(padding))
                        Page.WHATS_NEW_PLAY -> WhatsNewList(whatsNewPlay, Modifier.padding(padding))
                        Page.WHATS_NEW_GITHUB -> WhatsNewList(whatsNewGitHub, Modifier.padding(padding))
                        Page.HOME -> Demo(
                            modifier = Modifier.padding(padding),
                            promos = promos,
                            onMoreAppsCompose = { page = Page.MORE_APPS },
                            onMoreAppsView = { promos.openList(this@MainActivity) },
                            onLicencesCompose = { page = Page.LICENCES },
                            onLicencesView = { OpenSource.open(this@MainActivity) },
                            onWhatsNewPlay = { page = Page.WHATS_NEW_PLAY },
                            onWhatsNewGitHub = { page = Page.WHATS_NEW_GITHUB },
                            onWhatsNewView = { whatsNewPlay.openList(this@MainActivity) },
                            onWhatsNewDialog = { whatsNewPlay.showDialog(this@MainActivity, whatsNewPlay.notes()) },
                            onCheckUpdate = { updates.play.checkNow { toast("Play update: $it") } },
                            onReview = {
                                reviewAsk.recordEvent()
                                if (!reviewAsk.askIfDue(this@MainActivity)) {
                                    toast("Not due; asking anyway for the demo")
                                    AppReview.ask(this@MainActivity)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

@Composable
private fun Demo(
    modifier: Modifier,
    promos: AppPromos,
    onMoreAppsCompose: () -> Unit,
    onMoreAppsView: () -> Unit,
    onLicencesCompose: () -> Unit,
    onLicencesView: () -> Unit,
    onWhatsNewPlay: () -> Unit,
    onWhatsNewGitHub: () -> Unit,
    onWhatsNewView: () -> Unit,
    onWhatsNewDialog: suspend () -> Unit,
    onCheckUpdate: () -> Unit,
    onReview: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Publishly Kit sample", style = MaterialTheme.typography.headlineSmall)
        // The own-apps row: from the JSON feed, minus whatever this phone already has
        MoreAppsRow(promos, Modifier.fillMaxWidth())
        Button(onClick = onMoreAppsCompose) { Text("All our apps (Compose list)") }
        Button(onClick = onMoreAppsView) { Text("All our apps (View screen)") }
        Button(onClick = onCheckUpdate) { Text("Check Play for updates") }
        Button(onClick = onReview) { Text("Ask for a review") }
        Button(onClick = onLicencesCompose) { Text("Open source licences (Compose)") }
        Button(onClick = onLicencesView) { Text("Open source licences (View screen)") }
        Button(onClick = onWhatsNewPlay) { Text("What's new from Play (Compose list)") }
        Button(onClick = onWhatsNewGitHub) { Text("What's new from GitHub commits (Compose list)") }
        Button(onClick = onWhatsNewView) { Text("What's new (View screen)") }
        Button(onClick = { scope.launch { onWhatsNewDialog() } }) { Text("What's new dialog (forced, for the demo)") }
    }
}
