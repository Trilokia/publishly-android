package inc.trilokia.publishly.kit.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextTest {
    @Test
    fun `html to lines`() {
        assertEquals(
            "Major update\n• New: thing\n• Fixed &amp; done <b>\n\nThanks",
            PlainText.fromHtml("<b>Major update</b><br>• New: thing<br/><i>• Fixed &amp;amp; done &lt;b&gt;</i><br><br><br>Thanks"),
        )
        assertEquals("One\n• a\n• b", PlainText.fromHtml("<p>One</p><ul><li>a</li><li>b</li></ul>"))
        assertEquals("Tom & Jerry ’ €", PlainText.fromHtml("Tom &amp; Jerry &#39; &#x20AC;".replace("&#39;", "&rsquo;")))
    }

    @Test
    fun `markdown to lines`() {
        assertEquals(
            "Features\n\n- bold text and code\n- a link\n\nFixes\n- nested star stays",
            PlainText.fromMarkdown("## Features\r\n\r\n* **bold text** and `code`\n* [a link](https://x)\n\n### Fixes\n- nested *star* stays"),
        )
    }
}
