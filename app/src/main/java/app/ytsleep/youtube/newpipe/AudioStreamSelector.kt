package app.ytsleep.youtube.newpipe

import android.net.Uri
import app.ytsleep.youtube.AudioStream
import app.ytsleep.youtube.ContentError
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.AudioStream as NewPipeAudioStream

object AudioStreamSelector {

    data class Candidate(val mimeType: String, val bitrate: Int)

    /** Index of the best candidate: Opus/WebM first, then highest bitrate. */
    fun bestIndex(candidates: List<Candidate>): Int {
        if (candidates.isEmpty()) throw ContentError.Unplayable("No audio-only streams available")
        return candidates.indices.maxWith(
            compareBy<Int>({ if (candidates[it].mimeType.contains("webm")) 1 else 0 }, { candidates[it].bitrate })
        )
    }

    fun select(streams: List<NewPipeAudioStream>): AudioStream {
        // Only direct HTTP files can be read with ranged requests; skip HLS/DASH manifests.
        val playable = streams.filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val best = playable[bestIndex(playable.map { Candidate(it.format?.mimeType.orEmpty(), it.averageBitrate) })]
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
