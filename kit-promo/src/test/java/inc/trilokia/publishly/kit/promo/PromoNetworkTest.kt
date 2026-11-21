package inc.trilokia.publishly.kit.promo

import inc.trilokia.publishly.kit.KitNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

/** The promo feed follows kit's INTERNET rule (KitNetworkTest has the rule itself and the other fetches). */
class PromoNetworkTest {
    // Nothing listens on this port: a fetch that is attempted fails at once, one that isn't answers null
    private val closedPort = "http://127.0.0.1:1/"

    @Test
    fun `the feed fetch is skipped without INTERNET, and runs with it`() {
        // Skipped: null, no exception, although the address can't be reached
        runBlocking { assertNull(KitNetwork.fetchIfPermitted(false) { AppPromos.fetch(closedPort + "apps.json") }) }
        // Attempted: the fetch itself throws, which AppPromos catches and logs
        assertThrows(IOException::class.java) { runBlocking { KitNetwork.fetchIfPermitted(true) { AppPromos.fetch(closedPort + "apps.json") } } }
    }
}
