package inc.trilokia.publishly.kit.oss

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LicenceText.reflow] on the licence texts of a real build: the `aboutlibraries.json` the
 * AboutLibraries plugin wrote for Gamers GLTool Pro (Apache-2.0, MIT, GPL-2.0 with the Classpath
 * Exception, LGPL-3.0, OFL-1.1, Bouncy Castle).
 */
class LicenceTextTest {
    private val licences: Map<String, String> = run {
        val json = JSONObject(javaClass.getResource("/aboutlibraries_gltool.json")!!.readText()).getJSONObject("licenses")
        json.keys().asSequence().associateWith { json.getJSONObject(it).optString("content") }.filterValues { it.isNotBlank() }
    }

    private fun words(text: String) = text.trim().split(Regex("\\s+"))

    @Test
    fun theFixtureHasTheLongTexts() {
        assertTrue(licences.keys.containsAll(listOf("Apache-2.0", "MIT", "glt-lgpl-3.0", "glt-gpl-2.0-classpath-exception")))
        assertTrue(licences.getValue("glt-lgpl-3.0").length > 40_000)
    }

    @Test
    fun everyWordStaysInPlace() {
        for ((id, text) in licences) {
            assertEquals(id, words(text), words(LicenceText.reflow(text)))
        }
    }

    @Test
    fun aGnuHeaderKeepsItsTwoTitleLines() {
        val lgpl = LicenceText.reflow(licences.getValue("glt-lgpl-3.0"))
        assertTrue(lgpl, lgpl.startsWith("GNU LESSER GENERAL PUBLIC LICENSE\nVersion 3, 29 June 2007\n\n"))
        val gpl = LicenceText.reflow(licences.getValue("glt-gpl-2.0-classpath-exception"))
        assertTrue(gpl, gpl.startsWith("The GNU General Public License (GPL)\n\nVersion 2, June 1991\n\n"))
    }

    @Test
    fun aProseParagraphBecomesOneLine() {
        val lines = LicenceText.reflow(licences.getValue("glt-lgpl-3.0")).lines()
        assertTrue(
            "This version of the GNU Lesser General Public License incorporates the terms and conditions of version 3 " +
                "of the GNU General Public License, supplemented by the additional permissions listed below." in lines,
        )
        val gpl = LicenceText.reflow(licences.getValue("glt-gpl-2.0-classpath-exception")).lines()
        assertTrue(gpl.any { it.startsWith("The licenses for most software are designed to take away your freedom to share and change it. By contrast,") })
    }

    @Test
    fun shortLinesStayAsTheyAre() {
        val apache = LicenceText.reflow(licences.getValue("Apache-2.0"))
        assertTrue(apache, apache.startsWith("Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\n"))
    }

    @Test
    fun linesAreTrimmedAndBlocksOneBlankLineApart() {
        for ((id, text) in licences) {
            val reflowed = LicenceText.reflow(text)
            assertTrue(id, reflowed.lines().all { it == it.trim() })
            assertTrue(id, "\n\n\n" !in reflowed)
        }
    }

    @Test
    fun hardWrappedTextsGetFewerLines() {
        for (id in listOf("glt-lgpl-3.0", "glt-gpl-2.0-classpath-exception")) {
            val text = licences.getValue(id)
            assertTrue(id, LicenceText.reflow(text).lines().size * 2 < text.lines().size)
        }
    }

    @Test
    fun aParagraphWithAShortLastLineIsJoined() {
        val gpl = LicenceText.reflow(licences.getValue("glt-gpl-2.0-classpath-exception")).lines()
        assertTrue("The precise terms and conditions for copying, distribution and modification follow." in gpl)
    }

    @Test
    fun aHeadingAboveItsParagraphKeepsItsLine() {
        val ofl = LicenceText.reflow(licences.getValue("glt-ofl-1.1")).lines()
        val at = ofl.indexOf("PREAMBLE")
        assertTrue(at >= 0)
        assertTrue(ofl[at + 1], ofl[at + 1].startsWith("The goals of the Open Font License (OFL) are to stimulate worldwide development"))
    }

    @Test
    fun alreadyUnwrappedTextIsUnchanged() {
        val mit = licences.getValue("MIT")
        assertEquals(mit.trim(), LicenceText.reflow(mit))
    }

    @Test
    fun windowsLineEndsAndBlankEdges() {
        assertEquals("Title\n\nA line of prose that is long enough to count, wrapped here and continued on the next line.",
            LicenceText.reflow("\r\n  Title\r\n\r\n\r\nA line of prose that is long enough to count, wrapped\r\nhere and continued on the next line.\r\n  \r\n"))
        assertEquals("", LicenceText.reflow(""))
        // Two short lines are a list, not prose
        assertEquals("Line one\nLine two", LicenceText.reflow("Line one\nLine two"))
    }
}
