package inc.trilokia.publishly.kit.oss

/**
 * Licence texts as AboutLibraries supplies them are hard-wrapped at about 72 columns (the SPDX
 * copies of Apache-2.0, GPL, LGPL), so on a phone every line wraps a second time into ragged half
 * lines. [reflow] undoes the hard wrap for display. Used by [OpenSourceActivity] and
 * `OpenSourceScreen`; pure Kotlin, so it is tested on the JVM against real licence texts.
 */
object LicenceText {
    /** A line this long (trimmed) is a wrapped line of prose, not a title or an address line. */
    private const val PROSE_LINE = 40

    /**
     * Splits [text] into blocks on blank lines. A block where most lines are prose length becomes
     * one line (lines trimmed, joined by single spaces); any other block (a title, "Version 3, 29
     * June 2007", an address, a list of short lines) keeps its lines, trimmed. Blocks stay apart by
     * one blank line. No word is lost or moved.
     *
     * Two refinements, both from real texts: a block's last line counts as prose length, since the
     * last line of a wrapped paragraph is short by nature ("…copying, distribution and modification"
     * / "follow."); and in a prose block, short lines before the first long one are a heading and
     * keep their own line (OFL's "PREAMBLE" sits right above its paragraph).
     */
    fun reflow(text: String): String {
        val blocks = mutableListOf<List<String>>()
        var block = mutableListOf<String>()
        for (line in text.lines()) {
            if (line.isBlank()) {
                if (block.isNotEmpty()) blocks += block
                block = mutableListOf()
            } else {
                block += line.trim()
            }
        }
        if (block.isNotEmpty()) blocks += block
        return blocks.joinToString("\n\n") { lines ->
            val prose = (lines.dropLast(1).count { it.length >= PROSE_LINE } + 1) * 2 > lines.size
            if (prose) {
                val heading = lines.indexOfFirst { it.length >= PROSE_LINE }.coerceAtLeast(0)
                (lines.take(heading) + lines.drop(heading).joinToString(" ").replace(SPACES, " ")).joinToString("\n")
            } else {
                lines.joinToString("\n")
            }
        }
    }

    private val SPACES = Regex("\\s+")
}
