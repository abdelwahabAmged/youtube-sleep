package app.ytsleep.youtube.innertube

import android.content.Context
import android.webkit.CookieManager
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.ContentError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val YOUTUBE_URL = "https://www.youtube.com"
private const val CLIENT_VERSION = "2.20250101.00.00"

class InnerTubeClient(
    private val client: OkHttpClient,
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val context_body = """
        "context":{
          "client":{
            "clientName":"WEB",
            "clientVersion":"$CLIENT_VERSION",
            "hl":"en","gl":"US"
          }
        }
    """.trimIndent()

    private suspend fun cookies(): Map<String, String> {
        val container = (context.applicationContext as YtSleepApp).container
        return container.accountStore.cookies.first()
    }

    private fun cookieHeader(map: Map<String, String>): String =
        map.entries.joinToString("; ") { (k, v) -> "$k=$v" }

    suspend fun browse(browseId: String): JsonObject = withContext(Dispatchers.IO) {
        val cookieMap = cookies()
        if (cookieMap.isEmpty()) throw ContentError.NotSignedIn

        val sapisid = cookieMap["SAPISID"] ?: throw ContentError.NotSignedIn
        val body = """{"browseId":"$browseId",$context_body}"""

        postInnerTube("browse", body, cookieMap, sapisid)
    }

    suspend fun continuation(token: String): JsonObject = withContext(Dispatchers.IO) {
        val cookieMap = cookies()
        if (cookieMap.isEmpty()) throw ContentError.NotSignedIn

        val sapisid = cookieMap["SAPISID"] ?: throw ContentError.NotSignedIn
        val body = """{"continuation":"$token",$context_body}"""

        postInnerTube("browse", body, cookieMap, sapisid)
    }

    /** Adds [videoId] to one of the user's playlists (including Watch later, "WL"). */
    suspend fun addToPlaylist(playlistId: String, videoId: String) = withContext(Dispatchers.IO) {
        val cookieMap = cookies()
        val sapisid = cookieMap["SAPISID"] ?: throw ContentError.NotSignedIn
        // JsonPrimitive quotes and escapes the ids, so they can't break the request body.
        val body = """{"playlistId":${JsonPrimitive(playlistId)},""" +
            """"actions":[{"action":"ACTION_ADD_VIDEO","addedVideoId":${JsonPrimitive(videoId)}}],$context_body}"""
        val result = postInnerTube("browse/edit_playlist", body, cookieMap, sapisid)
        if (result["status"]?.jsonPrimitive?.contentOrNull != "STATUS_SUCCEEDED") {
            throw ContentError.SourceChanged("Adding to playlist was not accepted")
        }
    }

    /** Marks the stored session expired so Settings offers "Reconnect", then signals the caller. */
    private suspend fun sessionExpired(): Nothing {
        (context.applicationContext as YtSleepApp).container.accountStore.markExpired()
        throw ContentError.SessionExpired
    }

    private suspend fun postInnerTube(
        endpoint: String,
        body: String,
        cookieMap: Map<String, String>,
        sapisid: String,
    ): JsonObject {
        val url = "$YOUTUBE_URL/youtubei/v1/$endpoint?prettyPrint=false"
        val auth = SapisidHash.header(sapisid)

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Cookie", cookieHeader(cookieMap))
            .header("Authorization", auth)
            .header("X-Origin", YOUTUBE_URL)
            .header("Origin", YOUTUBE_URL)
            .header("X-Goog-AuthUser", "0")
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 401 && response.code != 403) {
            response.body?.close()
            throw ContentError.SourceChanged("HTTP ${response.code} from $endpoint")
        }

        if (response.code == 401 || response.code == 403) {
            response.body?.close()
            sessionExpired()
        }

        val responseBody = response.body?.string() ?: throw ContentError.Offline
        val obj = json.parseToJsonElement(responseBody).jsonObject

        // Check for logged-out state in responseContext
        val responseContext = obj["responseContext"]?.jsonObject
        val mainAppWebResponseContext = responseContext
            ?.get("mainAppWebResponseContext")?.jsonObject
        val loggedOut = mainAppWebResponseContext?.get("loggedOut")
        if (loggedOut?.toString() == "true") sessionExpired()

        return obj
    }
}
