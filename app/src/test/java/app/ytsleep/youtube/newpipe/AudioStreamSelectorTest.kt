package app.ytsleep.youtube.newpipe

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

    @Test(expected = ContentError.Unplayable::class)
    fun `throws Unplayable when no audio-only streams`() {
        AudioStreamSelector.bestIndex(emptyList())
    }
}
