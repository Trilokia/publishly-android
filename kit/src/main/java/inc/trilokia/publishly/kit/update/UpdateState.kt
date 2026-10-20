package inc.trilokia.publishly.kit.update

/**
 * Where an update stands, as one value: for a "Check for updates" row, a progress bar, a line in
 * Settings. [PlayUpdates.state] holds Play's side; [Updates.state] holds the whole flow, Play
 * first and your JSON file when Play can't answer, so the app watches one thing:
 *
 * ```
 * Idle ─► Checking ─┬─► UpToDate
 *                   ├─► Downloading(bytes, total) ─► Downloaded ─► Restart: the new version
 *                   ├─► ServerUpdate(info)   your JSON file knows a newer version
 *                   └─► Failed(reason)       nobody could answer
 * ```
 */
sealed class UpdateState {
    /** Nothing asked yet, or the user turned down Play's update screen. */
    data object Idle : UpdateState()

    /** Asking Play or your JSON file. Also while Play's own update screen waits for the user. */
    data object Checking : UpdateState()

    /** This is the newest version, as far as the sources know. */
    data object UpToDate : UpdateState()

    /** Play is downloading the new version. [total] is 0 until Play knows the size. */
    data class Downloading(val bytes: Long, val total: Long) : UpdateState() {
        /** How far along, 0 to 1; null while the size isn't known (show an endless bar). */
        val fraction: Float? get() = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else null
    }

    /** Play has the new version downloaded: offer "Restart" ([PlayUpdates.complete]). */
    data object Downloaded : UpdateState()

    /** Your JSON file names a newer version: show the update prompt for [info]. */
    data class ServerUpdate(val info: UpdateInfo) : UpdateState()

    /** No source could answer. [reason] is for logs and bug reports, in English, not for users. */
    data class Failed(val reason: String) : UpdateState()
}
