package app.ytsleep.youtube

import android.content.Context
import app.ytsleep.youtube.innertube.FeedParser
import app.ytsleep.youtube.innertube.InnerTubeClient
import app.ytsleep.youtube.innertube.LibraryParser
import app.ytsleep.youtube.innertube.PlaylistParser
import app.ytsleep.youtube.newpipe.AudioStreamSelector
import app.ytsleep.youtube.newpipe.NewPipeMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.schabi.newpipe.extractor.Page as NpPage

private const val VIDEOS_FILTER = "videos"
private val PRIVATE_PLAYLISTS = setOf("WL", "LL")

class YouTubeContentSource(
    client: OkHttpClient,
    context: Context,
) : ContentSource {

    private val yt = ServiceList.YouTube
    private val innerTube = InnerTubeClient(client, context)

    /**
     * NewPipe continuation pages carry an id/body, not just a URL, so they are kept here and the
     * UI only ever sees an opaque token.
     */
    private data class NewPipeCursor(val url: String, val page: NpPage)

    private val cursors = ConcurrentHashMap<String, NewPipeCursor>()

    private fun remember(url: String, page: NpPage?): String? =
        page?.let { UUID.randomUUID().toString().also { token -> cursors[token] = NewPipeCursor(url, page) } }

    override suspend fun search(query: String, nextToken: String?): Page<MediaItem> = io {
        val extractor = yt.getSearchExtractor(query, listOf(VIDEOS_FILTER), "")
        val page: InfoItemsPage<InfoItem> = if (nextToken == null) {
            extractor.fetchPage()
            extractor.initialPage
        } else {
            val cursor = cursors.remove(nextToken) ?: throw ContentError.SourceChanged("Search page expired")
            extractor.getPage(cursor.page)
        }
        Page(items = mapStreams(page.items), nextToken = remember(query, page.nextPage))
    }

    override suspend fun feed(kind: FeedKind, nextToken: String?): Page<MediaItem> = io {
        val browseId = when (kind) {
            FeedKind.HOME -> "FEwhat_to_watch"
            FeedKind.SUBSCRIPTIONS -> "FEsubscriptions"
        }
        val json = if (nextToken == null) innerTube.browse(browseId) else innerTube.continuation(nextToken)
        val (items, continuation) = FeedParser.parse(json)
        Page(items = ContentFilter.filter(items), nextToken = continuation)
    }

    override suspend fun myPlaylists(): List<PlaylistSummary> = io {
        LibraryParser.parse(innerTube.browse("FEplaylist_aggregation"))
    }

    override suspend fun playlistItems(playlistId: String, nextToken: String?): Page<MediaItem> = io {
        val publicCursor = nextToken?.let { cursors[it] }
        if (publicCursor != null) {
            cursors.remove(nextToken)
            publicPlaylistPage(publicCursor.url, publicCursor.page)
        } else {
            try {
                val json = if (nextToken == null) innerTube.browse("VL$playlistId")
                else innerTube.continuation(nextToken)
                val (items, continuation) = PlaylistParser.parse(json)
                Page(items = ContentFilter.filter(items), nextToken = continuation)
            } catch (e: ContentError.NotSignedIn) {
                // Watch later and Liked are private: only the signed-in path can open them.
                if (playlistId in PRIVATE_PLAYLISTS) throw e
                // Other playlists may be public and still work through NewPipe.
                publicPlaylistPage("https://www.youtube.com/playlist?list=$playlistId", null)
            }
        }
    }

    private fun publicPlaylistPage(url: String, page: NpPage?): Page<MediaItem> {
        val extractor = yt.getPlaylistExtractor(url)
        val result = if (page == null) {
            extractor.fetchPage()
            extractor.initialPage
        } else {
            extractor.getPage(page)
        }
        return Page(items = mapStreams(result.items), nextToken = remember(url, result.nextPage))
    }

    override suspend fun addToPlaylist(playlistId: String, videoId: String) = io {
        innerTube.addToPlaylist(playlistId, videoId)
    }

    override suspend fun resolveAudio(videoId: String): AudioStream = io {
        val info = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")
        if (info.audioStreams.isEmpty()) throw ContentError.Unplayable("No audio stream")
        AudioStreamSelector.select(info.audioStreams)
    }

    private fun mapStreams(items: List<InfoItem>): List<MediaItem> =
        ContentFilter.filter(items.filterIsInstance<StreamInfoItem>().map(NewPipeMapper::toMediaItem))

    /** Runs [block] on IO and maps every failure into the [ContentError] contract. */
    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: ContentError) {
            throw e
        } catch (e: AgeRestrictedContentException) {
            throw ContentError.Unplayable("Age-restricted")
        } catch (e: GeographicRestrictionException) {
            throw ContentError.Unplayable("Not available in this region")
        } catch (e: ContentNotAvailableException) {
            throw ContentError.Unplayable(e.message ?: "Not available")
        } catch (e: IOException) {
            throw ContentError.Offline
        } catch (e: Exception) {
            throw ContentError.SourceChanged(e.message ?: e.javaClass.simpleName)
        }
    }
}
