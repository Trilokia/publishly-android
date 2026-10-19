package inc.trilokia.publishly.kit.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsonUpdatesTest {
    // The real file PGT Free reads, as served by trilokiainc.com on 19 Oct 2026
    private val pgtFree = """
        {
          "latestVersion": "0.21.7",
          "latestVersionCode": "193",
          "url": "https://play.google.com/store/apps/details?id=inc.trilokia.pubgfxtool.free",
          "releaseNotes": [
              "This update includes: ",
            "- New Feature: Added support for Flash Variant,",
            "- Enhanced Support: Fully compatible with the latest 4.6.x version",
             "Know more on Google Play Store"
          ]
        }
    """.trimIndent()

    @Test
    fun `parses the PGT Free file`() {
        val info = JsonUpdates.parse(pgtFree)
        assertEquals("0.21.7", info.versionName)
        assertEquals(193, info.versionCode)
        assertEquals("https://play.google.com/store/apps/details?id=inc.trilokia.pubgfxtool.free", info.url)
        assertEquals(
            "This update includes:\n" +
                "- New Feature: Added support for Flash Variant,\n" +
                "- Enhanced Support: Fully compatible with the latest 4.6.x version\n" +
                "Know more on Google Play Store",
            info.releaseNotes,
        )
    }

    @Test
    fun `accepts a numeric version code and plain-string notes`() {
        val info = JsonUpdates.parse("""{"latestVersion":"1.2","latestVersionCode":5,"url":"https://x","releaseNotes":"Bug fixes"}""")
        assertEquals(5, info.versionCode)
        assertEquals("Bug fixes", info.releaseNotes)
    }

    @Test
    fun `notes are optional`() {
        assertEquals("", JsonUpdates.parse("""{"latestVersion":"1.2","latestVersionCode":"5","url":"https://x"}""").releaseNotes)
    }

    @Test
    fun `a broken file throws instead of reporting a bogus update`() {
        assertThrows(Exception::class.java) { JsonUpdates.parse("""{"latestVersion":"1.2"}""") }
        assertThrows(Exception::class.java) { JsonUpdates.parse("<html>502 Bad Gateway</html>") }
    }
}
