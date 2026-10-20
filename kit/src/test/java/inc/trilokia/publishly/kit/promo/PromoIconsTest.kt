package inc.trilokia.publishly.kit.promo

import inc.trilokia.publishly.kit.promo.PromoIcons.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class PromoIconsTest {
    // The server template (web icons only) and the sample's bundled copy of it (icons shipped as assets, the web ones as the backup)
    private val server = PromoFeed.parse(File("../promo/apps.json").readText())
    private val bundled = PromoFeed.parse(javaClass.getResource("/promo/apps_bundled.json")!!.readText())

    @Test
    fun `a web icon is tried once, with nothing after it`() {
        val card = server.apps.first()
        assertNull(card.iconFallbackUrl)
        assertEquals(listOf(Source.Web(card.iconUrl!!)), PromoIcons.sources(card))
    }

    @Test
    fun `an asset icon is tried first, the web copy after it`() {
        val card = bundled.apps.first()
        assertEquals("asset:promo/inc.trilokia.cardjuggler.webp", card.iconUrl)
        assertEquals(
            listOf(
                Source.Asset("promo/inc.trilokia.cardjuggler.webp"),
                Source.Web("https://play-lh.googleusercontent.com/wuQzpb-Zc7LkdVVG2Douyw35zf3qNTRtE99-VNPVTYsG6flBsf78g__-Yzpyn_hUJQ3sIBvfbJn_m6wQZTYlIQ=s256"),
            ),
            PromoIcons.sources(card),
        )
    }

    @Test
    fun `the bundled copy names the same web icons as the server`() {
        assertEquals(server.apps.map { it.iconUrl }, bundled.apps.map { it.iconFallbackUrl })
        assertEquals(server.apps.map { it.packageName }, bundled.apps.map { it.packageName })
    }

    @Test
    fun `the asset forms`() {
        assertEquals(Source.Asset("promo/x.webp"), PromoIcons.source("asset:promo/x.webp"))
        assertEquals(Source.Asset("promo/x.webp"), PromoIcons.source("asset:///promo/x.webp"))
        assertEquals(Source.Asset("promo/x.webp"), PromoIcons.source("file:///android_asset/promo/x.webp"))
        assertEquals(Source.Asset("promo/x.webp"), PromoIcons.source("  asset:promo/x.webp "))
        assertEquals(Source.Web("HTTPS://example.com/i.png"), PromoIcons.source("HTTPS://example.com/i.png"))
    }

    @Test
    fun `blank, an empty asset path and anything else is nothing`() {
        assertNull(PromoIcons.source(null))
        assertNull(PromoIcons.source(""))
        assertNull(PromoIcons.source("asset:"))
        assertNull(PromoIcons.source("asset:///"))
        assertNull(PromoIcons.source("promo/x.webp"))
        assertNull(PromoIcons.source("content://media/1"))
        assertEquals(emptyList<Source>(), PromoIcons.sources(PromoApp("a.b", "AB")))
    }

    @Test
    fun `the same place named twice is tried once`() {
        val app = PromoApp("a.b", "AB", iconUrl = "https://x/i.png", iconFallbackUrl = "https://x/i.png")
        assertEquals(listOf(Source.Web("https://x/i.png")), PromoIcons.sources(app))
    }

    @Test
    fun `an app with only a web backup still gets it`() {
        val app = PromoFeed.parse("""{"apps":[{"package":"a.b","name":"AB","iconUrl":"https://x/i.png"}]}""").apps.single()
        assertNull(app.iconUrl)
        assertEquals(listOf(Source.Web("https://x/i.png")), PromoIcons.sources(app))
    }
}
