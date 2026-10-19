package app.ytsleep.youtube.newpipe

import android.net.Uri
import app.ytsleep.youtube.AudioQuality
import app.ytsleep.youtube.AudioStream
import app.ytsleep.youtube.ContentError
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.AudioStream as NewPipeAudioStream

object AudioStreamSelector {

    /**
     * [bitrateKbps] as reported by NewPipe (kbit/s). [trackType] is YouTube's label for the track
     * (ORIGINAL, DUBBED, ...), null when a video has a single track.
     */
    data class Candidate(val mimeType: String, val bitrateKbps: Int, val trackType: AudioTrackType? = null)

    /** Upper bitrate for [AudioQuality.NORMAL]: Opus 70 kbps in practice, ~31 MB per hour. */
    const val NORMAL_MAX_KBPS = 80

    /**
     * Index of the best candidate for [quality]. The original-language track always comes first
     * (never YouTube's auto-dubbed or descriptive tracks while an original exists). Then:
     * - DATA_SAVER: the smallest stream.
     * - NORMAL: the best stream within [NORMAL_MAX_KBPS], else the smallest above it.
     * - HIGH: the best stream.
     * Ties go to Opus/WebM, which sounds better than AAC at the same bitrate.
     */
    fun bestIndex(candidates: List<Candidate>, quality: AudioQuality = AudioQuality.NORMAL): Int {
        if (candidates.isEmpty()) throw ContentError.Unplayable("No audio-only streams available")
        val byTrack = compareBy<Int> { trackRank(candidates[it].trackType) }
        val order = when (quality) {
            AudioQuality.DATA_SAVER -> byTrack
                // An unknown bitrate (<= 0) must not pass for the smallest stream.
                .thenBy { -(candidates[it].bitrateKbps.takeIf { kbps -> kbps > 0 } ?: Int.MAX_VALUE / 2) }
                .thenBy { opus(candidates[it]) }
            AudioQuality.NORMAL -> byTrack
                .thenBy { if (withinBudget(candidates[it])) 1 else 0 }
                // Within the budget: Opus first, then the highest bitrate.
                // Above it: the smallest stream first, whatever the codec.
                .thenBy { if (withinBudget(candidates[it])) opus(candidates[it]) else -candidates[it].bitrateKbps }
                .thenBy { if (withinBudget(candidates[it])) candidates[it].bitrateKbps else opus(candidates[it]) }
            AudioQuality.HIGH -> byTrack
                .thenBy { candidates[it].bitrateKbps }
                .thenBy { opus(candidates[it]) }
        }
        return candidates.indices.maxWith(order)
    }

    private fun withinBudget(c: Candidate) = c.bitrateKbps <= NORMAL_MAX_KBPS
    private fun opus(c: Candidate) = if (c.mimeType.contains("webm")) 1 else 0

    private fun trackRank(type: AudioTrackType?): Int = when (type) {
        AudioTrackType.ORIGINAL -> 3
        null -> 2
        AudioTrackType.SECONDARY -> 1
        AudioTrackType.DUBBED, AudioTrackType.DESCRIPTIVE -> 0
    }

    fun select(streams: List<NewPipeAudioStream>, quality: AudioQuality): AudioStream {
        // Only direct HTTP files can be read with ranged requests; skip HLS/DASH manifests.
        val playable = streams.filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val best = playable[
            bestIndex(playable.map { Candidate(it.format?.mimeType.orEmpty(), it.averageBitrate, it.audioTrackType) }, quality)
        ]
        return AudioStream(
            url = best.content,
            mimeType = best.format?.mimeType ?: "audio/webm",
            bitrate = best.averageBitrate,
            contentLength = best.itagItem?.contentLength?.takeIf { it > 0 },
            expiresAtEpochMs = Uri.parse(best.content).getQueryParameter("expire")?.toLongOrNull()
                // Refresh a few minutes before YouTube's own expiry.
                ?.let { it * 1000 - 5 * 60_000 },
        )
    }
}
