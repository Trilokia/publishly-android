package inc.trilokia.publishly.kit.update

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * only ever on Play; [bundledJson] is a copy of the update file shipped in `assets/`, used until
 * the server has answered once (see [JsonUpdates]). [check] is safe to call on every start:
 * Play's own sheet rations itself, and the JSON dialog is yours to show. [checkNow] is the
 * "Check for updates" row; [state] follows both as one [UpdateState], whichever source answers.
 */
class Updates(activity: ComponentActivity, jsonUrl: String?, bundledJson: String? = null) {
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
    val json: JsonUpdates? = if (jsonUrl != null || bundledJson != null) JsonUpdates(activity, jsonUrl, bundledJson) else null

    /** Play's download has landed; offer "Restart" and call [complete]. Same as `play.downloaded`. */
    val downloaded: StateFlow<Boolean> get() = play.downloaded

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)

    /**
     * The whole flow as one value: [UpdateState.Checking] while either source is asked, then
     * Play's answer ([UpdateState.UpToDate], [UpdateState.Downloading] with its progress,
     * [UpdateState.Downloaded]) or, when Play can't answer, the JSON file's
     * ([UpdateState.ServerUpdate], [UpdateState.UpToDate]); [UpdateState.Failed] when neither could.
     */
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    // While a check runs, it decides the state itself: a Play failure is only a step on the way to the JSON file
    @Volatile
    private var checking = false

    init {
        // Play's answers that come later (the download's progress, its consent sheet closing) carry on here
        play.onState = { s -> if (!checking || s is UpdateState.Downloading || s is UpdateState.Downloaded) _state.value = s }
    }

    /** Runs the flow above once. Never throws; a dead network just means [Outcome.Nothing]. */
    suspend fun check(): Outcome = checking {
        val fromPlay = installedFromPlay(app)
        val playResult = if (fromPlay) play.check() else null
        when (route(fromPlay, playResult)) {
            Route.PLAY_DOWNLOADING -> play.state.value to Outcome.PlayDownloading
            Route.PLAY_DOWNLOADED -> UpdateState.Downloaded to Outcome.PlayDownloaded
            Route.UP_TO_DATE -> UpdateState.UpToDate to Outcome.UpToDate
            Route.CHECK_JSON -> {
                val s = jsonState(fromPlay)
                s to ((s as? UpdateState.ServerUpdate)?.let { Outcome.JsonUpdate(it.info) } ?: Outcome.Nothing)
            }
        }
    }

    /**
     * The "Check for updates" row: Play's full-screen flow when Play has a newer version
     * ([PlayUpdates.checkNow], which waits for the user to close it), your JSON file when Play
     * can't answer. Returns the state it ends in, also in [state]: show [UpdateState.ServerUpdate]'s
     * prompt, say "up to date" for [UpdateState.UpToDate]. Never throws.
     */
    suspend fun checkNow(): UpdateState = checking {
        val fromPlay = installedFromPlay(app)
        val playState = if (fromPlay) play.checkNow() else null
        // Same rule as the quiet check: only a Play that can't answer sends us to the JSON file
        val s = if (playState == null || playState is UpdateState.Failed) jsonState(fromPlay) else playState
        s to s
    }

    private suspend fun <T> checking(run: suspend () -> Pair<UpdateState, T>): T {
        checking = true
        _state.value = UpdateState.Checking
        try {
            val (state, result) = run()
            _state.value = state
            return result
        } catch (e: CancellationException) {
            if (_state.value == UpdateState.Checking) _state.value = UpdateState.Idle
            throw e
        } finally {
            checking = false
        }
    }

    private suspend fun jsonState(fromPlay: Boolean): UpdateState =
        json?.checkState()
            ?: (play.state.value as? UpdateState.Failed).takeIf { fromPlay }
            ?: UpdateState.Failed("Not installed from Play, and no update file to ask")

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
