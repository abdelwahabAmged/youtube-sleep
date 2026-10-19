package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.ContentFilter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** YouTube's 2026 `lockupViewModel` layout (fixtures are synthetic, modelled on real responses). */
class LockupFormatTest {

    private fun fixture(name: String): JsonObject {
        val stream = javaClass.classLoader!!.getResourceAsStream("innertube/$name") ?: error("Missing fixture: $name")
        return Json.parseToJsonElement(stream.reader().readText()).jsonObject
    }

    @Test
    fun `feed lockups become videos with title, channel, duration and a list-sized thumbnail`() {
        val (items, next) = FeedParser.parse(fixture("home_lockup.json"))
        val ocean = items.single { it.videoId == "lockVid0001" }
        assertEquals("Ocean waves for sleep", ocean.title)
        assertEquals("Calm Channel", ocean.channelName)
        assertEquals(3723, ocean.durationSeconds)
        assertEquals("https://i.ytimg.com/vi/lockVid0001/hq720.jpg?small", ocean.thumbnailUrl)
        assertEquals("FEED_NEXT_TOKEN", next)
    }

    @Test
    fun `feed drops shorts, members-only, live, upcoming and playlist lockups`() {
        val ids = ContentFilter.filter(FeedParser.parse(fixture("home_lockup.json")).first).map { it.videoId }
        assertEquals(listOf("lockVid0001"), ids)
    }

    @Test
    fun `library lockups become playlists with counts, without Saved Shorts`() {
        val lists = LibraryParser.parse(fixture("library_lockup.json"))
        assertEquals(listOf("LL", "WL", "PLfakePlaylist01", "PLfakePodcast01"), lists.map { it.playlistId })
        val wl = lists.single { it.playlistId == "WL" }
        assertEquals(1036, wl.itemCount)
        assertEquals("https://i.ytimg.com/vi/WLthumb/hqdefault.jpg", wl.thumbnailUrl)
        assertEquals(3, lists.single { it.playlistId == "PLfakePodcast01" }.itemCount)
    }

    @Test
    fun `playlist continuation nested in commandExecutorCommand is found`() {
        val (items, next) = PlaylistParser.parse(fixture("playlist_WL_2026.json"))
        assertEquals("WL_NEXT_TOKEN", next)
        assertEquals(listOf("wlVideo0001"), items.map { it.videoId })
        assertEquals("https://i.ytimg.com/vi/wlVideo0001/hqdefault.jpg", items.single().thumbnailUrl)
    }

    @Test
    fun `continuation token lookup`() {
        assertNull(LockupParser.continuationToken(fixture("library_lockup.json")))
        assertTrue(LockupParser.parseDuration("1:02:03") == 3723)
        assertFalse(LockupParser.parseDuration("LIVE") != null)
    }
}
