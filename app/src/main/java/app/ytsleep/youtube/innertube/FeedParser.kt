package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.ContentFilter
import app.ytsleep.youtube.MediaItem
import kotlinx.serialization.json.*

object FeedParser {

    fun parse(root: JsonObject): Pair<List<MediaItem>, String?> {
        val items = mutableListOf<MediaItem>()
        var continuation: String? = null

        // Handle both initial browse and onResponseReceivedActions continuation shapes
        val contents = root["contents"]
            ?: root["onResponseReceivedActions"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("appendContinuationItemsAction")?.jsonObject
                ?.get("continuationItems")

        fun walkElement(element: JsonElement) {
            if (element !is JsonObject) return
            for ((key, value) in element) {
                when (key) {
                    "videoRenderer" -> parseVideoRenderer(value.jsonObject)?.let { items.add(it) }
                    "lockupViewModel" -> LockupParser.video(value.jsonObject)?.let { items.add(it) }
                    "reelItemRenderer" -> {
                        parseVideoRenderer(value.jsonObject, forceShort = true)?.let { items.add(it) }
                    }
                    "shortsLockupViewModel", "reelShelfRenderer" -> {
                        // Shorts are never shown (FR-012)
                    }
                    "adSlotRenderer", "promotedSparklesWebRenderer", "statementBannerRenderer" -> {
                        // Drop ad renderers
                    }
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

        if (contents is JsonArray) contents.forEach { walkElement(it) }
        else if (contents is JsonObject) walkElement(contents)

        return Pair(items, continuation)
    }

    private fun parseVideoRenderer(obj: JsonObject, forceShort: Boolean = false): MediaItem? {
        val videoId = obj["videoId"]?.jsonPrimitive?.contentOrNull ?: return null

        val title = obj["title"]?.let {
            it.jsonObject["runs"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?: it.jsonObject["simpleText"]?.jsonPrimitive?.contentOrNull
        } ?: ""

        val channel = obj["ownerText"]?.jsonObject
            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?: obj["shortBylineText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            ?: ""

        val lengthText = obj["lengthText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
        val durationSeconds = lengthText?.parseDuration()

        val thumbnails = obj["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.mapNotNull { it.jsonObject["url"]?.jsonPrimitive?.contentOrNull }
            ?: emptyList()
        val thumbnail = thumbnails.firstOrNull() ?: ""

        val url = "https://www.youtube.com/watch?v=$videoId"
        val isMarkedShort = forceShort || ContentFilter.isShortsUrl(url)

        return MediaItem(
            videoId = videoId,
            title = title,
            channelName = channel,
            channelId = null,
            thumbnailUrl = thumbnail,
            durationSeconds = durationSeconds,
            isMarkedShort = isMarkedShort,
            isLive = false,
        )
    }

    private fun String.parseDuration(): Int? {
        val parts = this.split(":").map { it.trim().toIntOrNull() ?: return null }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }
}
