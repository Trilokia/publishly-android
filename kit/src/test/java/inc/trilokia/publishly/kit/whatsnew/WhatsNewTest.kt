package inc.trilokia.publishly.kit.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewTest {
    private val versioned = listOf(
        ReleaseNote("1.3", 13, notes = "three"),
        ReleaseNote("1.2", 12, notes = "two"),
        ReleaseNote("1.1", 11, notes = "one"),
    )
    private val play = listOf(ReleaseNote(null, notes = "latest from Play"))

    @Test
    fun `with version codes, every version the user skipped is pending`() {
        assertEquals(listOf("1.3", "1.2"), WhatsNew.pick(versioned, lastSeenCode = 11, currentCode = 13).map { it.version })
        assertEquals(listOf("1.3"), WhatsNew.pick(versioned, lastSeenCode = 12, currentCode = 13).map { it.version })
        // The file hasn't caught up with this build yet: say nothing rather than repeat old news
        assertEquals(emptyList<ReleaseNote>(), WhatsNew.pick(versioned, lastSeenCode = 13, currentCode = 14))
    }

    @Test
    fun `without version codes, the newest entry is pending`() {
        assertEquals(play, WhatsNew.pick(play, lastSeenCode = 11, currentCode = 13))
    }

    @Test
    fun `nothing is pending when the app didn't change`() {
        assertEquals(emptyList<ReleaseNote>(), WhatsNew.pick(versioned, lastSeenCode = 13, currentCode = 13))
        assertEquals(emptyList<ReleaseNote>(), WhatsNew.pick(play, lastSeenCode = 13, currentCode = 12))
        assertEquals(emptyList<ReleaseNote>(), WhatsNew.pick(emptyList(), lastSeenCode = 1, currentCode = 2))
    }

    @Test
    fun `dialog text`() {
        assertEquals(
            "1.3\nthree\n\n1.2\ntwo",
            WhatsNew.text(versioned.take(2)),
        )
        assertEquals("latest from Play", WhatsNew.text(play))
    }
}
