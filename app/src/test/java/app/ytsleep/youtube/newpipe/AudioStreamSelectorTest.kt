package app.ytsleep.youtube.newpipe

import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.newpipe.AudioStreamSelector.Candidate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.extractor.stream.AudioTrackType

class AudioStreamSelectorTest {

    @Test
    fun `prefers highest bitrate opus webm`() {
        val candidates = listOf(
            Candidate("audio/webm", 64_000),
            Candidate("audio/webm", 160_000),
            Candidate("audio/mp4", 256_000),
        )
        assertEquals(1, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `falls back to m4a aac when no opus`() {
        val candidates = listOf(
            Candidate("audio/mp4", 128_000),
            Candidate("audio/mp4", 64_000),
        )
        assertEquals(0, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `original language wins over a better-quality auto-dubbed track`() {
        val candidates = listOf(
            Candidate("audio/webm", 160_000, AudioTrackType.DUBBED),
            Candidate("audio/webm", 160_000, AudioTrackType.DESCRIPTIVE),
            Candidate("audio/mp4", 128_000, AudioTrackType.ORIGINAL),
        )
        assertEquals(2, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `best quality among original tracks`() {
        val candidates = listOf(
            Candidate("audio/webm", 160_000, AudioTrackType.DUBBED),
            Candidate("audio/webm", 70_000, AudioTrackType.ORIGINAL),
            Candidate("audio/webm", 160_000, AudioTrackType.ORIGINAL),
        )
        assertEquals(2, AudioStreamSelector.bestIndex(candidates))
    }

    @Test
    fun `dubbed track is used only when nothing else exists`() {
        assertEquals(0, AudioStreamSelector.bestIndex(listOf(Candidate("audio/webm", 160_000, AudioTrackType.DUBBED))))
    }

    @Test(expected = ContentError.Unplayable::class)
    fun `throws Unplayable when no audio-only streams`() {
        AudioStreamSelector.bestIndex(emptyList())
    }
}
