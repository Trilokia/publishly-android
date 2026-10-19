package inc.trilokia.publishly.kit.whatsnew

/** Turns the little HTML Play allows, and the Markdown GitHub releases use, into plain lines. Pure Kotlin, so it is unit-tested on the JVM. */
internal object PlainText {
    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "hellip" to "…", "mdash" to "—", "ndash" to "–", "bull" to "•", "rsquo" to "’", "lsquo" to "‘",
        "rdquo" to "”", "ldquo" to "“", "copy" to "©", "reg" to "®", "trade" to "™",
    )

    fun fromHtml(html: String): String {
        var s = html
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)<li[^>]*>"), "• ")
        s = s.replace(Regex("(?i)</(p|div|li|h[1-6])>"), "\n")
        s = s.replace(Regex("<[^>]+>"), "")
        s = unescape(s)
        return tidy(s)
    }

    /** Drops heading marks, emphasis, code ticks and link targets; keeps list dashes. */
    fun fromMarkdown(md: String): String {
        var s = md.replace("\r\n", "\n")
        s = s.replace(Regex("(?m)^[ \\t]{0,3}#{1,6}[ \\t]+"), "")
        s = s.replace(Regex("(?m)^[ \\t]{0,3}[*+][ \\t]+"), "- ")
        s = s.replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        s = s.replace(Regex("(\\*\\*|__)(.+?)\\1"), "$2")
        s = s.replace(Regex("(?<![*\\w])[*_](?=\\S)(.+?)(?<=\\S)[*_](?![*\\w])"), "$1")
        s = s.replace("`", "")
        return tidy(s)
    }

    fun unescape(s: String): String = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);").replace(s) { m ->
        val e = m.groupValues[1]
        when {
            e.startsWith("#x") -> e.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
            e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
            else -> named[e] ?: m.value
        }
    }

    private fun tidy(s: String): String = s.lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
