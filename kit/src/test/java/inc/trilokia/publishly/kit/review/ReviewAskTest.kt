package inc.trilokia.publishly.kit.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReviewAskTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = 1_800_000_000_000L
    private val installedLongAgo = now - 30 * day

    private fun due(events: Int = 3, asks: Int = 0, askedAt: Long = 0, firstSeenAt: Long = installedLongAgo, at: Long = now) =
        ReviewAsk.isDue(
            events = events, asks = asks, askedAt = askedAt, firstSeenAt = firstSeenAt, now = at,
            minEvents = 3, minAgeMs = 2 * day, restMs = 60 * day, maxAsks = 4,
        )

    @Test
    fun `needs the minimum number of good moments`() {
        assertFalse(due(events = 1))
        assertFalse(due(events = 2))
        assertTrue(due(events = 3))
        assertTrue(due(events = 30))
    }

    @Test
    fun `waits two days after install, like the PGT apps`() {
        assertFalse(due(firstSeenAt = now))
        assertFalse(due(firstSeenAt = now - 2 * day + 1))
        assertTrue(due(firstSeenAt = now - 2 * day))
    }

    @Test
    fun `rests between asks`() {
        assertFalse(due(asks = 1, askedAt = now - 60 * day + 1))
        assertTrue(due(asks = 1, askedAt = now - 60 * day))
    }

    @Test
    fun `gives up after the last allowed ask`() {
        assertTrue(due(events = 100, asks = 3, askedAt = now - 365 * day))
        assertFalse(due(events = 100, asks = 4, askedAt = now - 365 * day))
    }
}
