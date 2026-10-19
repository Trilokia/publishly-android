package inc.trilokia.publishly.kit.whatsnew

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.Locale

/**
 * Where the "what's new" text comes from. Pick one and hand it to [WhatsNew]:
 *
 * - [Play]: the "What's new" box of the app's Play listing, read from the public web page. Nothing
 *   to host; the text you type in Play Console is the text users see. Only the current version's
 *   notes are on the page, and Play can change its HTML at any time (PublishlyKit then logs and keeps
 *   showing the last copy it fetched). The page is about 1 MB, so it is fetched once a day at most.
 * - [Json]: a file on your server with every version ([Json] has the format). It also reads the
 *   `JsonUpdates` file as-is, so one file can feed both the update check and this screen.
 * - [GitHubReleases]: the repo's releases, tag + body (Markdown made plain).
 * - [GitHubCommits]: the latest commit subjects on a branch, one line each.
 *
 * GitHub's API allows 60 unsigned requests an hour per IP; with the daily cache that is plenty.
 */
sealed class WhatsNewSource : Serializable {
    /** Where to GET. */
    abstract val url: String

    /** Extra request headers, for the API calls. */
    open val headers: Map<String, String> get() = emptyMap()

    /** Turns the response body into notes, newest first. Throws when the body isn't what was expected. */
    abstract fun parse(body: String): List<ReleaseNote>

    data class Play(
        val packageName: String,
        /** The listing's language; your Play Console translations apply. */
        val language: String = Locale.getDefault().toLanguageTag(),
    ) : WhatsNewSource() {
        override val url get() = "https://play.google.com/store/apps/details?id=$packageName&hl=$language"
        override val headers get() = mapOf("User-Agent" to BROWSER_UA, "Accept-Language" to language)
        override fun parse(body: String) = parsePlayListing(body)
    }

    /**
     * ```json
     * {
     *   "releases": [
     *     { "version": "0.21.7", "versionCode": 193, "date": "2026-10-08",
     *       "notes": ["- New Feature: Added support for Flash Variant", "- Bug fixes"] }
     *   ]
     * }
     * ```
     * Newest first; `notes` may be one string; everything but `notes` is optional. The repo's
     * `whatsnew/release-notes.json` is a complete example. A `JsonUpdates` file (`latestVersion`,
     * `releaseNotes`...) is accepted too and gives one entry.
     */
    data class Json(override val url: String) : WhatsNewSource() {
        override val headers get() = mapOf("Accept" to "application/json")
        override fun parse(body: String) = parseJson(body)
    }

    /**
     * The same JSON, shipped inside the app (read it from `assets/`). Nothing is fetched: the
     * notes are whatever this build carries, which is enough for an app that writes its release
     * notes at build time, and for samples and tests.
     */
    data class Bundled(val json: String) : WhatsNewSource() {
        override val url get() = "bundled://release-notes"
        override fun parse(body: String) = parseJson(body)
    }

    data class GitHubReleases(
        val owner: String,
        val repo: String,
        val includePrereleases: Boolean = false,
        val count: Int = 10,
    ) : WhatsNewSource() {
        override val url get() = "https://api.github.com/repos/$owner/$repo/releases?per_page=$count"
        override val headers get() = GITHUB_HEADERS
        override fun parse(body: String) = parseGitHubReleases(body, includePrereleases)
    }

    data class GitHubCommits(
        val owner: String,
        val repo: String,
        /** Null means the repo's default branch. */
        val branch: String? = null,
        val count: Int = 30,
        /** Leave out "Merge pull request..." and "Merge branch..." commits. */
        val skipMerges: Boolean = true,
    ) : WhatsNewSource() {
        override val url get() = "https://api.github.com/repos/$owner/$repo/commits?per_page=$count" + (branch?.let { "&sha=$it" } ?: "")
        override val headers get() = GITHUB_HEADERS
        override fun parse(body: String) = parseGitHubCommits(body, skipMerges)
    }

    companion object {
        private const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
        private val GITHUB_HEADERS = mapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to "PublishlyKit",
        )

        /**
         * The listing page's "What's new" box. Play marks the box as `<div itemprop="description">`
         * (the long description uses `data-g-id="description"` instead), so this doesn't depend on
         * the heading's language or on Play's generated class names.
         */
        fun parsePlayListing(html: String): List<ReleaseNote> {
            if (!html.contains("data-g-id=\"description\"")) {
                throw IllegalStateException("Not a Play listing page (blocked, or the app isn't published?)")
            }
            val box = Regex("<div itemprop=\"description\">(.*?)</div>", RegexOption.DOT_MATCHES_ALL).find(html)
                ?: return emptyList() // the listing has no "What's new" text
            val text = PlainText.fromHtml(box.groupValues[1])
            if (text.isBlank()) return emptyList()
            return listOf(ReleaseNote(version = null, notes = text))
        }

        fun parseJson(json: String): List<ReleaseNote> {
            val o = JSONObject(json)
            if (o.has("latestVersion")) {
                // A JsonUpdates file: one version
                return listOf(
                    ReleaseNote(
                        version = o.getString("latestVersion"),
                        versionCode = o.opt("latestVersionCode")?.toString()?.trim()?.toIntOrNull(),
                        notes = lines(o.opt("releaseNotes")),
                        url = o.optString("url").ifEmpty { null },
                    ),
                )
            }
            val arr = o.getJSONArray("releases")
            return (0 until arr.length()).map { i ->
                val r = arr.getJSONObject(i)
                ReleaseNote(
                    version = r.optString("version").ifEmpty { null },
                    versionCode = if (r.has("versionCode")) r.get("versionCode").toString().trim().toIntOrNull() else null,
                    date = r.optString("date").ifEmpty { null },
                    notes = lines(r.opt("notes")),
                    url = r.optString("url").ifEmpty { null },
                )
            }
        }

        fun parseGitHubReleases(json: String, includePrereleases: Boolean): List<ReleaseNote> {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { i ->
                val r = arr.getJSONObject(i)
                if (r.optBoolean("draft")) return@mapNotNull null
                if (r.optBoolean("prerelease") && !includePrereleases) return@mapNotNull null
                ReleaseNote(
                    version = r.optString("tag_name").ifEmpty { r.optString("name") }.ifEmpty { null },
                    date = r.optString("published_at").take(10).ifEmpty { null },
                    notes = PlainText.fromMarkdown(r.optString("body")),
                    url = r.optString("html_url").ifEmpty { null },
                )
            }
        }

        fun parseGitHubCommits(json: String, skipMerges: Boolean): List<ReleaseNote> {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { i ->
                val c = arr.getJSONObject(i)
                val commit = c.getJSONObject("commit")
                val message = commit.getString("message")
                if (skipMerges && message.startsWith("Merge ")) return@mapNotNull null
                ReleaseNote(
                    version = c.getString("sha").take(7),
                    date = commit.optJSONObject("author")?.optString("date")?.take(10)?.ifEmpty { null },
                    notes = message.lineSequence().first().trim(),
                    url = c.optString("html_url").ifEmpty { null },
                )
            }
        }

        /** A JSON string, or an array of lines, as one newline-separated string. */
        private fun lines(value: Any?): String = when (value) {
            null -> ""
            is JSONArray -> (0 until value.length()).joinToString("\n") { value.getString(it).trimEnd() }.trim()
            else -> value.toString().trim()
        }
    }
}
