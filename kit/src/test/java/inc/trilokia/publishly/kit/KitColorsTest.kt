package inc.trilokia.publishly.kit

import org.junit.Assert.assertEquals
import org.junit.Test

class KitColorsTest {
    @Test
    fun `text on a colour is black on light, white on dark`() {
        assertEquals(0xFF000000.toInt(), KitColors.onColor(0xFFFFFFFF.toInt()))
        assertEquals(0xFF000000.toInt(), KitColors.onColor(0xFFEFEFEF.toInt()))   // framework Material Light primary
        assertEquals(0xFFFFFFFF.toInt(), KitColors.onColor(0xFF6750A4.toInt()))   // Material 3 purple
        assertEquals(0xFFFFFFFF.toInt(), KitColors.onColor(0xFF212121.toInt()))
    }
}
