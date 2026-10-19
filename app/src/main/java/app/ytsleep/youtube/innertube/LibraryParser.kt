package app.ytsleep.youtube.innertube

import app.ytsleep.youtube.PlaylistSummary
import kotlinx.serialization.json.*

object LibraryParser {

    fun parse(root: JsonObject): List<PlaylistSummary> {
        val result = mutableListOf<PlaylistSummary>()

        fun walkElement(element: JsonElement) {
            if (element !is JsonObject) return
            for ((key, value) in element) {
                when (key) {
                    "gridPlaylistRenderer", "playlistRenderer", "lockupViewModel" -> {
                        parsePlaylist(value.jsonObject)?.let { result.add(it) }
                    }
                    else -> {
                        if (value is JsonObject) walkElement(value)
                        else if (value is JsonArray) value.forEach { walkElement(it) }
                    }
                }
            }
        }

        walkElement(root)

        // Always prepend WL (Watch later) and LL (Liked videos)
        val specialIds = result.map { it.playlistId }.toSet()
        val specials = mutableListOf<PlaylistSummary>()
        if ("WL" !in specialIds) specials.add(PlaylistSummary("WL", "Watch later", null, null))
        if ("LL" !in specialIds) specials.add(PlaylistSummary("LL", "Liked videos", null, null))

        return specials + result
    }

    private fun parsePlaylist(obj: JsonObject): PlaylistSummary? {
        val playlistId = obj["playlistId"]?.jsonPrimitive?.contentOrNull ?: return null

        val title = obj["title"]?.let {
            when (it) {
                is JsonObject -> it["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: it["simpleText"]?.jsonPrimitive?.contentOrNull
                is JsonPrimitive -> it.contentOrNull
                else -> null
            }
        } ?: ""

        val thumbnail = obj["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull

        val count = obj["videoCountText"]?.jsonObject
            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        return PlaylistSummary(playlistId, title, thumbnail, count)
    }
}
