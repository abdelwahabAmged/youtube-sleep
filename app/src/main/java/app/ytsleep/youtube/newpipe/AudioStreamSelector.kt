package app.ytsleep.youtube.newpipe

import android.net.Uri
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

    /**
     * Highest bitrate worth downloading. Opus at ~70 kbps sounds the same as 160 kbps for speech,
     * music and sleep sounds at bedtime volume, and uses ~31 MB/hour instead of ~70 MB/hour.
     */
    const val DATA_SAVER_MAX_KBPS = 80

    /**
     * Index of the best candidate: the original-language track first (never YouTube's auto-dubbed
     * or descriptive tracks while an original exists), then a stream within the data budget.
     */
    fun bestIndex(candidates: List<Candidate>): Int {
        if (candidates.isEmpty()) throw ContentError.Unplayable("No audio-only streams available")
        return candidates.indices.maxWith(
            compareBy<Int>(
                { trackRank(candidates[it].trackType) },
                { if (candidates[it].bitrateKbps <= DATA_SAVER_MAX_KBPS) 1 else 0 },
                // Within the budget: Opus first, then the highest bitrate.
                // Above it: the smallest stream first, whatever the codec.
                { if (withinBudget(candidates[it])) opus(candidates[it]) else -candidates[it].bitrateKbps },
                { if (withinBudget(candidates[it])) candidates[it].bitrateKbps else opus(candidates[it]) },
            )
        )
    }

    private fun withinBudget(c: Candidate) = c.bitrateKbps <= DATA_SAVER_MAX_KBPS
    private fun opus(c: Candidate) = if (c.mimeType.contains("webm")) 1 else 0

    private fun trackRank(type: AudioTrackType?): Int = when (type) {
        AudioTrackType.ORIGINAL -> 3
        null -> 2
        AudioTrackType.SECONDARY -> 1
        AudioTrackType.DUBBED, AudioTrackType.DESCRIPTIVE -> 0
    }

    fun select(streams: List<NewPipeAudioStream>): AudioStream {
        // Only direct HTTP files can be read with ranged requests; skip HLS/DASH manifests.
        val playable = streams.filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val best = playable[
            bestIndex(playable.map { Candidate(it.format?.mimeType.orEmpty(), it.averageBitrate, it.audioTrackType) })
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
