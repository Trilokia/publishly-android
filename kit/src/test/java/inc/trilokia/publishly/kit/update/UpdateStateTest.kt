package inc.trilokia.publishly.kit.update

import android.app.Activity
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import inc.trilokia.publishly.kit.update.UpdateState.Downloaded
import inc.trilokia.publishly.kit.update.UpdateState.Downloading
import inc.trilokia.publishly.kit.update.UpdateState.Failed
import inc.trilokia.publishly.kit.update.UpdateState.Idle
import inc.trilokia.publishly.kit.update.UpdateState.ServerUpdate
import inc.trilokia.publishly.kit.update.UpdateState.UpToDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateStateTest {
    // The real file PGT Free reads, as served by trilokiainc.com on 19 Oct 2026
    private val pgtFree = JsonUpdates.parse(javaClass.getResource("/update/pgt_free.json")!!.readText())

    @Test
    fun `Play's download shows its progress`() {
        // A 18.4 MB update, a quarter of the way
        val s = PlayUpdates.stateFor(InstallStatus.DOWNLOADING, bytes = 4_600_000, total = 18_400_000)
        assertEquals(Downloading(4_600_000, 18_400_000), s)
        assertEquals(0.25f, (s as Downloading).fraction!!, 0.0001f)
    }

    @Test
    fun `a download that hasn't started, or whose size isn't known, has no fraction`() {
        assertEquals(Downloading(0, 0), PlayUpdates.stateFor(InstallStatus.PENDING, bytes = 0, total = 0))
        assertNull(Downloading(1_000, 0).fraction)
        assertEquals(1f, Downloading(20, 10).fraction!!, 0f)
    }

    @Test
    fun `every install status Play reports has its state`() {
        assertEquals(Downloaded, PlayUpdates.stateFor(InstallStatus.DOWNLOADED, 18_400_000, 18_400_000))
        assertEquals(UpToDate, PlayUpdates.stateFor(InstallStatus.INSTALLED, 0, 0))
        assertEquals(Idle, PlayUpdates.stateFor(InstallStatus.CANCELED, 0, 0))
        val failed = PlayUpdates.stateFor(InstallStatus.FAILED, 0, 0, errorCode = InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED)
        assertTrue(failed is Failed && failed.reason.contains("-6"))
    }

    @Test
    fun `unknown and installing change nothing`() {
        // Installing: the app is about to restart, the Restart bar stays as it was
        assertNull(PlayUpdates.stateFor(InstallStatus.INSTALLING, 0, 0))
        assertNull(PlayUpdates.stateFor(InstallStatus.UNKNOWN, 0, 0))
    }

    @Test
    fun `closing Play's update screen`() {
        assertEquals(Downloading(0, 0), PlayUpdates.stateAfterFlow(Activity.RESULT_OK, UpdateState.Checking))
        assertEquals(Idle, PlayUpdates.stateAfterFlow(Activity.RESULT_CANCELED, UpdateState.Checking))
        assertTrue(PlayUpdates.stateAfterFlow(ActivityResult.RESULT_IN_APP_UPDATE_FAILED, UpdateState.Checking) is Failed)
    }

    @Test
    fun `the download listener's state wins over the screen closing after it`() {
        val progress = Downloading(1_200_000, 18_400_000)
        assertEquals(progress, PlayUpdates.stateAfterFlow(Activity.RESULT_OK, progress))
        assertEquals(Downloaded, PlayUpdates.stateAfterFlow(Activity.RESULT_CANCELED, Downloaded))
    }

    @Test
    fun `the JSON file's answer`() {
        // PGT Free 0.21.6 (192) is offered 0.21.7 (193); 193 itself is up to date
        assertEquals(ServerUpdate(pgtFree), JsonUpdates.stateFor(pgtFree, currentCode = 192))
        assertEquals(UpToDate, JsonUpdates.stateFor(pgtFree, currentCode = 193))
        assertEquals(UpToDate, JsonUpdates.stateFor(pgtFree, currentCode = 200))
    }

    @Test
    fun `nothing to read is a failure, with the reason when there is one`() {
        assertEquals(Failed("java.net.UnknownHostException: www.trilokiainc.com"), JsonUpdates.stateFor(null, 192, "java.net.UnknownHostException: www.trilokiainc.com"))
        assertTrue(JsonUpdates.stateFor(null, 192) is Failed)
    }
}
