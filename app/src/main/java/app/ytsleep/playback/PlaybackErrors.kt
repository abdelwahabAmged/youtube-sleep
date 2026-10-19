package app.ytsleep.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import app.ytsleep.youtube.ContentError

sealed class PlaybackFailure(val message: String) {
    /** The item itself can't be played; skipping to the next one makes sense. */
    class Unplayable(message: String) : PlaybackFailure(message)

    /** Connectivity was lost and retries ran out; skipping would only fail again. */
    class Network : PlaybackFailure("Connection lost. Playback stopped.")

    companion object {
        fun classify(error: PlaybackException): PlaybackFailure {
            val causes = generateSequence<Throwable>(error) { it.cause }.toList()
            causes.firstNotNullOfOrNull { it as? ContentError.Unplayable }?.let {
                return Unplayable("This item can't be played (${it.reason}).")
            }
            if (causes.any { it is ContentError.SourceChanged }) {
                return Unplayable("YouTube changed something. An app update is needed.")
            }
            val network = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                causes.any { it is ContentError.Offline }
            return if (network) Network() else Unplayable("This item can't be played.")
        }
    }
}

/** Keeps retrying network loads for roughly ten minutes so short outages at night don't stop playback. */
@UnstableApi
class PatientLoadErrorPolicy : DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount= */ 60) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val base = super.getRetryDelayMsFor(loadErrorInfo)
        if (base == C.TIME_UNSET) return C.TIME_UNSET
        return minOf(loadErrorInfo.errorCount * 2_000L, 10_000L)
    }
}
