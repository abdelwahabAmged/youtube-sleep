package app.ytsleep.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterTest {

    private fun item(
        videoId: String = "abc",
        durationSeconds: Int? = 300,
        isMarkedShort: Boolean = false,
        isLive: Boolean = false,
    ) = MediaItem(
        videoId = videoId,
        title = "title",
        channelName = "chan",
        channelId = null,
        thumbnailUrl = "https://example.com/thumb.jpg",
        durationSeconds = durationSeconds,
        isMarkedShort = isMarkedShort,
        isLive = isLive,
    )

    @Test
    fun `marked short with long duration is a Short`() {
        assertTrue(ContentFilter.isShort(item(isMarkedShort = true, durationSeconds = 3600)))
    }

    @Test
    fun `duration exactly 180 is a Short`() {
        assertTrue(ContentFilter.isShort(item(durationSeconds = 180)))
    }

    @Test
    fun `duration 181 is not a Short`() {
        assertFalse(ContentFilter.isShort(item(durationSeconds = 181)))
    }

    @Test
    fun `null duration not marked is not a Short`() {
        assertFalse(ContentFilter.isShort(item(durationSeconds = null, isMarkedShort = false)))
    }

    @Test
    fun `live with null duration is not a Short`() {
        assertFalse(ContentFilter.isShort(item(durationSeconds = null, isLive = true)))
    }

    @Test
    fun `filter keeps order and drops only Shorts`() {
        val items = listOf(
            item("keep1", 300),
            item("drop1", 180, isMarkedShort = false),
            item("keep2", 200),
            item("drop2", 120, isMarkedShort = true),
            item("keep3", null),
        )
        val result = ContentFilter.filter(items)
        assertEquals(listOf("keep1", "keep2", "keep3"), result.map { it.videoId })
    }

    @Test
    fun `isShortsUrl detects shorts path`() {
        assertTrue(ContentFilter.isShortsUrl("https://youtube.com/shorts/abc123"))
        assertFalse(ContentFilter.isShortsUrl("https://youtube.com/watch?v=abc123"))
    }
}
