package app.ytsleep.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRulesTest {

    @Test
    fun `position within 30s of end is stored as 0`() {
        val saved = HistoryRules.savePosition(
            positionMs = (600 - 20) * 1000L,
            durationSeconds = 600,
        )
        assertEquals(0L, saved)
    }

    @Test
    fun `position exactly at 30s before end is stored as 0`() {
        val saved = HistoryRules.savePosition(
            positionMs = (600 - 30) * 1000L,
            durationSeconds = 600,
        )
        assertEquals(0L, saved)
    }

    @Test
    fun `position before 30s of end is stored as is`() {
        val saved = HistoryRules.savePosition(
            positionMs = (600 - 31) * 1000L,
            durationSeconds = 600,
        )
        assertEquals((600 - 31) * 1000L, saved)
    }

    @Test
    fun `null duration stores position as is`() {
        val posMs = 12345L
        val saved = HistoryRules.savePosition(positionMs = posMs, durationSeconds = null)
        assertEquals(posMs, saved)
    }

    @Test
    fun `prune threshold is 500`() {
        assertEquals(500, HistoryRules.MAX_HISTORY)
    }
}
