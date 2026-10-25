package inc.trilokia.publishly.kit.oss

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** [OpenSource.byName] and [OpenSource.nameOrder]: the names a View app's list showed out of order. */
class OpenSourceOrderTest {
    // AboutLibraries' Library class is built for Java 21 and the tests run on 17, so the rows are (name, uniqueId) pairs
    private data class Row(val name: String, val id: String = name)

    private fun List<Row>.sorted() = sortedWith(OpenSource.byName(Locale.ENGLISH, Row::name, Row::id))

    @Test
    fun lowercaseNamesSortAmongTheOthers() {
        val names = listOf(
            "sun-security-android", "Kotlin Stdlib", "libadb-android", "AboutLibraries Core Library",
            "spake2-java", "Lifecycle Runtime", "Bouncy Castle", "app-update", "Zstd", "Émoji2",
        )
        val sorted = names.map { Row(it) }.sorted().map { it.name }
        assertEquals(
            listOf(
                "AboutLibraries Core Library", "app-update", "Bouncy Castle", "Émoji2", "Kotlin Stdlib",
                "libadb-android", "Lifecycle Runtime", "spake2-java", "sun-security-android", "Zstd",
            ),
            sorted,
        )
    }

    @Test
    fun equalNamesFallBackToUniqueId() {
        val libraries = listOf(
            Row("Annotation", "androidx.collection:annotation"),
            Row("annotation", "androidx.annotation:annotation"),
            Row("Annotation", "androidx.annotation:annotation-jvm"),
        )
        assertEquals(
            listOf("androidx.annotation:annotation", "androidx.annotation:annotation-jvm", "androidx.collection:annotation"),
            libraries.sorted().map { it.id },
        )
    }

    @Test
    fun licenceGroupsSortTheSameWayAndNeverMerge() {
        val groups = listOf("MIT License", "apache License 2.0", "Bouncy Castle Licence (MIT)", "MIT license")
            .associateWith { it }.toSortedMap(OpenSource.nameOrder(Locale.ENGLISH))
        assertEquals(listOf("apache License 2.0", "Bouncy Castle Licence (MIT)", "MIT License", "MIT license"), groups.keys.toList())
    }
}
