package app.ytsleep.youtube.innertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class PlaylistParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun load(name: String) = json.parseToJsonElement(
        javaClass.classLoader!!.getResourceAsStream("innertube/$name")!!.reader().readText()
    ).jsonObject

    @Test
    fun `playlist items keep order and include lengthSeconds`() {
        val root = load("playlist_WL.json")
        val (items, _) = PlaylistParser.parse(root)
        assertTrue(items.isNotEmpty())
        val first = items[0]
        assertEquals("wl11111111", first.videoId)
        assertEquals("Watch later video", first.title)
        assertEquals(754, first.durationSeconds) // 12:34 = 754 seconds
    }

    @Test
    fun `playlist continuation is parsed`() {
        val root = load("playlist_continuation.json")
        val (items, _) = PlaylistParser.parse(root)
        assertEquals(1, items.size)
        assertEquals("plcont11111", items[0].videoId)
    }
}

class LibraryParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun load(name: String) = json.parseToJsonElement(
        javaClass.classLoader!!.getResourceAsStream("innertube/$name")!!.reader().readText()
    ).jsonObject

    @Test
    fun `library yields PlaylistSummary entries plus WL and LL`() {
        val root = load("library_playlists.json")
        val playlists = LibraryParser.parse(root)
        val ids = playlists.map { it.playlistId }
        assertTrue("WL" in ids)
        assertTrue("LL" in ids)
        assertTrue("PLabc1234" in ids)
    }

    @Test
    fun `WL and LL are always first`() {
        val root = load("library_playlists.json")
        val playlists = LibraryParser.parse(root)
        assertEquals("WL", playlists[0].playlistId)
        assertEquals("LL", playlists[1].playlistId)
    }
}
