package inc.trilokia.publishly.kit

import inc.trilokia.publishly.kit.promo.AppPromos
import inc.trilokia.publishly.kit.update.JsonUpdates
import inc.trilokia.publishly.kit.whatsnew.WhatsNew
import inc.trilokia.publishly.kit.whatsnew.WhatsNewSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class KitNetworkTest {
    // Nothing listens on this port: a fetch that is attempted fails at once, one that isn't answers null
    private val closedPort = "http://127.0.0.1:1/"

    @Test
    fun `without INTERNET no fetch is attempted`() = runBlocking {
        var attempts = 0
        val answer = KitNetwork.fetchIfPermitted(permitted = false) { attempts++; "fetched" }
        assertNull(answer)
        assertEquals(0, attempts)
    }

    @Test
    fun `with INTERNET the fetch runs and its answer comes back`() = runBlocking {
        assertEquals("fetched", KitNetwork.fetchIfPermitted(permitted = true) { "fetched" })
    }

    @Test
    fun `the real fetches are skipped without INTERNET, and run with it`() {
        // Skipped: null, no exception, although the address can't be reached
        runBlocking {
            assertNull(KitNetwork.fetchIfPermitted(false) { JsonUpdates.fetch(closedPort + "update.json") })
            assertNull(KitNetwork.fetchIfPermitted(false) { AppPromos.fetch(closedPort + "apps.json") })
            assertNull(KitNetwork.fetchIfPermitted(false) { WhatsNew.fetch(WhatsNewSource.Json(closedPort + "release-notes.json")) })
        }
        // Attempted: the fetch itself throws, which the callers catch and log
        assertThrows(IOException::class.java) { runBlocking { KitNetwork.fetchIfPermitted(true) { JsonUpdates.fetch(closedPort + "update.json") } } }
        assertThrows(IOException::class.java) { runBlocking { KitNetwork.fetchIfPermitted(true) { AppPromos.fetch(closedPort + "apps.json") } } }
        assertThrows(IOException::class.java) { runBlocking { KitNetwork.fetchIfPermitted(true) { WhatsNew.fetch(WhatsNewSource.Json(closedPort + "release-notes.json")) } } }
    }

    @Test
    fun `bundled release notes need no network, so they are read either way`() = runBlocking {
        val bundled = WhatsNewSource.Bundled("""{"releases":[{"version":"1.1","notes":"Shipped in the app"}]}""")
        // WhatsNew treats a bundled source as always reachable: its "fetch" is the text it carries
        assertEquals(bundled.json, WhatsNew.fetch(bundled))
        assertEquals("Shipped in the app", bundled.parse(WhatsNew.fetch(bundled)).single().notes)
    }
}
