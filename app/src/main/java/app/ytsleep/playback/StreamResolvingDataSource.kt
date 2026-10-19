package app.ytsleep.playback

import androidx.annotation.OptIn
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.HttpDataSource
import app.ytsleep.youtube.AudioStream
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.ContentSource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

private const val SCHEME = "ytsleep"
private const val CHUNK_SIZE = 10 * 1024 * 1024L
private const val DEFAULT_URL_LIFETIME_MS = 5 * 60 * 60 * 1000L

@OptIn(UnstableApi::class)
class StreamResolver(private val contentSource: ContentSource) {
    private data class Cached(val stream: AudioStream, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, Cached>()

    fun resolve(videoId: String): AudioStream {
        val now = System.currentTimeMillis()
        cache[videoId]?.takeIf { now < it.expiresAtMs }?.let { return it.stream }
        val stream = try {
            runBlocking { contentSource.resolveAudio(videoId) }
        } catch (e: ContentError.Offline) {
            // An IOException lets ExoPlayer's load-error policy retry until the network returns.
            throw java.io.IOException("Offline while resolving $videoId", e)
        }
        cache[videoId] = Cached(stream, stream.expiresAtEpochMs ?: (now + DEFAULT_URL_LIFETIME_MS))
        return stream
    }

    fun invalidate(videoId: String) {
        cache.remove(videoId)
    }
}

/**
 * Plays `ytsleep://<videoId>` by resolving a fresh audio URL and reading it in ≤10 MB ranged
 * requests (YouTube throttles unbounded reads), opening the next range until the stream ends.
 */
@OptIn(UnstableApi::class)
class StreamResolvingDataSource(
    private val httpFactory: HttpDataSource.Factory,
    private val resolver: StreamResolver,
) : BaseDataSource(/* isNetwork= */ true) {

    private var upstream: HttpDataSource? = null
    private var videoId: String = ""
    private var position = 0L
    private var endPosition = C.LENGTH_UNSET.toLong()
    private var totalLength = C.LENGTH_UNSET.toLong()
    private var currentUri: Uri? = null
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        videoId = dataSpec.uri.host ?: dataSpec.uri.pathSegments.firstOrNull()
            ?: throw java.io.IOException("Missing video id in ${dataSpec.uri}")
        position = dataSpec.position
        endPosition = if (dataSpec.length == C.LENGTH_UNSET.toLong()) C.LENGTH_UNSET.toLong()
        else dataSpec.position + dataSpec.length
        totalLength = C.LENGTH_UNSET.toLong()

        transferInitializing(dataSpec)
        openChunk()
        opened = true
        transferStarted(dataSpec)

        return when {
            endPosition != C.LENGTH_UNSET.toLong() -> endPosition - position
            totalLength != C.LENGTH_UNSET.toLong() -> totalLength - position
            else -> C.LENGTH_UNSET.toLong()
        }
    }

    private fun openChunk() {
        try {
            openChunkOnce()
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            if (e.responseCode != 403 && e.responseCode != 410) throw e
            // The signed URL expired (~6 h) or was revoked: resolve a fresh one and retry once.
            resolver.invalidate(videoId)
            openChunkOnce()
        }
    }

    private fun openChunkOnce() {
        val stream = resolver.resolve(videoId)
        if (totalLength == C.LENGTH_UNSET.toLong()) {
            stream.contentLength?.let { totalLength = it }
        }
        val limit = listOf(endPosition, totalLength)
            .filter { it != C.LENGTH_UNSET.toLong() }
            .minOrNull()
        val chunkEnd = if (limit == null) position + CHUNK_SIZE else minOf(limit, position + CHUNK_SIZE)

        val uri = Uri.parse(stream.url)
        val http = httpFactory.createDataSource()
        http.open(
            DataSpec.Builder()
                .setUri(uri)
                .setPosition(position)
                .setLength(chunkEnd - position)
                .build()
        )
        if (totalLength == C.LENGTH_UNSET.toLong()) {
            totalLength = parseTotalFromContentRange(http.responseHeaders)
        }
        upstream = http
        currentUri = uri
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (endPosition != C.LENGTH_UNSET.toLong() && position >= endPosition) return C.RESULT_END_OF_INPUT

        var read = upstream?.read(buffer, offset, length) ?: return C.RESULT_END_OF_INPUT
        if (read == C.RESULT_END_OF_INPUT) {
            val moreAvailable = totalLength != C.LENGTH_UNSET.toLong() && position < totalLength &&
                (endPosition == C.LENGTH_UNSET.toLong() || position < endPosition)
            if (!moreAvailable) return C.RESULT_END_OF_INPUT
            upstream?.close()
            openChunk()
            read = upstream?.read(buffer, offset, length) ?: return C.RESULT_END_OF_INPUT
            if (read == C.RESULT_END_OF_INPUT) return read
        }
        position += read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        try {
            upstream?.close()
        } finally {
            upstream = null
            currentUri = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    @OptIn(UnstableApi::class)
    class Factory(contentSource: ContentSource, client: OkHttpClient) : DataSource.Factory {
        private val resolver = StreamResolver(contentSource)
        // The app-wide OkHttp client keeps connections alive, so each 10 MB range request reuses
        // the open TLS connection instead of handshaking again.
        private val httpFactory = OkHttpDataSource.Factory(client)

        override fun createDataSource(): DataSource = StreamResolvingDataSource(httpFactory, resolver)
    }

    private companion object {
        fun parseTotalFromContentRange(headers: Map<String, List<String>>): Long {
            val value = headers.entries
                .firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
                ?.value?.firstOrNull() ?: return C.LENGTH_UNSET.toLong()
            return value.substringAfterLast('/').trim().toLongOrNull() ?: C.LENGTH_UNSET.toLong()
        }
    }
}
