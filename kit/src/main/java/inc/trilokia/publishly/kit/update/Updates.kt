package inc.trilokia.publishly.kit.update

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * The whole update flow in one call, Play first and your own JSON file as the fallback:
 *
 * ```
 * app start ─► installed from Play? ─ no ─────────────────────────┐
 *                    │ yes                                         ▼
 *              ask Play Store ─ newer ─► downloads in the background ─► "Restart" bar
 *                    │ up to date ─► nothing to show                │
 *                    │ failed (no Play, no network, Play error) ────┤
 *                                                                   ▼
 *                                                        fetch update.json ─ newer ─► update dialog ─► opens url
 *                                                                   │ same version ─► nothing
 *                                                                   │ can't fetch ─► yesterday's answer, if newer
 * ```
 *
 * Why this order: Play's flow is the smooth one (background download, no leaving the app), so
 * it goes first wherever it can work. It can't work for side-loads, other stores, debug builds
 * and phones without Play, and it fails whenever Play itself has a bad day; all of those land
 * on the JSON file, which only needs a web server. When Play answers "up to date" the JSON file
 * is *not* consulted: a staged rollout may not have reached this user yet, and a dialog that
 * sends them to a Play page still showing the old version is a dead end.
 *
 * Build it in `onCreate` (it holds a [PlayUpdates]). [jsonUrl] may be null for an app that is
 * only ever on Play. [check] is safe to call on every start: Play's own sheet rations itself,
 * and the JSON dialog is yours to show.
 */
class Updates(activity: ComponentActivity, jsonUrl: String?) {
    /** What [check] found. Only [JsonUpdate] needs the app to show something. */
    sealed class Outcome {
        /** Play is downloading the new version; the Restart bar appears when it lands. */
        object PlayDownloading : Outcome()
        /** Play already has the new version downloaded: show the Restart bar now. */
        object PlayDownloaded : Outcome()
        /** Play says this is the latest version. */
        object UpToDate : Outcome()
        /** Your JSON file knows a newer version: show the update dialog. */
        data class JsonUpdate(val info: UpdateInfo) : Outcome()
        /** Neither source has anything (or could answer). */
        object Nothing : Outcome()
    }

    /** The route the flow takes after asking (or not asking) Play. */
    enum class Route { PLAY_DOWNLOADING, PLAY_DOWNLOADED, UP_TO_DATE, CHECK_JSON }

    private val app: Context = activity.applicationContext
    val play = PlayUpdates(activity)
    val json: JsonUpdates? = jsonUrl?.let { JsonUpdates(activity, it) }

    /** Play's download has landed; offer "Restart" and call [complete]. Same as `play.downloaded`. */
    val downloaded: StateFlow<Boolean> get() = play.downloaded

    /** Runs the flow above once. Never throws; a dead network just means [Outcome.Nothing]. */
    suspend fun check(): Outcome {
        val fromPlay = installedFromPlay(app)
        val playResult = if (fromPlay) play.check() else null
        return when (route(fromPlay, playResult)) {
            Route.PLAY_DOWNLOADING -> Outcome.PlayDownloading
            Route.PLAY_DOWNLOADED -> Outcome.PlayDownloaded
            Route.UP_TO_DATE -> Outcome.UpToDate
            Route.CHECK_JSON -> json?.check()?.let { Outcome.JsonUpdate(it) } ?: Outcome.Nothing
        }
    }

    /** The plain-dialog version of the JSON update prompt, for View-based apps. */
    fun showDialog(activity: Activity, info: UpdateInfo, cancelable: Boolean = true) {
        (json ?: JsonUpdates(activity, info.url)).showDialog(activity, info, cancelable)
    }

    /** "Restart": installs Play's downloaded update, which closes and reopens the app. */
    fun complete() = play.complete()

    companion object {
        /** The decision on its own, so it can be tested. [playResult] is null when Play wasn't asked. */
        fun route(installedFromPlay: Boolean, playResult: PlayUpdates.Result?): Route = when {
            !installedFromPlay -> Route.CHECK_JSON
            playResult == PlayUpdates.Result.FLOW_STARTED -> Route.PLAY_DOWNLOADING
            playResult == PlayUpdates.Result.DOWNLOADED -> Route.PLAY_DOWNLOADED
            playResult == PlayUpdates.Result.UP_TO_DATE -> Route.UP_TO_DATE
            else -> Route.CHECK_JSON // FAILED, or Play never answered
        }

        /** True when the Play Store installed this app (so Play's in-app update can work at all). */
        fun installedFromPlay(context: Context): Boolean {
            val pm = context.packageManager
            val installer = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) pm.getInstallSourceInfo(context.packageName).installingPackageName
                else @Suppress("DEPRECATION") pm.getInstallerPackageName(context.packageName)
            }.getOrNull()
            return installer == "com.android.vending"
        }
    }
}
