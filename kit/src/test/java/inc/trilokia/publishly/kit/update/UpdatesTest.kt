package inc.trilokia.publishly.kit.update

import inc.trilokia.publishly.kit.update.PlayUpdates.Result
import inc.trilokia.publishly.kit.update.Updates.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatesTest {
    @Test
    fun `not from Play goes straight to the JSON file`() {
        assertEquals(Route.CHECK_JSON, Updates.route(installedFromPlay = false, playResult = null))
    }

    @Test
    fun `Play handles its own update when it can`() {
        assertEquals(Route.PLAY_DOWNLOADING, Updates.route(true, Result.FLOW_STARTED))
        assertEquals(Route.PLAY_DOWNLOADED, Updates.route(true, Result.DOWNLOADED))
    }

    @Test
    fun `up to date on Play means no JSON dialog, even if the file says newer`() {
        // A staged rollout may not have reached this phone; the JSON dialog would be a dead end
        assertEquals(Route.UP_TO_DATE, Updates.route(true, Result.UP_TO_DATE))
    }

    @Test
    fun `Play failing falls back to the JSON file`() {
        assertEquals(Route.CHECK_JSON, Updates.route(true, Result.FAILED))
        assertEquals(Route.CHECK_JSON, Updates.route(true, null))
    }
}
