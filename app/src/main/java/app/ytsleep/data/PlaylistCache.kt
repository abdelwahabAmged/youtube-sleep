package app.ytsleep.data

import android.content.Context
import app.ytsleep.youtube.PlaylistSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Last known "My playlists", so the Start screen can show them before the network answers. */
class PlaylistCache(context: Context) {

    private val file = File(context.filesDir, "playlists.json")

    suspend fun read(): List<PlaylistSummary> = withContext(Dispatchers.IO) {
        runCatching { Json.decodeFromString<List<PlaylistSummary>>(file.readText()) }.getOrDefault(emptyList())
    }

    suspend fun write(playlists: List<PlaylistSummary>) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(Json.encodeToString(playlists)) }
    }

    fun clear() {
        file.delete()
    }
}
