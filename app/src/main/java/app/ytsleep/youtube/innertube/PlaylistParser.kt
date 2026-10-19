package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.MediaItem
import kotlinx.serialization.json.*

object PlaylistParser {

    fun parse(root: JsonObject): Pair<List<MediaItem>, String?> {
        val items = mutableListOf<MediaItem>()
        var continuation: String? = null

        fun walkElement(element: JsonElement) {
            if (element !is JsonObject) return
            for ((key, value) in element) {
                when (key) {
                    "playlistVideoRenderer" -> parseVideo(value.jsonObject)?.let { items.add(it) }
                    "lockupViewModel" -> LockupParser.video(value.jsonObject)?.let { items.add(it) }
                    "continuationItemRenderer" -> {
                        continuation = LockupParser.continuationToken(value)
                    }
                    else -> {
                        if (value is JsonObject) walkElement(value)
                        else if (value is JsonArray) value.forEach { walkElement(it) }
                    }
                }
            }
        }

        walkElement(root)
        return Pair(items, continuation)
    }

    private fun parseVideo(obj: JsonObject): MediaItem? {
        val videoId = obj["videoId"]?.jsonPrimitive?.contentOrNull ?: return null
        // Deleted or private videos stay in playlists but can't be played.
        if (obj["isPlayable"]?.jsonPrimitive?.contentOrNull == "false") return null
        if (LockupParser.isMembersOnly(obj["badges"])) return null

        val title = obj["title"]?.let {
            when (it) {
                is JsonObject -> it["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: it["simpleText"]?.jsonPrimitive?.contentOrNull
                is JsonPrimitive -> it.contentOrNull
                else -> null
            }
        } ?: ""

        val channel = obj["shortBylineText"]?.jsonObject
            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

        val lengthText = obj["lengthText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
        val durationSeconds = lengthText?.let { parseLength(it) }

        // Largest thumbnail that is still small enough for a list row.
        val thumbnail = obj["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.map { it.jsonObject }
            ?.let { thumbs -> thumbs.lastOrNull { (it["width"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0) <= 480 } ?: thumbs.firstOrNull() }
            ?.get("url")?.jsonPrimitive?.contentOrNull ?: ""

        return MediaItem(
            videoId = videoId,
            title = title,
            channelName = channel,
            channelId = null,
            thumbnailUrl = thumbnail,
            durationSeconds = durationSeconds,
            isMarkedShort = false,
            isLive = false,
        )
    }

    private fun parseLength(text: String): Int? {
        val parts = text.split(":").map { it.trim().toIntOrNull() ?: return null }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }
}
