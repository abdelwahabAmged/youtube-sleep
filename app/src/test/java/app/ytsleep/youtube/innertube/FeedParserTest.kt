package app.ytsleep.youtube.innertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class FeedParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadFixture(name: String): kotlinx.serialization.json.JsonObject {
        val stream = javaClass.classLoader!!.getResourceAsStream("innertube/$name")
            ?: error("Missing fixture: $name")
        return json.parseToJsonElement(stream.reader().readText()).jsonObject
    }

    @Test
    fun `home fixture produces MediaItems with correct fields`() {
        val root = loadFixture("home.json")
        val (items, continuation) = FeedParser.parse(root)
        val normal = items.find { it.videoId == "abc12345678" }
        assertNotNull(normal)
        assertEquals("Rain sounds 10 hours", normal!!.title)
        assertEquals("Relaxing Channel", normal.channelName)
        assertEquals(36000, normal.durationSeconds)
        assertTrue(normal.thumbnailUrl.isNotEmpty())
        assertFalse(normal.isMarkedShort)
    }

    @Test
    fun `reelItemRenderer is marked as short`() {
        val root = loadFixture("home.json")
        val (items, _) = FeedParser.parse(root)
        val short = items.find { it.videoId == "short123456" }
        assertNotNull(short)
        assertTrue(short!!.isMarkedShort)
    }

    @Test
    fun `adSlotRenderer produces no items`() {
        val root = loadFixture("home.json")
        val (items, _) = FeedParser.parse(root)
        // ads should not appear in items
        assertTrue(items.none { it.title.contains("ad", ignoreCase = true) && it.videoId.isEmpty() })
        // total items: abc12345678, short123456, xyz98765432 = 3 (no ad)
        assertEquals(3, items.size)
    }

    @Test
    fun `unknown renderers are skipped without exception`() {
        val root = loadFixture("home.json")
        // Should not throw
        val (items, _) = FeedParser.parse(root)
        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `continuation token is extracted`() {
        val root = loadFixture("home.json")
        val (_, continuation) = FeedParser.parse(root)
        assertEquals("cont_token_home_1", continuation)
    }

    @Test
    fun `continuation fixture parses`() {
        val root = loadFixture("home_continuation.json")
        val (items, _) = FeedParser.parse(root)
        assertEquals(1, items.size)
        assertEquals("cont11111111", items[0].videoId)
    }
}
