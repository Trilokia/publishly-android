package inc.trilokia.publishly.kit.whatsnew

/** One version's "what's new" text, newest first in every list PublishlyKit returns. */
data class ReleaseNote(
    /** "0.21.7", a tag like "v1.4", or a short commit hash. Null when the source doesn't say (Play). */
    val version: String?,
    /** The Android versionCode, when the source knows it (your JSON file). Lets [WhatsNew.pending] pick exactly the versions a user skipped. */
    val versionCode: Int? = null,
    /** "2026-10-13" or whatever the source gives; for showing, not for sorting. */
    val date: String? = null,
    /** Plain text, lines separated by newlines. HTML and Markdown are already stripped. */
    val notes: String,
    /** Somewhere to read more: the release or commit page. */
    val url: String? = null,
) {
    /** "Version 0.21.7", "0.21.7 · 2026-10-13", or the date alone, for a list heading. */
    val heading: String
        get() = listOfNotNull(version, date).joinToString("  ·  ")
}
