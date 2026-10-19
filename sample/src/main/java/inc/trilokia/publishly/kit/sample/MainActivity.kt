package inc.trilokia.publishly.kit.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import inc.trilokia.publishly.kit.compose.FeaturedPromoDialog
import inc.trilokia.publishly.kit.compose.MoreAppsList
import inc.trilokia.publishly.kit.compose.MoreAppsRow
import inc.trilokia.publishly.kit.compose.OpenSourceScreen
import inc.trilokia.publishly.kit.compose.ReviewPromptDialog
import inc.trilokia.publishly.kit.compose.UpdateFlow
import inc.trilokia.publishly.kit.compose.UpdateReadyBar
import inc.trilokia.publishly.kit.compose.WhatsNewDialog
import inc.trilokia.publishly.kit.compose.WhatsNewList
import inc.trilokia.publishly.kit.oss.OpenSource
import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.review.ReviewAsk
import inc.trilokia.publishly.kit.update.Updates
import inc.trilokia.publishly.kit.whatsnew.WhatsNew
import inc.trilokia.publishly.kit.whatsnew.WhatsNewSource
import kotlinx.coroutines.launch

/**
 * Every feature of the library in one Material You app, laid out the Now in Android way: a top
 * app bar, bottom navigation between Home, More apps, What's new and Licences, and grouped list
 * rows on Home for the actions.
 */
class MainActivity : ComponentActivity() {
    private lateinit var updates: Updates

    private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
        HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
        APPS("More apps", Icons.Outlined.Apps, Icons.Filled.Apps),
        NEWS("What's new", Icons.Outlined.NewReleases, Icons.Filled.NewReleases),
        LICENCES("Licences", Icons.Outlined.Description, Icons.Filled.Description),
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The whole update flow: Play first, the JSON file when Play can't answer. Built here because
        // it registers an activity-result launcher, which must happen before onStart. The sample
        // has no server, so it ships a sample update.json in assets/ (it offers 1.2 to this 1.1 build, so the
        // dialog shows); a real app passes jsonUrl = "https://your-site/update.json"
        updates = Updates(this, jsonUrl = null, bundledJson = asset("update.json"))

        // Always due, so the prompt shows on every tap (a real app keeps the defaults)
        val reviewAsk = ReviewAsk(this, minEvents = 1, minAgeMs = 0L, restMs = 0L, maxAsks = Int.MAX_VALUE)
        // Your own apps: the server copy when it exists, the bundled template (promo/apps.json) until then
        val promos = AppPromos(
            this,
            url = "https://www.trilokiainc.com/apps.json",
            bundledJson = asset("apps.json"),
        )
        // "What's new": sample release notes shipped in assets/ (a real app uses WhatsNewSource.Play(packageName)
        // or a JSON file on its server), plus a public GitHub repo's commits to show that source
        val whatsNew = WhatsNew(this, WhatsNewSource.Bundled(asset("release-notes.json")))
        val whatsNewGitHub = WhatsNew(this, WhatsNewSource.GitHubCommits("mikepenz", "AboutLibraries"))

        setContent {
            SampleTheme {
                var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
                BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }
                val scope = rememberCoroutineScope()

                // Dialogs take turns: the update flow first, then the featured app
                var updatesSettled by remember { mutableStateOf(false) }
                UpdateFlow(updates) { updatesSettled = true }
                if (updatesSettled) FeaturedPromoDialog(promos)
                // Shows once after a versionCode change (never on the first run); nothing otherwise
                WhatsNewDialog(whatsNew)
                // Our own "Enjoying the app?" prompt; shows itself after reviewAsk.requestIfDue()
                ReviewPromptDialog(reviewAsk)

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(if (tab == Tab.HOME) "Sample App" else tab.label) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        )
                    },
                    bottomBar = {
                        Column {
                            UpdateReadyBar(updates)
                            NavigationBar {
                                Tab.entries.forEach { t ->
                                    NavigationBarItem(
                                        selected = tab == t,
                                        onClick = { tab = t },
                                        icon = { Icon(if (tab == t) t.selectedIcon else t.icon, contentDescription = null) },
                                        label = { Text(t.label) },
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            padding = padding,
                            promos = promos,
                            onCheckUpdate = { updates.play.checkNow { toast("Play update: $it") } },
                            onReview = {
                                reviewAsk.recordEvent()
                                reviewAsk.requestIfDue()
                            },
                            // Forced for the demo: this version's notes (after a real update, pending() picks them)
                            onWhatsNewDialog = { scope.launch { whatsNew.showDialog(this@MainActivity, whatsNew.notes().take(1)) } },
                            onMoreAppsView = { promos.openList(this@MainActivity) },
                            onWhatsNewView = { whatsNew.openList(this@MainActivity) },
                            onLicencesView = { OpenSource.open(this@MainActivity) },
                        )
                        Tab.APPS -> MoreAppsList(promos, Modifier.padding(padding))
                        Tab.NEWS -> WhatsNewScreen(padding, whatsNew, whatsNewGitHub)
                        Tab.LICENCES -> OpenSourceScreen(Modifier.padding(padding))
                    }
                }
            }
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun asset(name: String): String = assets.open(name).bufferedReader().use { it.readText() }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    promos: AppPromos,
    onCheckUpdate: () -> Unit,
    onReview: () -> Unit,
    onWhatsNewDialog: () -> Unit,
    onMoreAppsView: () -> Unit,
    onWhatsNewView: () -> Unit,
    onLicencesView: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The own-apps row: from the JSON feed, minus whatever this phone already has
        item { MoreAppsRow(promos, Modifier.fillMaxWidth()) }
        item {
            Section("Updates and reviews") {
                ActionRow(Icons.Outlined.SystemUpdate, "Check Play for updates", "Play's in-app update; your JSON file when Play can't answer", onCheckUpdate)
                HorizontalDivider()
                ActionRow(Icons.Outlined.StarRate, "Rate this app", "Our prompt first, then Play's sheet or the Play listing", onReview)
            }
        }
        item {
            Section("What's new") {
                ActionRow(Icons.Outlined.NewReleases, "This version's notes", "The after-update dialog, forced for the demo", onWhatsNewDialog)
            }
        }
        item {
            Section("The same screens for View apps") {
                ActionRow(Icons.Outlined.Apps, "More apps", "A plain-View list in the app's colours", onMoreAppsView)
                HorizontalDivider()
                ActionRow(Icons.AutoMirrored.Outlined.ViewList, "What's new", "The plain-View list", onWhatsNewView)
                HorizontalDivider()
                ActionRow(Icons.Outlined.Description, "Open source licences", "The plain-View list", onLicencesView)
            }
        }
    }
}

/** A titled group of rows on one tonal card, the way settings screens group their items. */
@Composable
private fun Section(title: String, rows: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            rows()
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** The What's new tab: this app's own notes, or a GitHub repo's commits to show that source. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsNewScreen(padding: PaddingValues, own: WhatsNew, gitHub: WhatsNew) {
    var source by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.padding(padding)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("This app", "GitHub commits").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = source == i,
                    onClick = { source = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                ) { Text(label) }
            }
        }
        WhatsNewList(if (source == 0) own else gitHub, Modifier.fillMaxSize())
    }
}
