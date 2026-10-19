package inc.trilokia.publishly.kit.update

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
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
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

    // Play's consent sheet reports through this; the app doesn't care how it closed: a refused
    // download just leaves things as they were, and the next app start may ask again
    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    private val _downloaded = MutableStateFlow(false)

    /** A new version is downloaded and waiting; offer "Restart" and call [complete]. */
    val downloaded: StateFlow<Boolean> = _downloaded.asStateFlow()

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _downloaded.value = true
    }

    init {
        manager.registerListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                manager.unregisterListener(listener)
            }
        })
    }

    /**
     * On app start (and in `onResume`, if you like): begin a background download when Play has a
     * newer version, and pick up an interrupted full-screen update. Says nothing.
     */
    fun checkQuietly() {
        manager.appUpdateInfo.addOnSuccessListener { info -> quiet(info) }
    }

    /**
     * [checkQuietly] as a suspend call that says what happened, so a caller can fall back to
     * something else on [Result.FAILED] (see [Updates]). Never throws.
     */
    suspend fun check(): Result = suspendCancellableCoroutine { cont ->
        manager.appUpdateInfo
            .addOnSuccessListener { info -> if (cont.isActive) cont.resume(quiet(info)) }
            .addOnFailureListener { if (cont.isActive) cont.resume(Result.FAILED) }
    }

    private fun quiet(info: AppUpdateInfo): Result = when {
        // Downloaded on an earlier run and still waiting (the listener never saw it)
        info.installStatus() == InstallStatus.DOWNLOADED -> {
            _downloaded.value = true
            Result.DOWNLOADED
        }
        // An immediate update the user left half-way: Play wants it finished
        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
            if (start(info, AppUpdateType.IMMEDIATE)) Result.FLOW_STARTED else Result.FAILED
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
            if (start(info, AppUpdateType.FLEXIBLE)) Result.FLOW_STARTED else Result.FAILED
        else -> Result.UP_TO_DATE
    }

    /** The Settings row. [result] gets one answer, on the main thread, for a snackbar or toast. */
    fun checkNow(result: (Result) -> Unit) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        _downloaded.value = true
                        result(Result.DOWNLOADED)
                    }
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // The user asked, so the full-screen flow; flexible where a dev-triggered
                        // or size-limited update allows only that
                        val type = if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                        result(if (start(info, type)) Result.FLOW_STARTED else Result.FAILED)
                    }
                    else -> result(Result.UP_TO_DATE)
                }
            }
            .addOnFailureListener { result(Result.FAILED) }
    }

    private fun start(info: AppUpdateInfo, type: Int): Boolean = runCatching {
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
    }.isSuccess

    /** "Restart": installs the downloaded update, which closes and reopens the app. */
    fun complete() {
        manager.completeUpdate()
    }
}
