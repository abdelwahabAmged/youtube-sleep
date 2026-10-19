package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.MediaItem
import app.ytsleep.youtube.PlaylistSummary
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads YouTube's `lockupViewModel` entries, which replaced `videoRenderer` and
 * `gridPlaylistRenderer` in the web feeds and library during 2025–2026.
 */
object LockupParser {

    private const val TYPE_VIDEO = "LOCKUP_CONTENT_TYPE_VIDEO"
    private val PLAYLIST_TYPES = setOf("LOCKUP_CONTENT_TYPE_PLAYLIST", "LOCKUP_CONTENT_TYPE_PODCAST")
    private val DURATION = Regex("""^\d{1,2}(:\d{2}){1,2}$""")
    private const val MAX_THUMBNAIL_WIDTH = 480

    /** A playable video, or null for other lockups and for live or upcoming streams (no audio file to play). */
    fun video(lockup: JsonObject): MediaItem? {
        if (lockup.string("contentType") != TYPE_VIDEO) return null
        val videoId = lockup.string("contentId") ?: return null
        val badges = badgeTexts(lockup["contentImage"])
        if (badges.any { it.equals("LIVE", ignoreCase = true) || it.equals("Upcoming", ignoreCase = true) }) return null

        return MediaItem(
            videoId = videoId,
            title = title(lockup),
            channelName = metadataRows(lockup).firstOrNull()?.firstOrNull().orEmpty(),
            channelId = null,
            thumbnailUrl = thumbnail(lockup.obj("contentImage")?.obj("thumbnailViewModel")),
            durationSeconds = badges.firstOrNull { DURATION.matches(it) }?.let(::parseDuration),
            isMarkedShort = false,
            isLive = false,
        )
    }

    fun playlist(lockup: JsonObject): PlaylistSummary? {
        if (lockup.string("contentType") !in PLAYLIST_TYPES) return null
        val playlistId = lockup.string("contentId") ?: return null
        val count = badgeTexts(lockup["contentImage"])
            .firstNotNullOfOrNull { Regex("""^([\d,.]+)\s""").find(it)?.groupValues?.get(1) }
            ?.filter(Char::isDigit)?.toIntOrNull()
        val thumbnailVm = lockup.obj("contentImage")?.obj("collectionThumbnailViewModel")
            ?.obj("primaryThumbnail")?.obj("thumbnailViewModel")
        return PlaylistSummary(
            playlistId = playlistId,
            title = title(lockup),
            thumbnailUrl = thumbnail(thumbnailVm).ifEmpty { null },
            itemCount = count,
        )
    }

    /**
     * Next-page token of a `continuationItemRenderer`. It sits under `continuationCommand`, either
     * directly in `continuationEndpoint` or nested in a `commandExecutorCommand` list (playlists).
     */
    fun continuationToken(renderer: JsonElement?): String? = when (renderer) {
        is JsonObject -> (renderer["continuationCommand"] as? JsonObject)?.string("token")
            ?: renderer.values.firstNotNullOfOrNull(::continuationToken)
        is JsonArray -> renderer.firstNotNullOfOrNull(::continuationToken)
        else -> null
    }

    fun parseDuration(text: String): Int? {
        val parts = text.split(":").map { it.trim().toIntOrNull() ?: return null }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun title(lockup: JsonObject): String =
        lockup.obj("metadata")?.obj("lockupMetadataViewModel")?.obj("title")?.string("content").orEmpty()

    /** Text of each metadata row, e.g. [["Channel"], ["12K views", "2 days ago"]]. */
    private fun metadataRows(lockup: JsonObject): List<List<String>> {
        val rows = lockup.obj("metadata")?.obj("lockupMetadataViewModel")?.obj("metadata")
            ?.obj("contentMetadataViewModel")?.get("metadataRows") as? JsonArray ?: return emptyList()
        return rows.map { row ->
            (row.jsonObject["metadataParts"] as? JsonArray).orEmpty()
                .mapNotNull { it.jsonObject.obj("text")?.string("content") }
        }
    }

    /** Largest thumbnail no wider than 480 px (list rows are small), else the smallest one. */
    private fun thumbnail(thumbnailViewModel: JsonObject?): String {
        val sources = (thumbnailViewModel?.obj("image")?.get("sources") as? JsonArray).orEmpty()
            .mapNotNull { s ->
                val o = s.jsonObject
                val url = o.string("url") ?: return@mapNotNull null
                url to (o["width"]?.jsonPrimitive?.intOrNull ?: 0)
            }
        return (sources.filter { it.second <= MAX_THUMBNAIL_WIDTH }.maxByOrNull { it.second }
            ?: sources.minByOrNull { it.second })?.first.orEmpty()
    }

    /** All `thumbnailBadgeViewModel` texts under [element], e.g. "32:35", "LIVE", "353 videos". */
    private fun badgeTexts(element: JsonElement?): List<String> {
        val out = mutableListOf<String>()
        fun walk(e: JsonElement?) {
            when (e) {
                is JsonObject -> e.forEach { (k, v) ->
                    if (k == "thumbnailBadgeViewModel") (v as? JsonObject)?.string("text")?.let { if (it.isNotBlank()) out += it }
                    else walk(v)
                }
                is JsonArray -> e.forEach(::walk)
                else -> Unit
            }
        }
        walk(element)
        return out
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.string(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
}
