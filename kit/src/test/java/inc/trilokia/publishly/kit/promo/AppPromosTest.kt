package inc.trilokia.publishly.kit.promo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class AppPromosTest {
    // The template feed in the repo, built from the real Play listings of Trilokia Inc. (19 Oct 2026)
    private val feed = PromoFeed.parse(File("../promo/apps.json").readText())

    @Test
    fun `reads the template feed`() {
        assertEquals(8, feed.apps.size)
        val card = feed.apps.first()
        assertEquals("inc.trilokia.cardjuggler", card.packageName)
        assertEquals("Card Juggler: Bank Card Offers", card.name)
        assertEquals("New", card.badge)
        assertEquals(100, card.priority)
        assertTrue(card.iconUrl!!.startsWith("https://play-lh.googleusercontent.com/"))
        assertEquals("inc.trilokia.cardjuggler", feed.featured!!.packageName)
        assertEquals("New from Trilokia: Card Juggler", feed.featured!!.title)
    }

    @Test
    fun `the developer name comes from code first, then the feed, and blank counts as none`() {
        assertEquals("Trilokia Inc.", feed.developer)
        assertEquals("Trilokia Inc.", AppPromos.developerOf(null, feed))
        assertEquals("Trilokia", AppPromos.developerOf("  Trilokia ", feed))
        assertEquals("Trilokia Inc.", AppPromos.developerOf("   ", feed))
        val unnamed = PromoFeed.parse("""{"developer":"  ","apps":[{"package":"a.b","name":"AB"}]}""")
        assertNull(unnamed.developer)
        assertNull(AppPromos.developerOf(null, unnamed))
        assertNull(AppPromos.developerOf(null, null))
    }

    @Test
    fun `only package and name are required`() {
        val f = PromoFeed.parse("""{"apps":[{"package":"a.b","name":"AB"}]}""")
        assertEquals(PromoApp("a.b", "AB"), f.apps.single())
        assertNull(f.featured)
        assertThrows(Exception::class.java) { PromoFeed.parse("""{"apps":[{"name":"no package"}]}""") }
    }

    @Test
    fun `leaves out this app and installed ones, best first`() {
        val installed = setOf("inc.trilokia.gfxtool.free", "inc.trilokia.addon.instant.boost")
        val picked = AppPromos.pick(feed, self = "inc.trilokia.pubgfxtool.free") { it in installed }
        assertEquals(
            listOf(
                "inc.trilokia.cardjuggler",
                "inc.trilokia.pubgfxtool",
                "inc.trilokia.gfxtool",
                "inc.trilokia.addon.graphics.manager",
                "inc.trilokia.addon.preference.manager",
            ),
            picked.map { it.packageName },
        )
    }

    @Test
    fun `the full list keeps installed apps, after the others`() {
        val installed = setOf("inc.trilokia.cardjuggler", "inc.trilokia.addon.instant.boost")
        val all = AppPromos.arrange(feed, self = "inc.trilokia.pubgfxtool.free") { it in installed }
        assertEquals(
            listOf(
                "inc.trilokia.pubgfxtool" to false,
                "inc.trilokia.gfxtool.free" to false,
                "inc.trilokia.gfxtool" to false,
                "inc.trilokia.addon.graphics.manager" to false,
                "inc.trilokia.addon.preference.manager" to false,
                "inc.trilokia.cardjuggler" to true,
                "inc.trilokia.addon.instant.boost" to true,
            ),
            all.map { it.app.packageName to it.installed },
        )
    }

    @Test
    fun `the full list sorts each group by priority, ties in feed order, and never shows this app`() {
        val f = PromoFeed.parse(
            """{"apps":[{"package":"me","name":"Me","priority":99},{"package":"a","name":"A"},{"package":"b","name":"B","priority":5},
            {"package":"c","name":"C"},{"package":"d","name":"D","priority":5}]}""",
        )
        val all = AppPromos.arrange(f, self = "me") { it == "a" || it == "d" }
        assertEquals(listOf("b", "c", "d", "a"), all.map { it.app.packageName })
        assertEquals(listOf(false, false, true, true), all.map { it.installed })
    }

    @Test
    fun `equal priorities keep the feed's order`() {
        val f = PromoFeed.parse("""{"apps":[{"package":"x","name":"X"},{"package":"y","name":"Y","priority":5},{"package":"z","name":"Z"}]}""")
        assertEquals(listOf("y", "x", "z"), AppPromos.pick(f, "me") { false }.map { it.packageName })
    }

    @Test
    fun `the featured app rests a week and stops when dismissed`() {
        val week = TimeUnit.DAYS.toMillis(7)
        val now = 1_800_000_000_000L
        assertTrue(AppPromos.featuredDue(lastShownAt = 0, now = now, restMs = week, dismissed = false))
        assertFalse(AppPromos.featuredDue(lastShownAt = now - week + 1, now = now, restMs = week, dismissed = false))
        assertTrue(AppPromos.featuredDue(lastShownAt = now - week, now = now, restMs = week, dismissed = false))
        assertFalse(AppPromos.featuredDue(lastShownAt = 0, now = now, restMs = week, dismissed = true))
    }

    @Test
    fun `the Play link carries the sending app as referrer`() {
        val app = PromoApp("inc.trilokia.cardjuggler", "Card Juggler")
        assertEquals("https://play.google.com/store/apps/details?id=inc.trilokia.cardjuggler", app.storeUrl())
        assertEquals(
            "https://play.google.com/store/apps/details?id=inc.trilokia.cardjuggler&referrer=utm_source%3Dinc.trilokia.pubgfxtool.free%26utm_medium%3Dkit",
            app.storeUrl(from = "inc.trilokia.pubgfxtool.free"),
        )
        assertEquals("https://example.com/x", PromoApp("a", "A", url = "https://example.com/x").storeUrl(from = "b"))
    }
}
