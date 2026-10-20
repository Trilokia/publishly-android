package inc.trilokia.publishly.kit.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * In-app updates through the phone's own Play Store. Two ways in:
 *
 * - [checkQuietly], once per app start: a newer version on Play starts downloading in the
 *   background (Play's flexible flow: a small sheet asks first, the app stays usable). When the
 *   download lands, [downloaded] turns true and the app should offer "Restart" ([complete] installs).
 * - [checkNow], a "Check for updates" row in Settings: the user asked, so a waiting update gets
 *   Play's full-screen flow, and "already on the latest" is reported instead of silently swallowed.
 *
 * [state] follows all of it as one [UpdateState], download progress included.
 *
 * Build it in the activity's `onCreate` (the result launcher must be registered before the activity
 * starts). It unregisters itself when the activity is destroyed. A phone without Play (an emulator,
 * a side load) answers with a failure, which the quiet check swallows and the loud one reports.
 *
 * Only works for apps installed from Play, and only once a higher versionCode is live on a track
 * the test account is on.
 */
class PlayUpdates(activity: ComponentActivity) {
    enum class Result {
        /** Nothing newer on Play. */
        UP_TO_DATE,
        /** Play's own update screen has taken over; say nothing. */
        FLOW_STARTED,
        /** A new version is already downloaded; show the Restart offer. */
        DOWNLOADED,
        /** Play couldn't be asked (no Play, no network, not installed from Play). */
        FAILED,
    }

    private val manager = AppUpdateManagerFactory.create(activity)

    // Completes when Play's update screen closes; the suspend checkNow waits on it
    private var flowClosed: CompletableDeferred<Unit>? = null

    // Play's consent sheet (or full-screen flow) reports through this. A refused download just
    // leaves things as they were, and the next app start may ask again
    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            set(stateAfterFlow(result.resultCode, _state.value))
            flowClosed?.complete(Unit)
        }

    private val _downloaded = MutableStateFlow(false)

    /** A new version is downloaded and waiting; offer "Restart" and call [complete]. */
    val downloaded: StateFlow<Boolean> = _downloaded.asStateFlow()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)

    /**
     * Where Play's update stands: [UpdateState.Checking] while Play is asked, then
     * [UpdateState.UpToDate], [UpdateState.Downloading] with the bytes so far, [UpdateState.Downloaded],
     * or [UpdateState.Failed]. Never [UpdateState.ServerUpdate]: that one is [Updates]'.
     */
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** [Updates] listens here to carry Play's answers into its own state. */
    internal var onState: ((UpdateState) -> Unit)? = null

    private val listener = InstallStateUpdatedListener { s ->
        stateFor(s.installStatus(), s.bytesDownloaded(), s.totalBytesToDownload(), s.installErrorCode())?.let(::set)
    }

    init {
        manager.registerListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                manager.unregisterListener(listener)
            }
        })
    }

    private fun set(state: UpdateState) {
        if (state is UpdateState.Downloaded) _downloaded.value = true
        _state.value = state
        onState?.invoke(state)
    }

    // A download under way keeps showing its progress while Play is asked again
    private fun startChecking() {
        val now = _state.value
        if (now !is UpdateState.Downloading && now !is UpdateState.Downloaded) set(UpdateState.Checking)
    }

    private fun failed(e: Exception): Result {
        set(UpdateState.Failed("Play couldn't be asked: ${e.message ?: e.javaClass.simpleName}"))
        return Result.FAILED
    }

    /**
     * On app start (and in `onResume`, if you like): begin a background download when Play has a
     * newer version, and pick up an interrupted full-screen update. Says nothing.
     */
    fun checkQuietly() {
        startChecking()
        manager.appUpdateInfo
            .addOnSuccessListener { info -> quiet(info) }
            .addOnFailureListener { e -> failed(e) }
    }

    /**
     * [checkQuietly] as a suspend call that says what happened, so a caller can fall back to
     * something else on [Result.FAILED] (see [Updates]). Never throws.
     */
    suspend fun check(): Result = suspendCancellableCoroutine { cont ->
        startChecking()
        manager.appUpdateInfo
            .addOnSuccessListener { info -> if (cont.isActive) cont.resume(quiet(info)) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resume(failed(e)) }
    }

    private fun quiet(info: AppUpdateInfo): Result = when {
        // Downloaded on an earlier run and still waiting (the listener never saw it)
        info.installStatus() == InstallStatus.DOWNLOADED -> {
            set(UpdateState.Downloaded)
            Result.DOWNLOADED
        }
        // An immediate update the user left half-way: Play wants it finished
        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
            showProgress(info)
            flow(info, AppUpdateType.IMMEDIATE)
        }
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> flow(info, AppUpdateType.FLEXIBLE)
        else -> {
            set(UpdateState.UpToDate)
            Result.UP_TO_DATE
        }
    }

    /** The Settings row. [result] gets one answer, on the main thread, for a snackbar or toast. */
    fun checkNow(result: (Result) -> Unit) {
        startChecking()
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        set(UpdateState.Downloaded)
                        result(Result.DOWNLOADED)
                    }
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        showProgress(info)
                        // The user asked, so the full-screen flow; flexible where a dev-triggered
                        // or size-limited update allows only that
                        val type = if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                        result(flow(info, type))
                    }
                    else -> {
                        set(UpdateState.UpToDate)
                        result(Result.UP_TO_DATE)
                    }
                }
            }
            .addOnFailureListener { e -> result(failed(e)) }
    }

    /**
     * The Settings row as a suspend call: the same check as `checkNow { }`, and when Play's update
     * screen opens, it waits for the user to close it. Returns where things ended:
     * [UpdateState.UpToDate], [UpdateState.Downloading], [UpdateState.Downloaded],
     * [UpdateState.Idle] (the user said no) or [UpdateState.Failed]. Never throws.
     */
    suspend fun checkNow(): UpdateState {
        val result = suspendCancellableCoroutine { cont -> checkNow { if (cont.isActive) cont.resume(it) } }
        if (result == Result.FLOW_STARTED) flowClosed?.await()
        return _state.value
    }

    // A download already under way (left from an earlier start) shows its progress at once
    private fun showProgress(info: AppUpdateInfo) {
        val progress = stateFor(info.installStatus(), info.bytesDownloaded(), info.totalBytesToDownload())
        if (progress is UpdateState.Downloading) set(progress)
    }

    private fun flow(info: AppUpdateInfo, type: Int): Result {
        if (start(info, type)) return Result.FLOW_STARTED
        set(UpdateState.Failed("Play's update screen didn't open"))
        return Result.FAILED
    }

    private fun start(info: AppUpdateInfo, type: Int): Boolean {
        flowClosed = CompletableDeferred()
        return runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.isSuccess
    }

    /** "Restart": installs the downloaded update, which closes and reopens the app. */
    fun complete() {
        manager.completeUpdate()
    }

    companion object {
        /**
         * Play's install status (with its download counts and error code) as an [UpdateState]; null
         * for the statuses that change nothing: unknown, and installing (the app is about to restart).
         */
        fun stateFor(installStatus: Int, bytes: Long, total: Long, errorCode: Int = 0): UpdateState? = when (installStatus) {
            InstallStatus.PENDING -> UpdateState.Downloading(0, total.coerceAtLeast(0))
            InstallStatus.DOWNLOADING -> UpdateState.Downloading(bytes.coerceAtLeast(0), total.coerceAtLeast(0))
            InstallStatus.DOWNLOADED -> UpdateState.Downloaded
            InstallStatus.INSTALLED -> UpdateState.UpToDate
            InstallStatus.FAILED -> UpdateState.Failed("Play couldn't download the update (install error $errorCode)")
            InstallStatus.CANCELED -> UpdateState.Idle
            else -> null
        }

        /**
         * What Play's update screen closing means for the state it found. Accepted: the download
         * starts (the listener then reports its progress). Turned down: [UpdateState.Idle]. When the
         * listener got there first, its state stands.
         */
        fun stateAfterFlow(resultCode: Int, current: UpdateState): UpdateState = when {
            current !is UpdateState.Checking -> current
            resultCode == Activity.RESULT_OK -> UpdateState.Downloading(0, 0)
            resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> UpdateState.Failed("Play's update screen failed")
            else -> UpdateState.Idle
        }
    }
}
