package inc.trilokia.publishly.kit.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewSourceTest {
    private fun fixture(name: String): String =
        javaClass.getResource("/whatsnew/$name")!!.readText()

    // PGT Free's Play listing (the part of the page around the description boxes), fetched 19 Oct 2026

    @Test
    fun `reads the What's new box of a Play listing`() {
        val notes = WhatsNewSource.parsePlayListing(fixture("play_en.html"))
        assertEquals(1, notes.size)
        val n = notes[0]
        assertNull(n.version)
        assertEquals(
            "📌 Major Update 📌\n" +
                "• New Feature: Added support for Flash variant.\n" +
                "• Enhanced Support: Fully compatible with latest 4.6.x version\n" +
                "• Crash & Bug Fixes: Experience faster & optimized app stability\n" +
                "• Compatibility: Supports Android 7 to 17\n" +
                "\n" +
                "⭐ Please leave a review, it helps a lot!",
            n.notes,
        )
    }

    @Test
    fun `the listing's language doesn't matter`() {
        // Same app, hl=hi: the headings are in Hindi, the box is found all the same
        assertEquals(
            WhatsNewSource.parsePlayListing(fixture("play_en.html")),
            WhatsNewSource.parsePlayListing(fixture("play_hi.html")),
        )
    }

    @Test
    fun `a listing without What's new text gives nothing, a non-listing page throws`() {
        val page = fixture("play_en.html")
        val withoutBox = page.replace(Regex("<div itemprop=\"description\">.*?</div>", RegexOption.DOT_MATCHES_ALL), "")
        assertTrue(withoutBox.contains("data-g-id=\"description\""))
        assertEquals(emptyList<ReleaseNote>(), WhatsNewSource.parsePlayListing(withoutBox))
        assertThrows(Exception::class.java) { WhatsNewSource.parsePlayListing("<html><body>Sorry, we couldn't find that page</body></html>") }
    }

    @Test
    fun `the Play source asks for the listing in the given language with a browser user agent`() {
        val src = WhatsNewSource.Play("inc.trilokia.pubgfxtool.free", language = "hi")
        assertEquals("https://play.google.com/store/apps/details?id=inc.trilokia.pubgfxtool.free&hl=hi", src.url)
        assertTrue(src.headers.getValue("User-Agent").startsWith("Mozilla/5.0"))
    }

    // Your own JSON file

    @Test
    fun `reads the releases file`() {
        val notes = WhatsNewSource.parseJson(
            """
            {"releases":[
              {"version":"0.21.7","versionCode":193,"date":"2026-10-08","notes":["- Flash variant","- Bug fixes"],"url":"https://x"},
              {"version":"0.21.6","versionCode":"192","notes":"Compatibility with the 4.5 update."},
              {"notes":"An entry with nothing but text"}
            ]}
            """,
        )
        assertEquals(3, notes.size)
        assertEquals(ReleaseNote("0.21.7", 193, "2026-10-08", "- Flash variant\n- Bug fixes", "https://x"), notes[0])
        assertEquals(192, notes[1].versionCode)
        assertNull(notes[1].date)
        assertEquals("0.21.7  ·  2026-10-08", notes[0].heading)
        assertEquals("", notes[2].heading)
    }

    @Test
    fun `reads a JsonUpdates file as one entry`() {
        // The real file PGT Free reads, as served by trilokiainc.com on 19 Oct 2026
        val notes = WhatsNewSource.parseJson(
            """
            {
              "latestVersion": "0.21.7",
              "latestVersionCode": "193",
              "url": "https://play.google.com/store/apps/details?id=inc.trilokia.pubgfxtool.free",
              "releaseNotes": ["This update includes: ", "- New Feature: Added support for Flash Variant,", "Know more on Google Play Store"]
            }
            """,
        )
        assertEquals(1, notes.size)
        assertEquals("0.21.7", notes[0].version)
        assertEquals(193, notes[0].versionCode)
        assertEquals("This update includes:\n- New Feature: Added support for Flash Variant,\nKnow more on Google Play Store", notes[0].notes)
    }

    @Test
    fun `a broken file throws`() {
        assertThrows(Exception::class.java) { WhatsNewSource.parseJson("""{"version":"1"}""") }
        assertThrows(Exception::class.java) { WhatsNewSource.parseJson("<html>502</html>") }
    }

    // GitHub: api.github.com answers for mikepenz/AboutLibraries, fetched 19 Oct 2026

    @Test
    fun `reads releases with tag, date and the body as plain text`() {
        val notes = WhatsNewSource.parseGitHubReleases(fixture("github_releases.json"), includePrereleases = false)
        assertEquals(listOf("15.3.0", "15.2.0"), notes.map { it.version })
        assertEquals("2026-10-13", notes[0].date)
        assertEquals("https://github.com/mikepenz/AboutLibraries/releases/tag/15.3.0", notes[0].url)
        assertTrue(notes[0].notes.startsWith("🚀 Features\n\n- chore(deps): update com.mikepenz:version-catalog to 0.22.0\n- PR: #1483"))
        assertFalse(notes[0].notes.lines().any { it.startsWith("#") }, "no heading marks left: " + notes[0].notes)
    }

    @Test
    fun `drafts are never shown and pre-releases only when asked`() {
        val json = """[
            {"tag_name":"2.0.0-beta1","prerelease":true,"draft":false,"body":"beta","published_at":"2026-10-01T00:00:00Z"},
            {"tag_name":"1.9.9","prerelease":false,"draft":true,"body":"draft","published_at":"2026-09-01T00:00:00Z"},
            {"tag_name":"1.9.0","prerelease":false,"draft":false,"body":"stable"}
        ]"""
        assertEquals(listOf("1.9.0"), WhatsNewSource.parseGitHubReleases(json, includePrereleases = false).map { it.version })
        assertEquals(listOf("2.0.0-beta1", "1.9.0"), WhatsNewSource.parseGitHubReleases(json, includePrereleases = true).map { it.version })
        assertNull(WhatsNewSource.parseGitHubReleases(json, true)[1].date)
    }

    @Test
    fun `reads commits as one line each, merges left out`() {
        val notes = WhatsNewSource.parseGitHubCommits(fixture("github_commits.json"), skipMerges = true)
        assertEquals(2, notes.size)
        assertEquals("f771ae4", notes[0].version)
        assertEquals("2026-10-18", notes[0].date)
        assertEquals("fix(plugin): strip .git suffix from repository url before fetching remote license", notes[0].notes)
        assertEquals("https://github.com/mikepenz/AboutLibraries/commit/f771ae47fc711c42c080538fdd0afe575f10045a", notes[0].url)
        assertEquals("chore(deps): update mikepenz/action-junit-report digest to 6ef3fdc (#1486)", notes[1].notes)

        val all = WhatsNewSource.parseGitHubCommits(fixture("github_commits.json"), skipMerges = false)
        assertEquals(3, all.size)
        assertEquals("Merge pull request #1487 from mikepenz/fix/strip-git-suffix", all[0].notes)
    }

    @Test
    fun `GitHub URLs`() {
        assertEquals("https://api.github.com/repos/a/b/releases?per_page=10", WhatsNewSource.GitHubReleases("a", "b").url)
        assertEquals("https://api.github.com/repos/a/b/commits?per_page=30", WhatsNewSource.GitHubCommits("a", "b").url)
        assertEquals("https://api.github.com/repos/a/b/commits?per_page=5&sha=dev", WhatsNewSource.GitHubCommits("a", "b", branch = "dev", count = 5).url)
    }

    private fun assertFalse(value: Boolean, message: String) = assertFalse(message, value)
}
