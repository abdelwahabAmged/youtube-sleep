package app.ytsleep.youtube.newpipe

import app.ytsleep.youtube.AudioQuality
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.newpipe.AudioStreamSelector.Candidate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.extractor.stream.AudioTrackType

class AudioStreamSelectorTest {

    /** What YouTube typically offers for one video: Opus 50/70/160 and AAC 48/128 kbps. */
    private val typical = listOf(
        Candidate("audio/mp4", 48),
        Candidate("audio/mp4", 128),
        Candidate("audio/webm", 50),
        Candidate("audio/webm", 70),
        Candidate("audio/webm", 160),
    )

    @Test
    fun `picks 70 kbps opus, not the 160 kbps stream`() {
        assertEquals(3, AudioStreamSelector.bestIndex(typical))
    }

    @Test
    fun `without opus picks the AAC stream within the data budget`() {
        val candidates = listOf(Candidate("audio/mp4", 128), Candidate("audio/mp4", 48))
        assertEquals(1, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `when everything is above the budget the smallest wins`() {
        val candidates = listOf(Candidate("audio/webm", 160), Candidate("audio/mp4", 128))
        assertEquals(1, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `original language wins over a smaller auto-dubbed track`() {
        val candidates = listOf(
            Candidate("audio/webm", 70, AudioTrackType.DUBBED),
            Candidate("audio/webm", 70, AudioTrackType.DESCRIPTIVE),
            Candidate("audio/mp4", 128, AudioTrackType.ORIGINAL),
        )
        assertEquals(2, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `data budget applies among original tracks`() {
        val candidates = listOf(
            Candidate("audio/webm", 70, AudioTrackType.DUBBED),
            Candidate("audio/webm", 160, AudioTrackType.ORIGINAL),
            Candidate("audio/webm", 70, AudioTrackType.ORIGINAL),
        )
        assertEquals(2, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `dubbed track is used only when nothing else exists`() {
        assertEquals(0, AudioStreamSelector.bestIndex(listOf(Candidate("audio/webm", 70, AudioTrackType.DUBBED))))
    }

    @Test
    fun `data saver picks the smallest stream`() {
        // Opus 50 is not the smallest: AAC 48 is.
        assertEquals(0, AudioStreamSelector.bestIndex(typical, AudioQuality.DATA_SAVER))
    }

    @Test
    fun `data saver ignores a stream with unknown bitrate`() {
        val candidates = listOf(Candidate("audio/webm", -1), Candidate("audio/webm", 50))
        assertEquals(1, AudioStreamSelector.bestIndex(candidates, AudioQuality.DATA_SAVER))
    }

    @Test
    fun `high picks the best stream`() {
        assertEquals(4, AudioStreamSelector.bestIndex(typical, AudioQuality.HIGH))
    }

    @Test
    fun `every quality keeps the original language`() {
        val candidates = listOf(
            Candidate("audio/mp4", 48, AudioTrackType.DUBBED),
            Candidate("audio/webm", 160, AudioTrackType.DUBBED),
            Candidate("audio/webm", 70, AudioTrackType.ORIGINAL),
        )
        AudioQuality.entries.forEach { assertEquals(it.name, 2, AudioStreamSelector.bestIndex(candidates, it)) }
    }

    @Test(expected = ContentError.Unplayable::class)
    fun `throws Unplayable when no audio-only streams`() {
        AudioStreamSelector.bestIndex(emptyList())
    }
}
